package org.openjdbcproxy.cache.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表级同步状态管理器 - 维护倒排索引，支持 O(1) 缓存决策
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableSyncStateManager {

    private final CacheRuleRepository cacheRuleRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Key: "jobName" (Stream 中的 jobName)
    private final Map<String, TableSyncState> tableSyncIndex = new ConcurrentHashMap<>();

    private static final String REDIS_SYNC_STATUS_KEY = "ojp:cache:sync_status";

    @PostConstruct
    public void init() {
        // 先从 Redis 加载持久化的状态
        loadFromRedis();
        // 再根据规则重建索引（确保所有规则中的表都有项，即使没有同步状态）
        rebuildAllFromRules();
    }

    /**
     * 从 Redis 加载持久化的同步状态
     */
    private void loadFromRedis() {
        try {
            log.info("正在从 Redis 加载持久化的同步状态，Key: {}", REDIS_SYNC_STATUS_KEY);
            Map<Object, Object> savedStates = redisTemplate.opsForHash().entries(REDIS_SYNC_STATUS_KEY);
            int count = 0;
            for (Map.Entry<Object, Object> entry : savedStates.entrySet()) {
                String key = (String) entry.getKey();
                String json = (String) entry.getValue();
                try {
                    TableSyncState state = objectMapper.readValue(json, TableSyncState.class);
                    // 恢复状态到内存索引
                    tableSyncIndex.put(key, state);
                    count++;
                } catch (Exception e) {
                    log.error("反序列化同步状态失败，Key: {}", key, e);
                }
            }
            log.info("已从 Redis 加载 {} 个同步状态", count);
        } catch (Exception e) {
            log.error("从 Redis 加载同步状态失败", e);
        }
    }

    /**
     * 判断指定表是否可用于缓存查询
     */
    public boolean isTableReady(String connHash, String tableName) {
        String database = org.openjdbcproxy.grpc.server.utils.JdbcUrlUtil.extractDatabaseName(connHash);
        if (database == null)
            return false;

        String key = buildKey(connHash, database, tableName);
        TableSyncState state = tableSyncIndex.get(key);
        if (state == null) {
            log.debug("未找到表的同步状态: {}", key);
            return false;
        }
        boolean available = state.isAvailable();
        if (!available) {
            log.info("表 {} 不可用: ready={}, snapshotFinished={}, stale={} (上次更新于 {} 秒前)",
                    key, state.isReady(), state.isSnapshotFinished(), state.isStale(),
                    (System.currentTimeMillis() - state.getUpdateTime()) / 1000);
        }
        return available;
    }

    /**
     * 从 Stream 事件更新表状态
     */
    public void updateFromStreamEvent(Map<String, String> event) {
        // 直接使用 Stream 中的 jobName 作为 Key
        String jobName = event.get("jobName");

        if (jobName == null) {
            return;
        }

        updateStateForKey(jobName, event);
    }

    private void updateStateForKey(String key, Map<String, String> event) {
        TableSyncState state = tableSyncIndex.computeIfAbsent(key, k -> {
            // 理论上 findMatchedKeys 只会返回已存在的 key，但防万一
            return new TableSyncState();
        });

        String type = event.get("type");
        String jobId = event.get("jobId");

        state.setJobId(jobId);
        state.setJobId(jobId);
        state.setUpdateTime(System.currentTimeMillis());

        // Attempt to populate metadata from event if missing
        if (state.getDatabase() == null && event.containsKey("database")) {
            state.setDatabase(event.get("database"));
        }
        if (state.getTableName() == null) {
            if (event.containsKey("tableName")) {
                state.setTableName(event.get("tableName"));
            } else if (event.containsKey("table")) {
                state.setTableName(event.get("table"));
            }
        }
        if (state.getConnHash() == null && event.containsKey("connHash")) {
            state.setConnHash(event.get("connHash"));
        }

        if ("SNAPSHOT_SYNCING".equals(event.get("state"))) {
            // 正在同步快照
            state.setSnapshotFinished(false);
            state.setReady(false);
        } else if ("INCREMENTAL_SYNCING".equals(event.get("state"))) {
            // 进入增量同步，视为 Ready
            state.setSnapshotFinished(true);
            state.setReady(true);
        } else if ("INCREMENTAL_IDLE".equals(event.get("state"))) {
            // 增量空闲，也是 Ready
            state.setSnapshotFinished(true);
            state.setReady(true);
        } else if ("FAILED".equals(event.get("state"))) {
            state.setReady(false);
        }

        log.debug("已根据流事件更新同步状态: {} -> ready={}", key, state.isReady());
    }

    private void persistStateToRedis(String key, TableSyncState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForHash().put(REDIS_SYNC_STATUS_KEY, key, json);
        } catch (Exception e) {
            log.error("持久化同步状态到 Redis 失败，Key: {}", key, e);
        }
    }

    /**
     * 规则变更时重建相关表的索引
     */
    public void rebuildForRule(CacheRule rule) {
        // ... (保持不变，或根据需要优化)
        if (rule == null)
            return;

        String connHash = rule.getConnHash();
        List<String> tables = rule.getTables();
        Map<String, String> jobIds = rule.getSeatunnelJobIds();

        if (tables == null || connHash == null)
            return;

        String database = org.openjdbcproxy.grpc.server.utils.JdbcUrlUtil.extractDatabaseName(connHash);
        if (database == null)
            return;

        for (String table : tables) {
            String key = buildKey(connHash, database, table);
            String normalizedTable = table.trim().toLowerCase(Locale.ROOT);

            // 使用 computeIfAbsent 确保如果 Redis 已加载状态，不会被覆盖为空状态
            TableSyncState state = tableSyncIndex.computeIfAbsent(key, k -> {
                TableSyncState s = new TableSyncState();
                s.setTableKey(k);
                s.setConnHash(connHash);
                s.setDatabase(database); // Set database explicitly
                s.setTableName(normalizedTable);
                return s;
            });

            state.setRuleId(rule.getId());

            // 尝试从规则的 jobIds 映射中获取 jobId
            if (jobIds != null && jobIds.containsKey(normalizedTable)) {
                state.setJobId(jobIds.get(normalizedTable));
            }

            log.debug("已为规则 {} 建立表索引: {}", rule.getId(), key);
        }
    }

    // ... (rest of the file)

    /**
     * 删除规则时清理相关索引
     */
    public void removeForRule(CacheRule rule) {
        if (rule == null || rule.getTables() == null)
            return;

        String connHash = rule.getConnHash();
        String database = org.openjdbcproxy.grpc.server.utils.JdbcUrlUtil.extractDatabaseName(connHash);
        if (database == null)
            return;

        for (String table : rule.getTables()) {
            String key = buildKey(connHash, database, table);
            tableSyncIndex.remove(key);
            // 同时清理 Redis 中的持久化状态
            redisTemplate.opsForHash().delete(REDIS_SYNC_STATUS_KEY, key);
            log.debug("已移除表索引: {}", key);
        }
    }

    /**
     * 启动时从所有规则重建索引
     */
    private void rebuildAllFromRules() {
        log.info("正在根据所有缓存规则重建表同步索引...");
        Iterable<CacheRule> rules = cacheRuleRepository.findAll();
        int count = 0;
        for (CacheRule rule : rules) {
            if (rule.isEnabled()) {
                rebuildForRule(rule);
                count++;
            }
        }
        log.info("已从 {} 个启用的规则中重建索引，共 {} 个表条目", count, tableSyncIndex.size());
    }

    /**
     * 构建规范化的索引键 (JobName)
     */
    private String buildKey(String connHash, String database, String tableName) {
        return org.openjdbcproxy.cache.util.SeatunnelUtils.buildJobName(connHash, database, tableName);
    }

    /**
     * 获取所有同步状态
     */
    public java.util.Collection<TableSyncState> getAllSyncStates() {
        return tableSyncIndex.values();
    }

    /**
     * 获取当前索引大小 (用于监控)
     */
    public int getIndexSize() {
        return tableSyncIndex.size();
    }
}
