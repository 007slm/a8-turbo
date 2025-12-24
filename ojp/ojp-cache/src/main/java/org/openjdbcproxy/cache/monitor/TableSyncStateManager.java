package org.openjdbcproxy.cache.monitor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
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
    
    // Key: "connHash:tableName" (全小写规范化)
    private final Map<String, TableSyncState> tableSyncIndex = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        rebuildAllFromRules();
    }

    /**
     * 判断指定表是否可用于缓存查询
     */
    public boolean isTableReady(String connHash, String tableName) {
        String key = buildKey(connHash, tableName);
        TableSyncState state = tableSyncIndex.get(key);
        if (state == null) {
            log.debug("No sync state found for table: {}", key);
            return false;
        }
        boolean available = state.isAvailable();
        if (!available) {
            log.debug("Table {} not available: ready={}, stale={}", key, state.isReady(), state.isStale());
        }
        return available;
    }

    /**
     * 从 Stream 事件更新表状态
     */
    public void updateFromStreamEvent(Map<String, String> event) {
        String type = event.get("type");
        String connHash = event.get("connHash");
        String tableName = event.get("table");
        String jobId = event.get("jobId");
        
        if (connHash == null || tableName == null) {
            log.warn("Stream event missing connHash or table: {}", event);
            return;
        }
        
        String key = buildKey(connHash, tableName);
        TableSyncState state = tableSyncIndex.computeIfAbsent(key, k -> {
            TableSyncState s = new TableSyncState();
            s.setTableKey(k);
            s.setConnHash(connHash);
            s.setTableName(tableName);
            return s;
        });
        
        state.setJobId(jobId);
        state.setUpdateTime(System.currentTimeMillis());
        
        // 根据事件类型更新 ready 状态
        if ("SNAPSHOT_DONE".equals(type) || "CHECKPOINT_OK".equals(type)) {
            state.setReady(true);
        } else if ("JOB_STOPPED".equals(type) || "JOB_FAILED".equals(type)) {
            state.setReady(false);
        }
        
        log.info("Updated table sync state: {} -> ready={}", key, state.isReady());
    }

    /**
     * 规则变更时重建相关表的索引
     */
    public void rebuildForRule(CacheRule rule) {
        if (rule == null) return;
        
        String connHash = rule.getConnHash();
        List<String> tables = rule.getTables();
        Map<String, String> jobIds = rule.getSeatunnelJobIds();
        
        if (tables == null || connHash == null) return;
        
        for (String table : tables) {
            String key = buildKey(connHash, table);
            String normalizedTable = table.trim().toLowerCase(Locale.ROOT);
            
            TableSyncState state = tableSyncIndex.computeIfAbsent(key, k -> {
                TableSyncState s = new TableSyncState();
                s.setTableKey(k);
                s.setConnHash(connHash);
                s.setTableName(normalizedTable);
                return s;
            });
            
            state.setRuleId(rule.getId());
            
            // 尝试从规则的 jobIds 映射中获取 jobId
            if (jobIds != null && jobIds.containsKey(normalizedTable)) {
                state.setJobId(jobIds.get(normalizedTable));
            }
            
            log.debug("Indexed table {} for rule {}", key, rule.getId());
        }
    }

    /**
     * 删除规则时清理相关索引
     */
    public void removeForRule(CacheRule rule) {
        if (rule == null || rule.getTables() == null) return;
        
        for (String table : rule.getTables()) {
            String key = buildKey(rule.getConnHash(), table);
            tableSyncIndex.remove(key);
            log.debug("Removed table index: {}", key);
        }
    }

    /**
     * 启动时从所有规则重建索引
     */
    private void rebuildAllFromRules() {
        log.info("Rebuilding table sync index from all cache rules...");
        Iterable<CacheRule> rules = cacheRuleRepository.findAll();
        int count = 0;
        for (CacheRule rule : rules) {
            if (rule.isEnabled()) {
                rebuildForRule(rule);
                count++;
            }
        }
        log.info("Indexed tables from {} enabled rules, total {} table entries", count, tableSyncIndex.size());
    }

    /**
     * 构建规范化的索引键
     */
    private String buildKey(String connHash, String tableName) {
        String normalizedTable = tableName == null ? "" : tableName.trim().toLowerCase(Locale.ROOT);
        return connHash + ":" + normalizedTable;
    }
    
    /**
     * 获取当前索引大小 (用于监控)
     */
    public int getIndexSize() {
        return tableSyncIndex.size();
    }
}
