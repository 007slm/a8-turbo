package org.openjdbcproxy.cdc.monitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.event.Event;
import org.apache.seatunnel.api.event.EventHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class TableCdcStateEventHandler implements EventHandler {

    /* ================= 配置 ================= */

    private static final String REDIS_HOST = System.getenv().getOrDefault("OJP_REDIS_HOST", "redis");

    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("OJP_REDIS_PORT", "6379"));

    private static final String REDIS_AUTH = System.getenv("OJP_REDIS_AUTH");

    /** CDC 空闲窗口：多久没事件认为追平（毫秒） */
    private static final long IDLE_WINDOW_MS = 30_000;

    /** 定时扫描频率 */
    private static final long SCAN_INTERVAL_MS = 5_000;

    /* ================= Redis ================= */

    private final JedisPool jedisPool;

    /* ================= Job → Table 映射 ================= */

    private final Map<String, TableId> jobTableCache = new ConcurrentHashMap<>();

    /* ================= 定时器 ================= */

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cdc-idle-detector");
        t.setDaemon(true);
        return t;
    });

    public TableCdcStateEventHandler() {
        this.jedisPool = new JedisPool(REDIS_HOST, REDIS_PORT);

        if (REDIS_AUTH != null && !REDIS_AUTH.isEmpty()) {
            try (Jedis j = jedisPool.getResource()) {
                j.auth(REDIS_AUTH);
            }
        }

        scheduler.scheduleAtFixedRate(
                this::monitorJobs,
                SCAN_INTERVAL_MS,
                SCAN_INTERVAL_MS,
                TimeUnit.MILLISECONDS);

        log.info("CDC 表状态处理器已启动。空闲窗口={}ms", IDLE_WINDOW_MS);
    }

    /* ================= Event 入口 ================= */

    @Override
    public void handle(Event event) {
        String jobId = event.getJobId();
        String eventType = event.getEventType().name();
        long now = System.currentTimeMillis();

        log.info("接收到事件: 类型={}, jobId={}", eventType, jobId);

        TableId table = resolveTable(jobId);
        if (table == null) {
            log.warn("无法解析 Job {} 的表信息", jobId);
            return;
        }

        String redisKey = redisKey(table);

        try (Jedis jedis = jedisPool.getResource()) {

            switch (eventType) {

                /* ---------- Job 启动 / Snapshot ---------- */

                case "JOB_START":
                case "SOURCE_START":
                    setState(jedis, redisKey, jobId, null,
                            CdcState.SNAPSHOT_SYNCING, now);
                    break;

                /* ---------- Snapshot 结束，进入增量 ---------- */

                case "READER_CLOSE":
                case "ENUMERATOR_CLOSE":
                case "LIFECYCLE_READER_CLOSE":
                    transitionToIncrementalSyncing(jedis, redisKey, jobId, now);
                    break;

                /* ---------- 任意 CDC 活动（认为正在同步） ---------- */

                case "CHECKPOINT_COMPLETED":
                case "BATCH_CHECKPOINT":
                    markIncrementalSyncing(jedis, redisKey, jobId, now);
                    break;

                /* ---------- Job 失败 ---------- */

                case "JOB_FAIL":
                case "TASK_FAIL":
                    setState(jedis, redisKey, jobId, null,
                            CdcState.FAILED, now);
                    break;

                default:
                    // 忽略其它事件
            }

        } catch (Exception e) {
            log.error("处理 Job {} 的事件 {} 失败", jobId, eventType, e);
        }
    }

    /* ================= 状态迁移逻辑 ================= */

    private void transitionToIncrementalSyncing(
            Jedis jedis, String key, String jobId, long now) {

        String state = jedis.hget(key, "state");
        if (CdcState.SNAPSHOT_SYNCING.name().equals(state)) {
            setState(jedis, key, jobId, null,
                    CdcState.INCREMENTAL_SYNCING, now);
        }
    }

    private void markIncrementalSyncing(
            Jedis jedis, String key, String jobId, long now) {

        String state = jedis.hget(key, "state");
        if (state == null
                || CdcState.INCREMENTAL_IDLE.name().equals(state)
                || CdcState.INCREMENTAL_SYNCING.name().equals(state)) {

            setState(jedis, key, jobId, null,
                    CdcState.INCREMENTAL_SYNCING, now);
        }
    }

    /* ================= Idle 判定 ================= */

    /* ================= Metric Polling (Main Logic) ================= */

    private final Map<String, Long> jobSourceCountCache = new ConcurrentHashMap<>();

    private void monitorJobs() {
        try {
            JsonNode runningJobs = fetchRunningJobs();
            if (runningJobs == null || !runningJobs.isArray()) {
                return;
            }

            long now = System.currentTimeMillis();

            try (Jedis jedis = jedisPool.getResource()) {
                for (JsonNode jobInfo : runningJobs) {
                    processJobMetrics(jedis, jobInfo, now);
                }
            }

        } catch (Exception e) {
            log.error("监控 Job 失败", e);
        }
    }

    private void processJobMetrics(Jedis jedis, JsonNode jobInfo, long now) {
        String jobId = jobInfo.path("jobId").asText();

        // 1. Resolve Table
        TableId table = jobTableCache.computeIfAbsent(jobId, k -> parseTableFromJobInfo(jobInfo, k));
        if (table == null)
            return;

        String key = redisKey(table);

        // 2. Get Source Metrics
        // Metrics structure: metrics -> SourceReceivedCount
        long currentCount = jobInfo.path("metrics").path("SourceReceivedCount").asLong(0);
        Long prevCount = jobSourceCountCache.getOrDefault(jobId, -1L);

        // 3. Determine State
        if (currentCount > prevCount) {
            // Data is flowing (or first run)
            // Force update to SYNCING and update timestamp
            setState(jedis, key, jobId, jobInfo, CdcState.INCREMENTAL_SYNCING, now);
            jobSourceCountCache.put(jobId, currentCount);
        } else {
            // Count hasn't changed. Check if we are IDLE or need Heartbeat.
            String stateStr = jedis.hget(key, "state");
            String lastEventStr = jedis.hget(key, "lastEventTime");

            // Heartbeat Logic: If > 30s since last event, force update
            long lastTs = 0;
            if (lastEventStr != null) {
                try {
                    lastTs = Long.parseLong(lastEventStr);
                } catch (NumberFormatException ignored) {
                }
            }

            if (now - lastTs >= 30_000) { // Heartbeat interval
                CdcState currentState;
                try {
                    currentState = stateStr != null ? CdcState.valueOf(stateStr) : CdcState.INCREMENTAL_SYNCING;
                } catch (Exception e) {
                    currentState = CdcState.INCREMENTAL_SYNCING;
                }

                // If IDLE logic applies, switch to IDLE
                if (CdcState.INCREMENTAL_SYNCING == currentState && now - lastTs >= IDLE_WINDOW_MS) {
                    currentState = CdcState.INCREMENTAL_IDLE;
                }

                setState(jedis, key, jobId, jobInfo, currentState, now);
            }
        }
    }

    private JsonNode fetchRunningJobs() {
        String url = SEATUNNEL_API_URL + "/running-jobs";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream()) {
                    return objectMapper.readTree(in);
                }
            }
        } catch (Exception e) {
            log.warn("获取运行中 Job 失败: {}", e.getMessage());
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return null;
    }

    /* ================= Redis 写入（唯一出口） ================= */

    private final Map<String, Long> lastStreamTimeCache = new ConcurrentHashMap<>();

    private void setState(
            Jedis jedis,
            String key,
            String jobId,
            JsonNode jobInfo,
            CdcState state,
            long now) {

        Map<String, String> hash = new java.util.HashMap<>();
        hash.put("state", state.name());
        hash.put("available", String.valueOf(state == CdcState.INCREMENTAL_IDLE));
        hash.put("jobId", jobId == null ? "" : jobId);
        hash.put("lastEventTime", String.valueOf(now));
        hash.put("updateTime", String.valueOf(now));
        jedis.hset(key, hash);

        log.info("CDC 状态 [{}] => {}", key, state);

        String jobName = jobInfo != null ? jobInfo.path("jobName").asText("") : "";
        publishStreamEvent(jedis, jobId, jobName, state, now);
    }

    private void publishStreamEvent(Jedis jedis, String jobId, String jobName, CdcState state, long now) {
        // Throttle Logic:
        // Always publish if:
        // 1. It is a new state (how to track? local cache)
        // 2. OR it has been > 30s since last publish (Heartbeat)

        Long lastTime = lastStreamTimeCache.getOrDefault(jobId, 0L);
        boolean shouldPublish = false;

        if (state != CdcState.INCREMENTAL_SYNCING) {
            // Non-syncing states (IDLE, FAIL, SNAPSHOT) are important transitions. Publish
            // immediately.
            shouldPublish = true;
        } else {
            // For INCREMENTAL_SYNCING, if > 30s, publish (Heartbeat)
            if (now - lastTime > 30_000) {
                shouldPublish = true;
            }
        }

        if (shouldPublish) {
            try {
                java.util.Map<String, String> event = new java.util.HashMap<>();
                event.put("type", "CDC_STATE_CHANGE");
                event.put("jobId", jobId);
                event.put("jobName", jobName);
                event.put("state", state.name());
                event.put("timestamp", String.valueOf(now));

                // 获取表信息
                TableId table = jobTableCache.get(jobId);
                if (table != null) {
                    event.put("database", table.database);
                    event.put("table", table.table);
                    event.put("connHash", "unknown"); // We might not have connHash here easily, but ojp-cache uses
                                                      // jobName to index.
                    // Important: Ideally we should pass connHash if we had it, but for now db/table
                    // + jobName is main key.
                }

                // Use XADD with MAXLEN ~1000 to prevent infinite growth
                redis.clients.jedis.params.XAddParams params = redis.clients.jedis.params.XAddParams.xAddParams()
                        .maxLen(1000);
                jedis.xadd("ojp:seatunnel:events", params, event);

                log.info("已发布 CDC 状态变更事件到 Stream: job={}, state={}", jobId, state);

                lastStreamTimeCache.put(jobId, now);
            } catch (Exception e) {
                log.error("发布 Stream 事件失败", e);
            }
        }
    }

    /* ================= Table / Job ================= */

    private TableId resolveTable(String jobId) {
        return jobTableCache.computeIfAbsent(jobId, this::fetchTableFromApi);
    }

    private static final String SEATUNNEL_API_URL = System.getenv().getOrDefault("OJP_SEATUNNEL_API_URL",
            "http://seatunnel-master:8080");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TableId fetchTableFromApi(String jobId) {
        String url = SEATUNNEL_API_URL + "/job-info/" + jobId;
        log.info("正在从 {} 获取 Job 信息", url);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("Seatunnel API 返回状态码 {} (Job: {})", status, jobId);
                return null;
            }

            try (InputStream in = conn.getInputStream()) {
                JsonNode root = objectMapper.readTree(in);
                return parseTableFromJobInfo(root, jobId);
            }

        } catch (Exception e) {
            log.error("获取 Job {} 信息失败", jobId, e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private TableId parseTableFromJobInfo(JsonNode root, String jobId) {

        JsonNode vertices = root.path("jobDag").path("vertices");
        // 如果 vertices 为空，尝试读取 vertexInfoMap (适用于部分新版本/API 差异)
        if (vertices.isMissingNode() || !vertices.isArray()) {
            vertices = root.path("jobDag").path("vertexInfoMap");
        }

        if (!vertices.isArray()) {
            log.warn("Job {} 的 jobDag 中未找到 vertices/vertexInfoMap", jobId);
            return null;
        }

        for (JsonNode vertex : vertices) {
            // 策略 1: 尝试解析 pluginConfig (旧逻辑)
            JsonNode pluginConfig = vertex.path("pluginConfig");
            if (!pluginConfig.isMissingNode()) {
                String database = extractFirst(pluginConfig, "database-names");
                String table = extractFirst(pluginConfig, "table-names");

                if (database != null && table != null) {
                    log.info("解析 Job {} -> {}.{} (通过 pluginConfig)", jobId, database, table);
                    return new TableId(database, table);
                }
            }

            // 策略 2: 尝试解析 tablePaths (新逻辑)
            JsonNode tablePaths = vertex.path("tablePaths");
            if (tablePaths.isArray() && tablePaths.size() > 0) {
                String fullPath = tablePaths.get(0).asText();
                if (fullPath != null && fullPath.contains(".")) {
                    String[] parts = fullPath.split("\\.", 2);
                    if (parts.length == 2) {
                        String database = parts[0];
                        String table = parts[1];
                        log.info("解析 Job {} -> {}.{} (通过 tablePaths)", jobId, database, table);
                        return new TableId(database, table);
                    }
                }
            }
        }

        log.warn("无法解析 Job {} 的表信息 (尝试了 pluginConfig 和 tablePaths)", jobId);
        return null;
    }

    private String extractFirst(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);

        if (field.isArray() && field.size() > 0) {
            return field.get(0).asText(null);
        }

        if (field.isTextual()) {
            return field.asText(null);
        }

        return null;
    }

    private String redisKey(TableId table) {
        return "ojp:cdc:table:" + table.database + "." + table.table;
    }

    /* ================= Model ================= */

    enum CdcState {
        SNAPSHOT_SYNCING,
        INCREMENTAL_SYNCING,
        INCREMENTAL_IDLE,
        FAILED
    }

    static class TableId {
        final String database;
        final String table;

        TableId(String database, String table) {
            this.database = database;
            this.table = table;
        }
    }
}
