package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis手动清理服务
 * 提供手动清理Redis数据的功能，优化Redis内存使用
 * 
 * 清理功能：
 * 1. 手动清理过期数据
 * 2. 清理孤立的索引键
 * 3. 内存优化和碎片整理
 * 4. 统计信息收集
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCleanupService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisExpirationService expirationService;
    
    // 时间格式化器
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    /**
     * 手动清理小时级数据
     * 清理过期的小时级数据和临时数据
     */
    public void manualHourlyCleanup() {
        log.info("开始执行手动小时级清理任务");
        
        try {
            // 清理过期的小时级统计数据
            cleanupExpiredHourlyStats();
            
            // 清理临时数据
            cleanupExpiredSlowQueries();
            
            // 清理孤立键
            cleanupOrphanedKeys();
            
            log.info("手动小时级清理任务完成");
        } catch (Exception e) {
            log.error("手动小时级清理任务执行失败", e);
        }
    }
    
    /**
     * 手动清理日级数据
     * 清理过期的日级数据和优化内存
     */
    public void manualDailyCleanup() {
        log.info("开始执行手动日级清理任务");
        
        try {
            // 清理过期的日级统计数据
            cleanupExpiredDailyStats();
            
            // 清理过期的慢查询数据
            cleanupExpiredSlowQueries();
            
            // 内存优化
            optimizeMemoryUsage();
            
            log.info("手动日级清理任务完成");
        } catch (Exception e) {
            log.error("手动日级清理任务执行失败", e);
        }
    }
    
    /**
     * 手动清理月级数据
     * 清理长期数据和深度优化
     */
    public void manualMonthlyCleanup() {
        log.info("开始执行手动月级清理任务");
        
        try {
            // 清理过期的月级统计数据
            cleanupExpiredMonthlyStats();
            
            // 清理孤立键
            cleanupOrphanedKeys();
            
            // 优化内存使用
            optimizeMemoryUsage();
            
            log.info("手动月级清理任务完成");
        } catch (Exception e) {
            log.error("手动月级清理任务执行失败", e);
        }
    }
    
    /**
     * 清理过期的小时级统计数据
     */
    private void cleanupExpiredHourlyStats() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        String cutoffHour = cutoffTime.format(HOUR_FORMATTER);
        
        // 清理缓存操作统计
        cleanupKeysByPattern("ojp:cache:ops:*:hour:*", cutoffHour, true);
        
        // 清理规则匹配统计
        cleanupKeysByPattern("ojp:cache:rule:*:hour:*", cutoffHour, true);
        
        // 清理表级统计
        cleanupKeysByPattern("ojp:stats:table:*:hour:*", cutoffHour, true);
        
        // 清理查询性能统计
        cleanupKeysByPattern("ojp:stats:perf:*:hour:*", cutoffHour, true);
        
        log.info("清理小时级统计数据完成，截止时间: {}", cutoffHour);
    }
    
    /**
     * 清理过期的日级统计数据
     */
    private void cleanupExpiredDailyStats() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
        String cutoffDay = cutoffTime.format(DAY_FORMATTER);
        
        // 清理缓存操作统计
        cleanupKeysByPattern("ojp:cache:ops:*:day:*", cutoffDay, false);
        
        // 清理规则匹配统计
        cleanupKeysByPattern("ojp:cache:rule:*:day:*", cutoffDay, false);
        
        // 清理表级统计
        cleanupKeysByPattern("ojp:stats:table:*:day:*", cutoffDay, false);
        
        // 清理查询性能统计
        cleanupKeysByPattern("ojp:stats:perf:*:day:*", cutoffDay, false);
        
        log.info("清理日级统计数据完成，截止时间: {}", cutoffDay);
    }
    
    /**
     * 清理过期的月级统计数据
     */
    private void cleanupExpiredMonthlyStats() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(365);
        String cutoffMonth = cutoffTime.format(MONTH_FORMATTER);
        
        // 清理缓存操作统计
        cleanupKeysByPattern("ojp:cache:ops:*:month:*", cutoffMonth, false);
        
        // 清理规则匹配统计
        cleanupKeysByPattern("ojp:cache:rule:*:month:*", cutoffMonth, false);
        
        // 清理表级统计
        cleanupKeysByPattern("ojp:stats:table:*:month:*", cutoffMonth, false);
        
        // 清理查询性能统计
        cleanupKeysByPattern("ojp:stats:perf:*:month:*", cutoffMonth, false);
        
        log.info("清理月级统计数据完成，截止时间: {}", cutoffMonth);
    }
    
    /**
     * 清理过期的慢查询数据
     */
    private void cleanupExpiredSlowQueries() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            long cutoffTimestamp = cutoffTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
            
            String indexKey = "ojp:stats:slow:index";
            
            // 从有序集合中移除过期的查询ID
            Long removedCount = redisTemplate.opsForZSet().removeRangeByScore(indexKey, 0, cutoffTimestamp);
            
            // 清理对应的Hash数据（通过扫描所有慢查询数据键）
            Set<String> dataKeys = redisTemplate.keys("ojp:stats:slow:data:*");
            if (dataKeys != null && !dataKeys.isEmpty()) {
                int cleanedDataKeys = 0;
                for (String dataKey : dataKeys) {
                    try {
                        Object timestampObj = redisTemplate.opsForHash().get(dataKey, "timestamp");
                        if (timestampObj != null) {
                            long timestamp = Long.parseLong(timestampObj.toString());
                            if (timestamp < cutoffTimestamp) {
                                redisTemplate.delete(dataKey);
                                cleanedDataKeys++;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("清理慢查询数据键失败: {}", dataKey, e);
                    }
                }
                log.info("清理过期慢查询数据完成，移除索引: {}, 清理数据键: {}", removedCount, cleanedDataKeys);
            }
            
        } catch (Exception e) {
            log.error("清理过期慢查询数据失败", e);
        }
    }
    
    /**
     * 清理孤立的数据键
     */
    private void cleanupOrphanedKeys() {
        try {
            // 查找可能的孤立键模式
            String[] orphanedPatterns = {
                "ojp:*:temp:*",
                "ojp:*:cache:expired:*",
                "ojp:*:lock:*"
            };
            
            int totalCleaned = 0;
            for (String pattern : orphanedPatterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    // 批量删除
                    redisTemplate.delete(keys);
                    totalCleaned += keys.size();
                }
            }
            
            if (totalCleaned > 0) {
                log.info("清理孤立键完成，总计: {}", totalCleaned);
            }
            
        } catch (Exception e) {
            log.error("清理孤立键失败", e);
        }
    }
    
    /**
     * 优化内存使用
     */
    private void optimizeMemoryUsage() {
        try {
            // 执行Redis内存优化命令
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                // 触发内存回收 - 使用MEMORY PURGE命令
                connection.execute("MEMORY", "PURGE".getBytes());
                return null;
            });
            
            log.info("Redis内存优化完成");
            
        } catch (Exception e) {
            log.error("Redis内存优化失败", e);
        }
    }
    
    /**
     * 根据模式和时间条件清理键
     */
    private void cleanupKeysByPattern(String pattern, String cutoffTime, boolean isHourly) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return;
            }
            
            int cleanedCount = 0;
            for (String key : keys) {
                try {
                    // 提取时间部分进行比较
                    String timeStr = extractTimeFromKey(key, isHourly);
                    if (timeStr != null && timeStr.compareTo(cutoffTime) < 0) {
                        redisTemplate.delete(key);
                        cleanedCount++;
                    }
                } catch (Exception e) {
                    log.warn("清理键失败: {}", key, e);
                }
            }
            
            if (cleanedCount > 0) {
                log.debug("清理模式 {} 的键完成，数量: {}", pattern, cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("清理模式 {} 的键失败", pattern, e);
        }
    }
    
    /**
     * 从键名中提取时间字符串
     */
    private String extractTimeFromKey(String key, boolean isHourly) {
        try {
            String[] parts = key.split(":");
            if (parts.length >= 2) {
                String timePart = parts[parts.length - 1];
                if (isHourly) {
                    // 小时格式: yyyy-MM-dd-HH
                    return timePart.length() >= 13 ? timePart : null;
                } else {
                    // 日/月格式: yyyy-MM-dd 或 yyyy-MM
                    return timePart.length() >= 7 ? timePart : null;
                }
            }
        } catch (Exception e) {
            log.warn("提取键时间失败: {}", key, e);
        }
        return null;
    }
    
    /**
     * 手动触发清理任务（用于管理接口）
     */
    public void manualCleanup(String type) {
        log.info("手动触发清理任务: {}", type);
        
        switch (type.toLowerCase()) {
            case "hourly":
                manualHourlyCleanup();
                break;
            case "daily":
                manualDailyCleanup();
                break;
            case "monthly":
                manualMonthlyCleanup();
                break;
            case "all":
                manualHourlyCleanup();
                manualDailyCleanup();
                manualMonthlyCleanup();
                break;
            default:
                log.warn("未知的清理类型: {}", type);
        }
    }
    
    /**
     * 获取清理统计信息
     */
    public CleanupStats getCleanupStats() {
        try {
            // 统计各类型键的数量
            long hourlyKeys = countKeysByPattern("ojp:*:*:hour:*");
            long dailyKeys = countKeysByPattern("ojp:*:*:day:*");
            long monthlyKeys = countKeysByPattern("ojp:*:*:month:*");
            long slowQueryKeys = countKeysByPattern("ojp:stats:slow:*");
            
            return CleanupStats.builder()
                .hourlyKeys(hourlyKeys)
                .dailyKeys(dailyKeys)
                .monthlyKeys(monthlyKeys)
                .slowQueryKeys(slowQueryKeys)
                .totalKeys(hourlyKeys + dailyKeys + monthlyKeys + slowQueryKeys)
                .lastCleanupTime(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("获取清理统计信息失败", e);
            return CleanupStats.builder()
                .totalKeys(0L)
                .lastCleanupTime(LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * 统计匹配模式的键数量
     */
    private long countKeysByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.warn("统计键数量失败: {}", pattern, e);
            return 0;
        }
    }
    
    /**
     * 清理统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class CleanupStats {
        private long hourlyKeys;
        private long dailyKeys;
        private long monthlyKeys;
        private long slowQueryKeys;
        private long totalKeys;
        private LocalDateTime lastCleanupTime;
    }
}