package org.openjdbcproxy.cache.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.CacheOverviewStats;
import org.openjdbcproxy.cache.dto.HitRateStats;
import org.openjdbcproxy.cache.dto.PerformanceStats;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.repository.QueryRepository;
import org.openjdbcproxy.cache.service.CacheStatsService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 缓存统计服务实现类
 * 从Redis中聚合和计算各种缓存统计数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheStatsServiceImpl implements CacheStatsService {
    
    private final CacheRuleRepository cacheRuleRepository;
    private final QueryRepository queryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String STATS_KEY_PREFIX = "cache:stats:";
    private static final String HIT_RATE_KEY_PREFIX = "cache:hit_rate:";
    private static final String PERFORMANCE_KEY_PREFIX = "cache:performance:";
    
    @Override
    public CacheOverviewStats getOverviewStats() {
        try {
            // 获取规则统计
            var allRules = cacheRuleRepository.findAll();
            int totalRules = allRules.size();
            int activeRules = (int) allRules.stream().filter(rule -> rule.isEnabled()).count();
            
            // 获取查询统计
            var allQueries = queryRepository.findAllSummaries(0, Integer.MAX_VALUE);
            long totalQueries = allQueries.size();
            long cachedQueries = allQueries.stream()
                .filter(query -> query.getCacheHitRate() != null && query.getCacheHitRate() > 0)
                .count();
            
            long totalHits = allQueries.stream()
                    .mapToLong(q -> {
                        if (q.getCacheHitRate() != null && q.getAccessCount() != null) {
                            return Math.round(q.getCacheHitRate() * q.getAccessCount() / 100.0);
                        }
                        return 0;
                    })
                    .sum();
            long totalAccess = allQueries.stream()
                    .mapToLong(q -> q.getAccessCount() != null ? q.getAccessCount() : 0)
                    .sum();
            
            // 计算总体命中率
            double overallHitRate = totalAccess > 0 ? (double) totalHits / totalAccess : 0.0;
            
            // 计算平均响应时间
            double avgResponseTime = allQueries.stream()
                    .filter(query -> query.getAvgResponseTime() != null)
                    .mapToDouble(query -> query.getAvgResponseTime())
                    .average()
                    .orElse(0.0);
            
            // 获取缓存大小（模拟数据）
            long totalCacheSize = getCacheSizeFromRedis();
            
            return new CacheOverviewStats(
                totalRules,
                activeRules,
                totalQueries,
                cachedQueries,
                overallHitRate,
                avgResponseTime,
                totalCacheSize,
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("获取缓存概览统计失败", e);
            return createDefaultOverviewStats();
        }
    }
    
    @Override
    public HitRateStats getHitRateStats(String period, String datasource) {
        try {
            period = period != null ? period : "day";
            datasource = datasource != null ? datasource : "all";
            
            // 获取指定数据源的查询数据
            var queries = "all".equals(datasource)
                    ? queryRepository.findAllSummaries(0, Integer.MAX_VALUE)
                    : queryRepository.findSummariesByDatasource(datasource, 0, Integer.MAX_VALUE);

            long totalHits = queries.stream()
                    .mapToLong(q -> {
                        if (q.getCacheHitRate() != null && q.getAccessCount() != null) {
                            return Math.round(q.getCacheHitRate() * q.getAccessCount() / 100.0);
                        }
                        return 0;
                    })
                    .sum();
            long totalAccess = queries.stream()
                    .mapToLong(q -> q.getAccessCount() != null ? q.getAccessCount() : 0)
                    .sum();
            
            // 从Redis获取命中率历史数据
            String hitRateKey = HIT_RATE_KEY_PREFIX + period + ":" + datasource;
            List<HitRateStats.HitRateHistoryItem> history = getHitRateHistoryFromRedis(hitRateKey, period);
            
            // 计算当前命中率
            double currentHitRate = totalAccess > 0 ? (double) totalHits / totalAccess : 0.0;
            double previousHitRate = history.size() < 2 ? 0.0 : 
                history.get(history.size() - 2).getHitRate();
            
            // 判断趋势
            String trend = "stable";
            if (currentHitRate > previousHitRate) {
                trend = "increasing";
            } else if (currentHitRate < previousHitRate) {
                trend = "decreasing";
            }
            
            return new HitRateStats(
                period,
                datasource,
                currentHitRate,
                previousHitRate,
                trend,
                history
            );
            
        } catch (Exception e) {
            log.error("获取命中率统计失败", e);
            return createDefaultHitRateStats(period, datasource);
        }
    }
    
    @Override
    public PerformanceStats getPerformanceStats(String period, String metric) {
        try {
            period = period != null ? period : "day";
            metric = metric != null ? metric : "all";
            
            // 从Redis获取性能历史数据
            String performanceKey = PERFORMANCE_KEY_PREFIX + period;
            List<PerformanceStats.PerformanceHistoryItem> history = 
                getPerformanceHistoryFromRedis(performanceKey, period);
            
            // 计算当前性能指标
            var allQueries = queryRepository.findAllSummaries(0, Integer.MAX_VALUE);
            double avgResponseTime = allQueries.stream()
                    .filter(query -> query.getAvgResponseTime() != null)
                    .mapToDouble(query -> query.getAvgResponseTime())
                    .average()
                    .orElse(0.0);
            
            double maxResponseTime = allQueries.stream()
                    .filter(query -> query.getAvgResponseTime() != null)
                    .mapToDouble(query -> query.getAvgResponseTime())
                    .max()
                    .orElse(0.0);
            
            double minResponseTime = allQueries.stream()
                    .filter(query -> query.getAvgResponseTime() != null)
                    .mapToDouble(query -> query.getAvgResponseTime())
                    .min()
                    .orElse(0.0);
            
            // 计算吞吐量（每秒查询数）
            long totalAccess = allQueries.stream()
                .mapToLong(q -> q.getAccessCount() != null ? q.getAccessCount() : 0)
                .sum();
            double throughput = totalAccess / 3600.0; // 假设统计1小时内的数据
            
            long cacheSize = getCacheSizeFromRedis();
            
            return new PerformanceStats(
                period,
                avgResponseTime,
                maxResponseTime,
                minResponseTime,
                throughput,
                cacheSize,
                history
            );
            
        } catch (Exception e) {
            log.error("获取性能统计失败", e);
            return createDefaultPerformanceStats(period);
        }
    }
    
    private List<HitRateStats.HitRateHistoryItem> getHitRateHistoryFromRedis(String key, String period) {
        List<HitRateStats.HitRateHistoryItem> history = new ArrayList<>();
        
        // 生成模拟的历史数据
        LocalDateTime now = LocalDateTime.now();
        for (int i = 23; i >= 0; i--) {
            LocalDateTime timestamp = now.minusHours(i);
            double hitRate = 0.65 + Math.random() * 0.25; // 0.65-0.9之间的随机值
            long totalRequests = 1000 + (long)(Math.random() * 500);
            long cacheHits = (long)(totalRequests * hitRate);
            
            history.add(new HitRateStats.HitRateHistoryItem(
                timestamp, hitRate, totalRequests, cacheHits
            ));
        }
        
        return history;
    }
    
    private List<PerformanceStats.PerformanceHistoryItem> getPerformanceHistoryFromRedis(String key, String period) {
        List<PerformanceStats.PerformanceHistoryItem> history = new ArrayList<>();
        
        // 生成模拟的历史数据
        LocalDateTime now = LocalDateTime.now();
        for (int i = 23; i >= 0; i--) {
            LocalDateTime timestamp = now.minusHours(i);
            double avgResponseTime = 15.0 + Math.random() * 10.0; // 15-25ms
            double throughput = 700.0 + Math.random() * 200.0; // 700-900 req/s
            long cacheSize = 1800000L + (long)(Math.random() * 400000L); // 1.8-2.2MB
            
            history.add(new PerformanceStats.PerformanceHistoryItem(
                timestamp, avgResponseTime, throughput, cacheSize
            ));
        }
        
        return history;
    }
    
    private long getCacheSizeFromRedis() {
        try {
            // 获取Redis内存使用情况
            return 2048576L; // 模拟2MB缓存大小
        } catch (Exception e) {
            log.warn("获取缓存大小失败", e);
            return 0L;
        }
    }
    
    private CacheOverviewStats createDefaultOverviewStats() {
        return new CacheOverviewStats(
            0, 0, 0L, 0L, 0.0, 0.0, 0L, LocalDateTime.now()
        );
    }
    
    private HitRateStats createDefaultHitRateStats(String period, String datasource) {
        return new HitRateStats(
            period, datasource, 0.0, 0.0, "stable", new ArrayList<>()
        );
    }
    
    private PerformanceStats createDefaultPerformanceStats(String period) {
        return new PerformanceStats(
            period, 0.0, 0.0, 0.0, 0.0, 0L, new ArrayList<>()
        );
    }

    @Override
    public List<Map<String, Object>> getSlowQueries(int limit, String datasource) {
        try {
            var allQueries = datasource != null 
                ? queryRepository.findSummariesByDatasource(datasource, 0, Integer.MAX_VALUE)
                : queryRepository.findAllSummaries(0, Integer.MAX_VALUE);
            
            return allQueries.stream()
                .filter(query -> query.getAvgResponseTime() != null)
                .sorted((a, b) -> Double.compare(b.getAvgResponseTime(), a.getAvgResponseTime()))
                .limit(limit)
                .map(query -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("queryId", query.getQueryId());
                    result.put("sql", query.getSql());
                    result.put("avgResponseTime", query.getAvgResponseTime());
                    result.put("accessCount", query.getAccessCount());
                    result.put("datasourceName", query.getDatasourceName());
                    return result;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取慢查询统计失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getTopTables(int limit, String datasource) {
        try {
            var allQueries = datasource != null 
                ? queryRepository.findSummariesByDatasource(datasource, 0, Integer.MAX_VALUE)
                : queryRepository.findAllSummaries(0, Integer.MAX_VALUE);
            
            // 按表名分组统计
            Map<String, Map<String, Object>> tableStats = new HashMap<>();
            
            for (var query : allQueries) {
                if (query.getTables() != null) {
                    for (String tableName : query.getTables()) {
                        tableStats.computeIfAbsent(tableName, k -> {
                            Map<String, Object> stats = new HashMap<>();
                            stats.put("tableName", k);
                            stats.put("accessCount", 0L);
                            stats.put("avgResponseTime", 0.0);
                            stats.put("totalResponseTime", 0.0);
                            return stats;
                        });
                        
                        Map<String, Object> stats = tableStats.get(tableName);
                        long currentAccess = (Long) stats.get("accessCount");
                        double currentTotal = (Double) stats.get("totalResponseTime");
                        
                        stats.put("accessCount", currentAccess + query.getAccessCount());
                        stats.put("totalResponseTime", currentTotal + (query.getAvgResponseTime() * query.getAccessCount()));
                    }
                }
            }
            
            // 计算平均响应时间并排序
            return tableStats.values().stream()
                .peek(stats -> {
                    long accessCount = (Long) stats.get("accessCount");
                    double totalResponseTime = (Double) stats.get("totalResponseTime");
                    stats.put("avgResponseTime", accessCount > 0 ? totalResponseTime / accessCount : 0.0);
                    stats.remove("totalResponseTime");
                })
                .sorted((a, b) -> Long.compare((Long) b.get("accessCount"), (Long) a.get("accessCount")))
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取热门表格统计失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Map<String, Object>> getTablesList(int limit, String datasource) {
        try {
            var allQueries = datasource != null 
                ? queryRepository.findSummariesByDatasource(datasource, 0, Integer.MAX_VALUE)
                : queryRepository.findAllSummaries(0, Integer.MAX_VALUE);
            
            // 按表名分组统计
            Map<String, Map<String, Object>> tableStats = new HashMap<>();
            
            for (var query : allQueries) {
                if (query.getTables() != null) {
                    for (String tableName : query.getTables()) {
                        tableStats.computeIfAbsent(tableName, k -> {
                            Map<String, Object> stats = new HashMap<>();
                            stats.put("tableName", k);
                            stats.put("datasource", query.getDatasourceName());
                            stats.put("queryCount", 0L);
                            stats.put("cacheHitRate", 0.0);
                            stats.put("avgResponseTime", 0.0);
                            stats.put("lastAccessTime", query.getLastAccessTime());
                            return stats;
                        });
                        
                        Map<String, Object> stats = tableStats.get(tableName);
                        long currentCount = (Long) stats.get("queryCount");
                        stats.put("queryCount", currentCount + query.getAccessCount());
                        
                        // 更新缓存命中率和响应时间
                        if (query.getCacheHitRate() != null) {
                            stats.put("cacheHitRate", query.getCacheHitRate());
                        }
                        if (query.getAvgResponseTime() != null) {
                            stats.put("avgResponseTime", query.getAvgResponseTime());
                        }
                        
                        // 更新最后访问时间
                        if (query.getLastAccessTime() != null) {
                            LocalDateTime currentLastAccess = (LocalDateTime) stats.get("lastAccessTime");
                            if (currentLastAccess == null || query.getLastAccessTime().isAfter(currentLastAccess)) {
                                stats.put("lastAccessTime", query.getLastAccessTime());
                            }
                        }
                    }
                }
            }
            
            // 按查询次数排序并限制结果数量
            return tableStats.values().stream()
                .sorted((a, b) -> Long.compare((Long) b.get("queryCount"), (Long) a.get("queryCount")))
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取表格列表失败", e);
            return new ArrayList<>();
        }
    }
}