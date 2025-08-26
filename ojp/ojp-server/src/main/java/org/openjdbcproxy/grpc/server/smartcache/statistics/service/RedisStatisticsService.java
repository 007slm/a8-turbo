package org.openjdbcproxy.grpc.server.smartcache.statistics.service;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.smartcache.statistics.RedisStatisticsKeys;
import org.openjdbcproxy.grpc.server.smartcache.statistics.SqlStatisticsData;
import org.openjdbcproxy.grpc.server.smartcache.statistics.TableStatisticsData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis统计服务
 * 负责SQL统计数据和表统计数据的存储、查询和管理
 */
@Slf4j
@Service
public class RedisStatisticsService {
    
    @Autowired
    private RedisTemplate<String, Object> redisStatisticsTemplate;
    
    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;
    
    /**
     * 保存SQL统计数据
     */
    public void saveSqlStatistics(SqlStatisticsData statistics) {
        try {
            String key = RedisStatisticsKeys.sqlStatsKey(statistics.getQueryId());
            redisStatisticsTemplate.opsForValue().set(key, statistics);
            
            // 设置过期时间（7天）
            redisStatisticsTemplate.expire(key, 7, TimeUnit.DAYS);
            
            log.debug("SQL统计数据已保存: {}", statistics.getQueryId());
        } catch (Exception e) {
            log.error("保存SQL统计数据失败: {}", statistics.getQueryId(), e);
        }
    }
    
    /**
     * 获取SQL统计数据
     */
    public SqlStatisticsData getSqlStatistics(String queryId) {
        try {
            String key = RedisStatisticsKeys.sqlStatsKey(queryId);
            return (SqlStatisticsData) redisStatisticsTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取SQL统计数据失败: {}", queryId, e);
            return null;
        }
    }
    
    /**
     * 更新SQL统计数据
     */
    public void updateSqlStatistics(String queryId, double executionTime, boolean isCacheHit) {
        try {
            SqlStatisticsData statistics = getSqlStatistics(queryId);
            if (statistics == null) {
                log.warn("SQL统计数据不存在，无法更新: {}", queryId);
                return;
            }
            
            statistics.updateExecutionStats(executionTime);
            statistics.updateCacheStats(isCacheHit);
            
            saveSqlStatistics(statistics);
            
            log.debug("SQL统计数据已更新: {}", queryId);
        } catch (Exception e) {
            log.error("更新SQL统计数据失败: {}", queryId, e);
        }
    }
    
    /**
     * 保存表统计数据
     */
    public void saveTableStatistics(TableStatisticsData statistics) {
        try {
            String key = RedisStatisticsKeys.tableStatsKey(statistics.getTableName());
            redisStatisticsTemplate.opsForValue().set(key, statistics);
            
            // 设置过期时间（30天）
            redisStatisticsTemplate.expire(key, 30, TimeUnit.DAYS);
            
            log.debug("表统计数据已保存: {}", statistics.getTableName());
        } catch (Exception e) {
            log.error("保存表统计数据失败: {}", statistics.getTableName(), e);
        }
    }
    
    /**
     * 获取表统计数据
     */
    public TableStatisticsData getTableStatistics(String tableName) {
        try {
            String key = RedisStatisticsKeys.tableStatsKey(tableName);
            return (TableStatisticsData) redisStatisticsTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取表统计数据失败: {}", tableName, e);
            return null;
        }
    }
    
    /**
     * 更新表统计数据
     */
    public void updateTableStatistics(String tableName, double queryTime, boolean isCacheHit) {
        try {
            TableStatisticsData statistics = getTableStatistics(tableName);
            if (statistics == null) {
                // 创建新的表统计数据
                statistics = TableStatisticsData.builder()
                        .tableName(tableName)
                        .accessFrequency(0)
                        .averageQueryTime(0)
                        .maxQueryTime(0)
                        .minQueryTime(0)
                        .totalQueryTime(0)
                        .isCached(false)
                        .currentTtl(0)
                        .cacheHitCount(0)
                        .cacheMissCount(0)
                        .relatedQueryCount(0)
                        .build();
            }
            
            statistics.updateAccessStats(queryTime);
            statistics.updateCacheStats(isCacheHit);
            
            saveTableStatistics(statistics);
            
            log.debug("表统计数据已更新: {}", tableName);
        } catch (Exception e) {
            log.error("更新表统计数据失败: {}", tableName, e);
        }
    }
    
