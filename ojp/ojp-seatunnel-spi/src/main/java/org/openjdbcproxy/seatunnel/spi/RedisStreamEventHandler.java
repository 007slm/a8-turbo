package org.openjdbcproxy.seatunnel.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.event.Event;
import org.apache.seatunnel.api.event.EventHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.XAddParams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event Listener for SeaTunnel Engine.
 * Pushes CDC phase and checkpoint events to Redis.
 * 
 * 首次收到事件时，通过 REST API 查询 Job 详情获取 database/table/connHash，
 * 并缓存起来供后续使用。
 */
@Slf4j
public class RedisStreamEventHandler implements EventHandler {

    private static final String REDIS_HOST_ENV = "OJP_REDIS_HOST";
    private static final String REDIS_PORT_ENV = "OJP_REDIS_PORT";
    private static final String REDIS_AUTH_ENV = "OJP_REDIS_AUTH";
    private static final String SEATUNNEL_API_URL_ENV = "OJP_SEATUNNEL_API_URL";
    
    private static final String STREAM_KEY = "ojp:seatunnel:events";
    private static final String JOB_STATE_KEY_PREFIX = "ojp:seatunnel:job:";
    private static final long STREAM_MAX_LEN = 10_000L;

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final String seatunnelApiUrl;
    
    // Job 上下文缓存: jobId -> JobContext
    private final Map<String, JobContext> jobContextCache = new ConcurrentHashMap<>();

    public RedisStreamEventHandler() {
        log.info("Initializing OJP RedisStreamEventHandler...");
        
        // Redis 配置
        String host = System.getenv().getOrDefault(REDIS_HOST_ENV, "redis");
        int port = Integer.parseInt(System.getenv().getOrDefault(REDIS_PORT_ENV, "6379"));
        String password = System.getenv(REDIS_AUTH_ENV);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWaitMillis(2000);
        
        if (password != null && !password.isEmpty()) {
            this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
        } else {
            this.jedisPool = new JedisPool(poolConfig, host, port);
        }
        
        // SeaTunnel REST API URL
        this.seatunnelApiUrl = System.getenv().getOrDefault(SEATUNNEL_API_URL_ENV, "http://seatunnel-master:8080");
        this.objectMapper = new ObjectMapper();
        
        log.info("Connected to Redis at {}:{}, SeaTunnel API at {}", host, port, seatunnelApiUrl);
    }

    @Override
    public void handle(Event event) {
        log.debug("Received SeaTunnel Event: {}", event.getEventType());
        
        try (Jedis jedis = jedisPool.getResource()) {
            String typeName = event.getEventType().name();
            if ("LIFECYCLE_READER_CLOSE".equals(typeName) || "READER_CLOSE".equals(typeName)) {
                handleReaderClose(event, jedis);
            } else if ("CHECKPOINT_COMPLETED".equals(typeName)) {
                handleCheckpoint(event, jedis);
            }
        } catch (Exception e) {
            log.error("Failed to handle event {}", event.getEventType(), e);
        }
    }
    
    private void handleReaderClose(Event event, Jedis jedis) {
        String jobId = event.getJobId();
        long ts = System.currentTimeMillis();
        
        // 获取 Job 上下文 (缓存 or REST API)
        JobContext ctx = getOrFetchJobContext(jobId);
        
        // 更新 Hash
        String hashKey = JOB_STATE_KEY_PREFIX + jobId;
        Map<String, String> hashData = new HashMap<>();
        hashData.put("phase", "INCREMENTAL");
        hashData.put("updateTime", String.valueOf(ts));
        if (ctx != null) {
            hashData.put("connHash", ctx.connHash);
            hashData.put("database", ctx.database);
            hashData.put("table", ctx.tableName);
        }
        jedis.hset(hashKey, hashData);
        
        // 发送 Stream 事件
        Map<String, String> eventData = new HashMap<>();
        eventData.put("type", "SNAPSHOT_DONE");
        eventData.put("jobId", jobId);
        eventData.put("ts", String.valueOf(ts));
        eventData.put("phase", "INCREMENTAL");
        if (ctx != null) {
            eventData.put("connHash", ctx.connHash);
            eventData.put("database", ctx.database);
            eventData.put("table", ctx.tableName);
        }
        
        jedis.xadd(STREAM_KEY, XAddParams.xAddParams().maxLen(STREAM_MAX_LEN), eventData);
        log.info("Reported SNAPSHOT_DONE for Job {} (table: {})", jobId, ctx != null ? ctx.tableName : "unknown");
    }

