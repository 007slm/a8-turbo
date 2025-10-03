package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Redis分级过期策略服务
 * 实现智能过期时间管理，根据数据类型和重要性设置不同的过期策略
 * 
 * 过期策略分级：
 * 1. 实时数据（小时级）：24小时过期
 * 2. 短期数据（日级）：30天过期
 * 3. 长期数据（月级）：365天过期
 * 4. 慢查询数据：7天过期
 * 5. 缓存规则数据：永久保留
 * 6. 临时数据：1小时过期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisExpirationService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 过期时间常量（秒）
    private static final long HOUR_EXPIRATION = 86400L;      // 24小时
    private static final long DAY_EXPIRATION = 2592000L;     // 30天
    private static final long MONTH_EXPIRATION = 31536000L;  // 365天
    private static final long SLOW_QUERY_EXPIRATION = 604800L; // 7天
    private static final long TEMP_EXPIRATION = 3600L;       // 1小时
    
    /**
     * 为小时级统计数据设置过期时间
     */
    public void setHourlyStatsExpiration(String key) {
        try {
            redisTemplate.expire(key, HOUR_EXPIRATION, TimeUnit.SECONDS);
            log.debug("设置小时级统计过期时间: key={}, expiration={}小时", key, HOUR_EXPIRATION / 3600);
        } catch (Exception e) {
            log.error("设置小时级统计过期时间失败: key={}", key, e);
        }
    }
    
    /**
     * 为日级统计数据设置过期时间
     */
    public void setDailyStatsExpiration(String key) {
        try {
            redisTemplate.expire(key, DAY_EXPIRATION, TimeUnit.SECONDS);
            log.debug("设置日级统计过期时间: key={}, expiration={}天", key, DAY_EXPIRATION / 86400);
        } catch (Exception e) {
            log.error("设置日级统计过期时间失败: key={}", key, e);
        }
    }
    
    /**
     * 为月级统计数据设置过期时间
     */
    public void setMonthlyStatsExpiration(String key) {
        try {
            redisTemplate.expire(key, MONTH_EXPIRATION, TimeUnit.SECONDS);
            log.debug("设置月级统计过期时间: key={}, expiration={}天", key, MONTH_EXPIRATION / 86400);
        } catch (Exception e) {
            log.error("设置月级统计过期时间失败: key={}", key, e);
        }
    }
    
    /**
     * 为慢查询数据设置过期时间
     */
    public void setSlowQueryExpiration(String key) {
        try {
            redisTemplate.expire(key, SLOW_QUERY_EXPIRATION, TimeUnit.SECONDS);
            log.debug("设置慢查询过期时间: key={}, expiration={}天", key, SLOW_QUERY_EXPIRATION / 86400);
        } catch (Exception e) {
            log.error("设置慢查询过期时间失败: key={}", key, e);
        }
    }
    
    /**
     * 为临时数据设置过期时间
     */
    public void setTempDataExpiration(String key) {
        try {
            redisTemplate.expire(key, TEMP_EXPIRATION, TimeUnit.SECONDS);
            log.debug("设置临时数据过期时间: key={}, expiration={}小时", key, TEMP_EXPIRATION / 3600);
        } catch (Exception e) {
            log.error("设置临时数据过期时间失败: key={}", key, e);
        }
    }
    
    /**
     * 智能设置过期时间（根据键名模式自动判断）
     */
    public void setSmartExpiration(String key) {
        try {
            if (key.contains(":hour:")) {
                setHourlyStatsExpiration(key);
            } else if (key.contains(":day:")) {
                setDailyStatsExpiration(key);
            } else if (key.contains(":month:")) {
                setMonthlyStatsExpiration(key);
            } else if (key.contains("slow")) {
                setSlowQueryExpiration(key);
            } else if (key.contains("temp") || key.contains("lock")) {
                setTempDataExpiration(key);
            } else {
                // 默认使用日级过期策略
                setDailyStatsExpiration(key);
                log.debug("使用默认过期策略: key={}", key);
            }
        } catch (Exception e) {
            log.error("智能设置过期时间失败: key={}", key, e);
        }
    }
    
    /**
     * 批量设置过期时间
     */
    public void setBatchExpiration(String[] keys, long expiration, TimeUnit timeUnit) {
        if (keys == null || keys.length == 0) {
            return;
        }
        
        try {
            for (String key : keys) {
                redisTemplate.expire(key, expiration, timeUnit);
            }
            log.debug("批量设置过期时间完成: count={}, expiration={} {}", 
                keys.length, expiration, timeUnit.name().toLowerCase());
        } catch (Exception e) {
            log.error("批量设置过期时间失败: count={}", keys.length, e);
        }
    }
    
    /**
     * 延长键的过期时间
     */
    public void extendExpiration(String key, long additionalTime, TimeUnit timeUnit) {
        try {
            Long currentTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (currentTtl != null && currentTtl > 0) {
                long newExpiration = currentTtl + timeUnit.toSeconds(additionalTime);
                redisTemplate.expire(key, newExpiration, TimeUnit.SECONDS);
                log.debug("延长过期时间: key={}, 原TTL={}s, 新TTL={}s", key, currentTtl, newExpiration);
            } else {
                // 如果键没有过期时间，使用智能设置
                setSmartExpiration(key);
            }
        } catch (Exception e) {
            log.error("延长过期时间失败: key={}", key, e);
        }
    }
    
    /**
     * 移除键的过期时间（设为永久）
     */
    public void removePersist(String key) {
        try {
            redisTemplate.persist(key);
            log.debug("移除过期时间: key={}", key);
        } catch (Exception e) {
            log.error("移除过期时间失败: key={}", key, e);
        }
    }
    
    /**
     * 获取键的剩余过期时间
     */
    public Long getTimeToLive(String key, TimeUnit timeUnit) {
        try {
            return redisTemplate.getExpire(key, timeUnit);
        } catch (Exception e) {
            log.error("获取过期时间失败: key={}", key, e);
            return null;
        }
    }
    
    /**
     * 检查键是否即将过期（剩余时间少于指定阈值）
     */
    public boolean isExpiringSoon(String key, long threshold, TimeUnit timeUnit) {
        try {
            Long ttl = redisTemplate.getExpire(key, timeUnit);
            return ttl != null && ttl > 0 && ttl <= threshold;
        } catch (Exception e) {
            log.error("检查过期状态失败: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 根据数据访问频率动态调整过期时间
     */
    public void adjustExpirationByAccessFrequency(String key, int accessCount, long baseExpiration, TimeUnit timeUnit) {
        try {
            // 根据访问频率计算调整因子
            double adjustmentFactor = calculateAdjustmentFactor(accessCount);
            long adjustedExpiration = (long) (baseExpiration * adjustmentFactor);
            
            redisTemplate.expire(key, adjustedExpiration, timeUnit);
            log.debug("动态调整过期时间: key={}, accessCount={}, factor={}, expiration={} {}", 
                key, accessCount, adjustmentFactor, adjustedExpiration, timeUnit.name().toLowerCase());
        } catch (Exception e) {
            log.error("动态调整过期时间失败: key={}", key, e);
        }
    }
    
    /**
     * 计算基于访问频率的调整因子
     */
    private double calculateAdjustmentFactor(int accessCount) {
        if (accessCount <= 1) {
            return 0.5; // 低频访问，缩短过期时间
        } else if (accessCount <= 10) {
            return 1.0; // 正常访问，保持原过期时间
        } else if (accessCount <= 100) {
            return 1.5; // 高频访问，延长过期时间
        } else {
            return 2.0; // 超高频访问，大幅延长过期时间
        }
    }
    
    /**
     * 根据时间模式设置渐进式过期时间
     */
    public void setProgressiveExpiration(String keyPattern, LocalDateTime startTime) {
        try {
            LocalDateTime now = LocalDateTime.now();
            long daysSinceStart = java.time.Duration.between(startTime, now).toDays();
            
            // 渐进式过期：数据越老，过期时间越短
            long expiration;
            if (daysSinceStart <= 1) {
                expiration = HOUR_EXPIRATION; // 最新数据保留24小时
            } else if (daysSinceStart <= 7) {
                expiration = DAY_EXPIRATION / 4; // 一周内数据保留7.5天
            } else if (daysSinceStart <= 30) {
                expiration = DAY_EXPIRATION / 2; // 一月内数据保留15天
            } else {
                expiration = DAY_EXPIRATION / 10; // 老数据保留3天
            }
            
            // 应用到匹配的键
            var keys = redisTemplate.keys(keyPattern);
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    redisTemplate.expire(key, expiration, TimeUnit.SECONDS);
                }
                log.debug("设置渐进式过期时间: pattern={}, count={}, expiration={}s", 
                    keyPattern, keys.size(), expiration);
            }
            
        } catch (Exception e) {
            log.error("设置渐进式过期时间失败: pattern={}", keyPattern, e);
        }
    }
    
    /**
     * 获取过期策略统计信息
     */
    public ExpirationStats getExpirationStats() {
        try {
            // 统计不同过期时间范围的键数量
            var allKeys = redisTemplate.keys("ojp:*");
            if (allKeys == null || allKeys.isEmpty()) {
                return ExpirationStats.builder().build();
            }
            
            long expiring1Hour = 0;
            long expiring1Day = 0;
            long expiring1Week = 0;
            long expiring1Month = 0;
            long persistent = 0;
            
            for (String key : allKeys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (ttl == null || ttl == -1) {
                    persistent++;
                } else if (ttl <= 3600) {
                    expiring1Hour++;
                } else if (ttl <= 86400) {
                    expiring1Day++;
                } else if (ttl <= 604800) {
                    expiring1Week++;
                } else {
                    expiring1Month++;
                }
            }
            
            return ExpirationStats.builder()
                .totalKeys(allKeys.size())
                .expiring1Hour(expiring1Hour)
                .expiring1Day(expiring1Day)
                .expiring1Week(expiring1Week)
                .expiring1Month(expiring1Month)
                .persistent(persistent)
                .checkTime(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("获取过期策略统计信息失败", e);
            return ExpirationStats.builder()
                .checkTime(LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * 过期策略统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class ExpirationStats {
        private long totalKeys;
        private long expiring1Hour;
        private long expiring1Day;
        private long expiring1Week;
        private long expiring1Month;
        private long persistent;
        private LocalDateTime checkTime;
    }
}