    /**
     * 获取所有SQL统计数据
     */
    public List<SqlStatisticsData> getAllSqlStatistics() {
        try {
            Set<String> keys = redisStatisticsTemplate.keys(RedisStatisticsKeys.SQL_STATS_HASH + ":*");
            List<SqlStatisticsData> statistics = new ArrayList<>();
            
            for (String key : keys) {
                SqlStatisticsData data = (SqlStatisticsData) redisStatisticsTemplate.opsForValue().get(key);
                if (data != null) {
                    statistics.add(data);
                }
            }
            
            return statistics;
        } catch (Exception e) {
            log.error("获取所有SQL统计数据失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取所有表统计数据
     */
    public List<TableStatisticsData> getAllTableStatistics() {
        try {
            Set<String> keys = redisStatisticsTemplate.keys(RedisStatisticsKeys.TABLE_STATS_HASH + ":*");
            List<TableStatisticsData> statistics = new ArrayList<>();
            
            for (String key : keys) {
                TableStatisticsData data = (TableStatisticsData) redisStatisticsTemplate.opsForValue().get(key);
                if (data != null) {
                    statistics.add(data);
                }
            }
            
            return statistics;
        } catch (Exception e) {
            log.error("获取所有表统计数据失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取热门SQL查询（按执行次数排序）
     */
    public List<SqlStatisticsData> getHotSqlQueries(int limit) {
        try {
            List<SqlStatisticsData> allStats = getAllSqlStatistics();
            allStats.sort((a, b) -> Long.compare(b.getExecutionCount(), a.getExecutionCount()));
            
            return allStats.stream()
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            log.error("获取热门SQL查询失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取慢查询（按平均执行时间排序）
     */
    public List<SqlStatisticsData> getSlowQueries(int limit) {
        try {
            List<SqlStatisticsData> allStats = getAllSqlStatistics();
            allStats.sort((a, b) -> Double.compare(b.getAverageExecutionTime(), a.getAverageExecutionTime()));
            
            return allStats.stream()
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            log.error("获取慢查询失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取热门表（按访问频率排序）
     */
    public List<TableStatisticsData> getHotTables(int limit) {
        try {
            List<TableStatisticsData> allStats = getAllTableStatistics();
            allStats.sort((a, b) -> Long.compare(b.getAccessFrequency(), a.getAccessFrequency()));
            
            return allStats.stream()
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            log.error("获取热门表失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取缓存命中率统计
     */
    public Map<String, Object> getCacheHitRateStats() {
        try {
            List<SqlStatisticsData> sqlStats = getAllSqlStatistics();
            List<TableStatisticsData> tableStats = getAllTableStatistics();
            
            long totalCacheHits = 0;
            long totalCacheMisses = 0;
            
            // 计算SQL查询的缓存命中率
            for (SqlStatisticsData stat : sqlStats) {
                totalCacheHits += stat.getCacheHitCount();
                totalCacheMisses += stat.getCacheMissCount();
            }
            
            // 计算表访问的缓存命中率
            for (TableStatisticsData stat : tableStats) {
                totalCacheHits += stat.getCacheHitCount();
                totalCacheMisses += stat.getCacheMissCount();
            }
            
            long totalCacheAccess = totalCacheHits + totalCacheMisses;
            double hitRate = totalCacheAccess > 0 ? (double) totalCacheHits / totalCacheAccess : 0.0;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCacheHits", totalCacheHits);
            stats.put("totalCacheMisses", totalCacheMisses);
            stats.put("totalCacheAccess", totalCacheAccess);
            stats.put("hitRate", hitRate);
            stats.put("hitRatePercentage", String.format("%.2f%%", hitRate * 100));
            
            return stats;
        } catch (Exception e) {
            log.error("获取缓存命中率统计失败", e);
            return new HashMap<>();
        }
    }
    
    /**
     * 清理过期的统计数据
     */
    public void cleanupExpiredStatistics() {
        try {
            // Redis会自动清理过期的键，这里可以添加额外的清理逻辑
            log.info("统计数据清理完成");
        } catch (Exception e) {
            log.error("清理过期统计数据失败", e);
        }
    }
}
