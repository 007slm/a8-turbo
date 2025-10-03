package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.QuerySummary;
import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.util.ReadableKeyGenerator;
import org.openjdbcproxy.cache.util.SqlParseUtil;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 异步统计服务
 * 参考e:\ojp项目实现，提供异步Redis统计和多维度指标收集
 * 支持实时统计更新、性能监控、缓存效果分析等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncStatsService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ReadableKeyGenerator readableKeyGenerator;
    
    // Redis键前缀 - 统一格式: ojp:{module}:{type}:{connHash}
    private static final String STATS_PREFIX = "ojp:stats:query:";
    private static final String QUERY_STATS_PREFIX = "ojp:stats:query:";
    private static final String CACHE_STATS_PREFIX = "ojp:cache:stats:";
    private static final String PERFORMANCE_PREFIX = "ojp:stats:performance:";
    private static final String TABLE_STATS_PREFIX = "ojp:stats:table:";
    private static final String HOURLY_STATS_PREFIX = "ojp:stats:hourly:";
    
    // 时间格式
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 异步记录查询统计
     * 包括查询次数、响应时间、缓存命中等指标
     */
    @Async
    public void recordQueryStats(String connHash, Query query, boolean cacheHit, long responseTime) {
        // 更新查询性能统计
        updateQueryPerformanceStats(connHash, query, responseTime);

        log.debug("异步统计记录完成: connHash={}, queryId={}, cacheHit={}, responseTime={}ms",
                connHash, query.getQueryId(), cacheHit, responseTime);
    }

    /**
     * 更新查询性能统计 - 使用时间分片存储优化
     */
    private void updateQueryPerformanceStats(String connHash, Query query, long responseTime) {
        LocalDateTime now = LocalDateTime.now();
        String key = PERFORMANCE_PREFIX + connHash + ":" + query.getQueryId() + ":" + SqlParseUtil.generateMD5Hash(query.getNormalizedSql());

        // 批量操作：使用Pipeline提高性能
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] keyBytes = key.getBytes();
            String queryType = query.getQueryType().name();
            String tables = String.join(",", query.getTables());
            String lastExecuted = String.valueOf(System.currentTimeMillis());

            connection.hIncrBy(keyBytes, "executionCount".getBytes(), 1);
            connection.hIncrBy(keyBytes, "totalResponseTime".getBytes(), responseTime);
            connection.hSet(keyBytes, "queryType".getBytes(), queryType.getBytes());
            connection.hSet(keyBytes, "tables".getBytes(), tables.getBytes());
            connection.hSet(keyBytes, "lastExecuted".getBytes(), lastExecuted.getBytes());
            connection.hSet(keyBytes, "connHash".getBytes(), connHash.getBytes());
            connection.expire(keyBytes, 31536000); // 365天过期
            
            return null;
        });

        // 更新最大响应时间
        String maxField = "maxResponseTime";
        Object currentMax = redisTemplate.opsForHash().get(key, maxField);
        if (currentMax == null || responseTime > Long.parseLong(currentMax.toString())) {
            redisTemplate.opsForHash().put(key, maxField, responseTime);
        }

        // 更新最小响应时间
        String minField = "minResponseTime";
        Object currentMin = redisTemplate.opsForHash().get(key, minField);
        if (currentMin == null || responseTime < Long.parseLong(currentMin.toString())) {
            redisTemplate.opsForHash().put(key, minField, responseTime);
        }
    }


    public List<QuerySummary> findAllSummaries() {
        // 使用SCAN命令查找所有匹配的性能统计键
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> result = new HashSet<>();
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions()
                    .match(PERFORMANCE_PREFIX + "*")
                    .count(1000)
                    .build());
            
            while (cursor.hasNext()) {
                result.add(new String(cursor.next()));
            }
            
            try {
                cursor.close();
            } catch (Exception e) {
                log.warn("Error closing cursor", e);
            }
            return result;
        });
        
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量查询所有哈希数据，使用原始连接操作避免序列化问题
        List<Map<String, String>> performanceDataList = new ArrayList<>();
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                Map<byte[], byte[]> rawData = connection.hGetAll(key.getBytes());
                Map<String, String> stringData = new HashMap<>();
                
                // 将字节数组转换为字符串
                for (Map.Entry<byte[], byte[]> entry : rawData.entrySet()) {
                    stringData.put(
                        new String(entry.getKey(), StandardCharsets.UTF_8),
                        new String(entry.getValue(), StandardCharsets.UTF_8)
                    );
                }
                
                performanceDataList.add(stringData);
            }
            return null;
        });
        
        // 转换结果为QuerySummary列表
        List<QuerySummary> summaries = new ArrayList<>();
        for (Map<String, String> data : performanceDataList) {
            // 创建QuerySummary对象
            QuerySummary summary = QuerySummary.builder()
                    .connHash(data.get("connHash"))
                    .accessCount(parseLongValue(data.get("executionCount")))
                    .avgResponseTime(calculateAvgResponseTime(
                            parseLongValue(data.get("totalResponseTime")), 
                            parseLongValue(data.get("executionCount"))))
                    .lastAccessTime(parseTimeValue(data.get("lastExecuted")))
                    .queryType(data.get("queryType"))
                    .tables(parseTableList(data.get("tables")))
                    .build();
            
            summaries.add(summary);
        }
        
        // 按总响应时间降序排序
        summaries.sort((a, b) -> {
            Long totalResponseTimeA = a.getAvgResponseTime() != null && a.getAccessCount() != null ? 
                    Math.round(a.getAvgResponseTime() * a.getAccessCount()) : 0L;
            Long totalResponseTimeB = b.getAvgResponseTime() != null && b.getAccessCount() != null ? 
                    Math.round(b.getAvgResponseTime() * b.getAccessCount()) : 0L;
            return totalResponseTimeB.compareTo(totalResponseTimeA);
        });
        
        return summaries;
    }
    
    private Long parseLongValue(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    private Double calculateAvgResponseTime(Long totalTime, Long count) {
        if (totalTime == null || count == null || count == 0) {
            return 0.0;
        }
        return (double) totalTime / count;
    }
    
    private LocalDateTime parseTimeValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            long timestamp = Long.parseLong(value);
            return LocalDateTime.ofInstant(new Date(timestamp).toInstant(), TimeZone.getDefault().toZoneId());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private List<String> parseTableList(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(value.split(","));
    }
    
    /**
     * 异步记录缓存操作统计 - 使用时间分片存储优化
     */
    @Async
    public void recordCacheOperation(String connHash, String operation, String cacheKey, long processingTime) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String readableConnId = "conn_" + connHash;
            
            // 实现多级时间分片：小时级、日级、月级
            String hourKey = "ojp:cache:ops:" + readableConnId + ":hour:" + now.format(HOUR_FORMATTER);
            String dayKey = "ojp:cache:ops:" + readableConnId + ":day:" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String monthKey = "ojp:cache:ops:" + readableConnId + ":month:" + now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            // 批量操作：使用Pipeline提高性能
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                // 小时级统计
                connection.hIncrBy(hourKey.getBytes(), operation.getBytes(), 1);
                connection.hIncrBy(hourKey.getBytes(), "totalProcessingTime".getBytes(), processingTime);
                connection.expire(hourKey.getBytes(), 86400); // 24小时过期
                
                // 日级统计
                connection.hIncrBy(dayKey.getBytes(), operation.getBytes(), 1);
                connection.hIncrBy(dayKey.getBytes(), "totalProcessingTime".getBytes(), processingTime);
                connection.expire(dayKey.getBytes(), 2592000); // 30天过期
                
                // 月级统计
                connection.hIncrBy(monthKey.getBytes(), operation.getBytes(), 1);
                connection.hIncrBy(monthKey.getBytes(), "totalProcessingTime".getBytes(), processingTime);
                connection.expire(monthKey.getBytes(), 31536000); // 365天过期
                
                return null;
            });
            
            log.debug("缓存操作统计记录(时间分片): connHash={}, operation={}, processingTime={}ms", 
                readableConnId, operation, processingTime);
                
        } catch (Exception e) {
            log.error("缓存操作统计记录失败: connHash={}, operation={}", connHash, operation, e);
        }
    }
    
    /**
     * 异步记录规则匹配统计 - 使用时间分片存储优化
     */
    @Async
    public void recordRuleMatchStats(String connHash, String ruleId, String ruleName, boolean matched, double score) {
        try {
            LocalDateTime now = LocalDateTime.now();
            // connHash由extractDatasourceName方法保证非空，直接使用
            String readableConnId = "conn_" + connHash;
            
            // 实现多级时间分片：小时级、日级、月级
            String hourKey = "ojp:cache:rule:" + readableConnId + ":" + ruleId + ":hour:" + now.format(HOUR_FORMATTER);
            String dayKey = "ojp:cache:rule:" + readableConnId + ":" + ruleId + ":day:" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String monthKey = "ojp:cache:rule:" + readableConnId + ":" + ruleId + ":month:" + now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            // 批量操作：使用Pipeline提高性能
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                long scoreInt = (long)(score * 100); // 保存为整数
                
                // 小时级统计
                connection.hIncrBy(hourKey.getBytes(), "totalEvaluations".getBytes(), 1);
                if (matched) {
                    connection.hIncrBy(hourKey.getBytes(), "matchCount".getBytes(), 1);
                    connection.hIncrBy(hourKey.getBytes(), "totalScore".getBytes(), scoreInt);
                }
                connection.hSet(hourKey.getBytes(), "ruleName".getBytes(), ruleName.getBytes());
                connection.hSet(hourKey.getBytes(), "lastEvaluated".getBytes(), String.valueOf(System.currentTimeMillis()).getBytes());
                connection.expire(hourKey.getBytes(), 86400); // 24小时过期
                
                // 日级统计
                connection.hIncrBy(dayKey.getBytes(), "totalEvaluations".getBytes(), 1);
                if (matched) {
                    connection.hIncrBy(dayKey.getBytes(), "matchCount".getBytes(), 1);
                    connection.hIncrBy(dayKey.getBytes(), "totalScore".getBytes(), scoreInt);
                }
                connection.hSet(dayKey.getBytes(), "ruleName".getBytes(), ruleName.getBytes());
                connection.hSet(dayKey.getBytes(), "lastEvaluated".getBytes(), String.valueOf(System.currentTimeMillis()).getBytes());
                connection.expire(dayKey.getBytes(), 2592000); // 30天过期
                
                // 月级统计
                connection.hIncrBy(monthKey.getBytes(), "totalEvaluations".getBytes(), 1);
                if (matched) {
                    connection.hIncrBy(monthKey.getBytes(), "matchCount".getBytes(), 1);
                    connection.hIncrBy(monthKey.getBytes(), "totalScore".getBytes(), scoreInt);
                }
                connection.hSet(monthKey.getBytes(), "ruleName".getBytes(), ruleName.getBytes());
                connection.hSet(monthKey.getBytes(), "lastEvaluated".getBytes(), String.valueOf(System.currentTimeMillis()).getBytes());
                connection.expire(monthKey.getBytes(), 31536000); // 365天过期
                
                return null;
            });
            
            log.debug("规则匹配统计记录(时间分片): datasource={}, ruleId={}, matched={}, score={}", 
                readableConnId, ruleId, matched, score);
                
        } catch (Exception e) {
            log.error("规则匹配统计记录失败: datasource={}, ruleId={}", connHash, ruleId, e);
        }
    }
    
    /**
     * 异步更新概览统计缓存
     */
    @Async
    public void updateOverviewStatsCache(Object overviewStats) {
        String key = STATS_PREFIX + "overview";
        redisTemplate.opsForValue().set(key, overviewStats, 5, TimeUnit.MINUTES);
        log.debug("概览统计缓存更新完成");
    }
    
    /**
     * 异步缓存表级别统计数据
     */
    @Async
    public void cacheTableStats(String connHash, String tableName, Object tableStats) {
        // connHash由extractDatasourceName方法保证非空，直接使用
        String readableConnId = "conn_" + connHash;
        String key = TABLE_STATS_PREFIX + readableConnId + ":" + tableName;
        redisTemplate.opsForValue().set(key, tableStats, 10, TimeUnit.MINUTES);
        log.debug("表统计缓存更新完成: connHash={}, tableName={}", readableConnId, tableName);
    }
    
    /**
     * 异步记录表访问统计
     */
    @Async
    public void recordTableAccess(String connHash, String tableName, boolean cached, long responseTime) {
        // connHash由extractDatasourceName方法保证非空，直接使用
        String readableConnId = "conn_" + connHash;
        String accessKey = TABLE_STATS_PREFIX + "access:" + readableConnId + ":" + tableName;
        
        // 增加访问计数
        redisTemplate.opsForHash().increment(accessKey, "totalAccess", 1);
        
        if (cached) {
            redisTemplate.opsForHash().increment(accessKey, "cachedAccess", 1);
        }
        
        // 更新响应时间统计
        redisTemplate.opsForHash().increment(accessKey, "totalResponseTime", responseTime);
        redisTemplate.opsForHash().put(accessKey, "lastAccessed", System.currentTimeMillis());
        
        // 设置过期时间（保留24小时）
        redisTemplate.expire(accessKey, 24, TimeUnit.HOURS);
        
        log.debug("表访问统计记录完成: connHash={}, tableName={}, cached={}", readableConnId, tableName, cached);
    }
    
    /**
     * 异步记录缓存操作统计
     */
    @Async
    public void recordCacheOperation(String connHash, String operation, int sqlLength, long responseTime, boolean success) {
        try {
            // connHash由extractDatasourceName方法保证非空，直接使用
            String readableConnId = "conn_" + connHash;
            String operationKey = CACHE_STATS_PREFIX + "operation:" + readableConnId + ":" + operation;
            
            // 增加操作计数
            redisTemplate.opsForHash().increment(operationKey, "totalCount", 1);
            
            if (success) {
                redisTemplate.opsForHash().increment(operationKey, "successCount", 1);
            }
            
            // 更新响应时间和SQL长度统计
            redisTemplate.opsForHash().increment(operationKey, "totalResponseTime", responseTime);
            redisTemplate.opsForHash().increment(operationKey, "totalSqlLength", sqlLength);
            redisTemplate.opsForHash().put(operationKey, "lastOperation", System.currentTimeMillis());
            
            // 设置过期时间（保留24小时）
            redisTemplate.expire(operationKey, 24, TimeUnit.HOURS);
            
            log.debug("缓存操作统计记录: connHash={}, operation={}, success={}, responseTime={}ms", 
                readableConnId, operation, success, responseTime);
                
        } catch (Exception e) {
            log.error("缓存操作统计记录失败: connHash={}, operation={}", connHash, operation, e);
        }
    }




}