    private void handleCheckpoint(Event event, Jedis jedis) {
        String jobId = event.getJobId();
        long ts = System.currentTimeMillis();
        
        JobContext ctx = getOrFetchJobContext(jobId);
        
        // 更新 Hash
        Map<String, String> hashData = new HashMap<>();
        hashData.put("status", "RUNNING");
        hashData.put("updateTime", String.valueOf(ts));
        if (ctx != null) {
            hashData.put("connHash", ctx.connHash);
            hashData.put("database", ctx.database);
            hashData.put("table", ctx.tableName);
        }
        jedis.hset(JOB_STATE_KEY_PREFIX + jobId, hashData);
        
        // 发送 Stream 事件
        Map<String, String> eventData = new HashMap<>();
        eventData.put("type", "CHECKPOINT_OK");
        eventData.put("jobId", jobId);
        eventData.put("ts", String.valueOf(ts));
        if (ctx != null) {
            eventData.put("connHash", ctx.connHash);
            eventData.put("database", ctx.database);
            eventData.put("table", ctx.tableName);
        }
        
        jedis.xadd(STREAM_KEY, XAddParams.xAddParams().maxLen(STREAM_MAX_LEN), eventData);
        log.debug("Handled Checkpoint for Job {} (table: {})", jobId, ctx != null ? ctx.tableName : "unknown");
    }
    
    /**
     * 获取 Job 上下文，优先从缓存读取，缓存未命中则调用 REST API
     */
    private JobContext getOrFetchJobContext(String jobId) {
        return jobContextCache.computeIfAbsent(jobId, this::fetchJobContextFromApi);
    }
    
    /**
     * 调用 SeaTunnel REST API 获取 Job 详情
     */
    private JobContext fetchJobContextFromApi(String jobId) {
        String url = seatunnelApiUrl + "/job-info/" + jobId;
        log.info("Fetching job context from REST API: {}", url);
        
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("REST API returned status {} for job {}", status, jobId);
                return null;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            return parseJobContext(response.toString());
            
        } catch (Exception e) {
            log.error("Failed to fetch job context for {}: {}", jobId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析 REST API 响应，提取 database/table/connHash
     * 响应结构: { jobDag: { vertices: [ { pluginConfig: { url, database-names, table-names } } ] } }
     */
    private JobContext parseJobContext(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode vertices = root.path("jobDag").path("vertices");
            
            if (!vertices.isArray() || vertices.isEmpty()) {
                log.warn("No vertices found in job response");
                return null;
            }
            
            // 找到 Source 节点 (通常是第一个)
            for (JsonNode vertex : vertices) {
                JsonNode pluginConfig = vertex.path("pluginConfig");
                if (pluginConfig.isMissingNode()) continue;
                
                // 提取 database-names 和 table-names
                String database = extractFirstValue(pluginConfig, "database-names");
                String table = extractFirstValue(pluginConfig, "table-names");
                String connUrl = pluginConfig.path("url").asText(null);
                
                // table-names 格式可能是 "db.table"
                if (table != null && table.contains(".")) {
                    String[] parts = table.split("\\.", 2);
                    if (parts.length == 2) {
                        database = parts[0];
                        table = parts[1];
                    }
                }
                
                if (database != null && table != null) {
                    JobContext ctx = new JobContext();
                    ctx.database = database;
                    ctx.tableName = table;
                    ctx.connHash = connUrl != null ? connUrl : "";
                    log.info("Parsed job context: database={}, table={}, connHash={}", 
                            ctx.database, ctx.tableName, ctx.connHash);
                    return ctx;
                }
            }
            
            log.warn("Could not extract database/table from job response");
            return null;
            
        } catch (Exception e) {
            log.error("Failed to parse job context JSON: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractFirstValue(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isArray() && !field.isEmpty()) {
            return field.get(0).asText(null);
        } else if (field.isTextual()) {
            return field.asText(null);
        }
        return null;
    }
    
    /**
     * Job 上下文信息
     */
    static class JobContext {
        String connHash;
        String database;
        String tableName;
    }
}


