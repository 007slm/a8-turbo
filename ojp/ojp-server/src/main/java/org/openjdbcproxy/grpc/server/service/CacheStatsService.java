package org.openjdbcproxy.grpc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.CacheStatsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存统计服务
 * 使用Redis存储所有缓存逻辑，参考smart-redis-cache实现
 */
@Slf4j
@Service
public class CacheStatsService {
    
    @Autowired
    private RedisService redisService;

    // Redis键前缀
    private static final String CACHE_PREFIX = "ojp:cache";
    private static final String STATS_PREFIX = "ojp:stats";
    private static final String RULES_PREFIX = "ojp:rules";
    private static final String QUERIES_PREFIX = "ojp:queries";
    private static final String TABLES_PREFIX = "ojp:tables";
    
    /**
     * 获取缓存概览统计
     */
    public CacheStatsDto.CacheOverview getCacheOverview() {
        try {
            Long totalRequests = getTotalRequests();
            Long cacheHits = getCacheHits();
            Long cacheMisses = getCacheMisses();
            Long activeRules = getActiveRulesCount();
            Long cacheSize = getCacheSize();
            Long totalQueries = getTotalQueries();
            Long slowQueries = getSlowQueriesCount();

            Double hitRate = totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0.0;
        
        return CacheStatsDto.CacheOverview.builder()
                .totalRequests(totalRequests)
                .cacheHits(cacheHits)
                .cacheMisses(cacheMisses)
                .hitRate(hitRate)
                .activeRules(activeRules)
                .cacheSize(cacheSize)
                .totalQueries(totalQueries)
                .slowQueries(slowQueries)
                .lastUpdated(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            log.error("获取缓存概览统计失败", e);
            throw new RuntimeException("获取缓存概览统计失败", e);
        }
    }
    
    /**
     * 获取缓存命中率统计
     */
    public CacheStatsDto.HitRateStats getHitRateStats() {
        try {
            List<CacheStatsDto.HourlyHitRate> hourlyStats = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            for (int i = 23; i >= 0; i--) {
                LocalDateTime hour = now.minusHours(i);
                String hourKey = hour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH"));
                
                Long requests = getHourlyRequests(hourKey);
                Long hits = getHourlyHits(hourKey);
                Long misses = getHourlyMisses(hourKey);
                
                Double hitRate = requests > 0 ? (double) hits / requests * 100 : 0.0;
                Double avgResponseTime = getHourlyAvgResponseTime(hourKey);
                
                hourlyStats.add(CacheStatsDto.HourlyHitRate.builder()
                    .hour(hour.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .requests(requests)
                    .hits(hits)
                    .misses(misses)
                    .hitRate(hitRate)
                    .averageResponseTime(avgResponseTime)
                    .build());
            }

            Long totalRequests = getTotalRequests();
            Long totalHits = getCacheHits();
            Long totalMisses = getCacheMisses();
            Double averageHitRate = totalRequests > 0 ? (double) totalHits / totalRequests * 100 : 0.0;
        
        return CacheStatsDto.HitRateStats.builder()
                .hourlyStats(hourlyStats)
                .averageHitRate(averageHitRate)
                .totalRequests(totalRequests)
                .totalHits(totalHits)
                .totalMisses(totalMisses)
                .build();
        } catch (Exception e) {
            log.error("获取缓存命中率统计失败", e);
            throw new RuntimeException("获取缓存命中率统计失败", e);
        }
    }
    
    /**
     * 获取查询性能统计
     */
    public CacheStatsDto.QueryPerformanceStats getQueryPerformanceStats() {
        try {
            Set<String> queryKeys = redisService.keys(QUERIES_PREFIX + ":*");
            List<CacheStatsDto.QueryInfo> queries = new ArrayList<>();
            
            for (String queryKey : queryKeys) {
                String queryId = queryKey.substring(QUERIES_PREFIX.length() + 1);
                CacheStatsDto.QueryInfo queryInfo = getQueryInfo(queryId);
                if (queryInfo != null) {
                    queries.add(queryInfo);
                }
            }

            queries.sort((a, b) -> Long.compare(b.getExecutionCount(), a.getExecutionCount()));

            Long totalQueries = queries.stream().mapToLong(CacheStatsDto.QueryInfo::getExecutionCount).sum();
            Double averageExecutionTime = queries.stream()
                .mapToDouble(CacheStatsDto.QueryInfo::getAverageExecutionTime)
                .average()
                .orElse(0.0);
            Long slowQueries = queries.stream()
                .filter(q -> q.getAverageExecutionTime() > 1000.0)
                .count();
            Long cachedQueries = queries.stream()
                .filter(CacheStatsDto.QueryInfo::getIsCached)
                .count();
        
        return CacheStatsDto.QueryPerformanceStats.builder()
                .queries(queries)
                .totalQueries(totalQueries)
                .averageExecutionTime(averageExecutionTime)
                .slowQueries(slowQueries)
                .cachedQueries(cachedQueries)
                .build();
        } catch (Exception e) {
            log.error("获取查询性能统计失败", e);
            throw new RuntimeException("获取查询性能统计失败", e);
        }
    }
    
    /**
     * 获取热门表格统计
     */
    public CacheStatsDto.PopularTablesStats getPopularTablesStats() {
        try {
            Set<String> tableKeys = redisService.keys(TABLES_PREFIX + ":*");
            List<CacheStatsDto.TableInfo> tables = new ArrayList<>();
            
            for (String tableKey : tableKeys) {
                String tableName = tableKey.substring(TABLES_PREFIX.length() + 1);
                CacheStatsDto.TableInfo tableInfo = getTableInfo(tableName);
                if (tableInfo != null) {
                    tables.add(tableInfo);
                }
            }

            tables.sort((a, b) -> Long.compare(b.getAccessFrequency(), a.getAccessFrequency()));

            Long totalTables = (long) tables.size();
            Long totalQueries = tables.stream().mapToLong(CacheStatsDto.TableInfo::getAccessFrequency).sum();
            Double averageQueryTime = tables.stream()
                .mapToDouble(CacheStatsDto.TableInfo::getAverageQueryTime)
                .average()
                .orElse(0.0);
        
        return CacheStatsDto.PopularTablesStats.builder()
                .tables(tables)
                .totalTables(totalTables)
                .totalQueries(totalQueries)
                .averageQueryTime(averageQueryTime)
                .build();
        } catch (Exception e) {
            log.error("获取热门表格统计失败", e);
            throw new RuntimeException("获取热门表格统计失败", e);
        }
    }
    
    /**
     * 获取慢查询统计
     */
    public CacheStatsDto.SlowQueryStats getSlowQueryStats() {
        try {
            Set<String> queryKeys = redisService.keys(QUERIES_PREFIX + ":*");
            List<CacheStatsDto.SlowQueryInfo> slowQueries = new ArrayList<>();
            
            for (String queryKey : queryKeys) {
                String queryId = queryKey.substring(QUERIES_PREFIX.length() + 1);
                CacheStatsDto.QueryInfo queryInfo = getQueryInfo(queryId);
                if (queryInfo != null && queryInfo.getAverageExecutionTime() > 1000.0) {
                    slowQueries.add(CacheStatsDto.SlowQueryInfo.builder()
                        .queryId(queryInfo.getQueryId())
                        .sql(queryInfo.getSql())
                        .tableName(queryInfo.getTableName())
                        .executionCount(queryInfo.getExecutionCount())
                        .averageExecutionTime(queryInfo.getAverageExecutionTime())
                        .maxExecutionTime(queryInfo.getMaxExecutionTime())
                        .lastExecuted(queryInfo.getLastExecuted())
                        .isCached(queryInfo.getIsCached())
                        .currentTtl(queryInfo.getCurrentTtl())
                        .queryType(queryInfo.getQueryType())
                        .build());
                }
            }

            slowQueries.sort((a, b) -> Double.compare(b.getAverageExecutionTime(), a.getAverageExecutionTime()));

            Long totalSlowQueries = (long) slowQueries.size();
            Double threshold = 1000.0;
            Double averageSlowQueryTime = slowQueries.stream()
                .mapToDouble(CacheStatsDto.SlowQueryInfo::getAverageExecutionTime)
                .average()
                .orElse(0.0);
        
        return CacheStatsDto.SlowQueryStats.builder()
                .slowQueries(slowQueries)
                .totalSlowQueries(totalSlowQueries)
                .threshold(threshold)
                .averageSlowQueryTime(averageSlowQueryTime)
                .build();
        } catch (Exception e) {
            log.error("获取慢查询统计失败", e);
            throw new RuntimeException("获取慢查询统计失败", e);
        }
    }

    /**
     * 获取查询列表
     */
    public CacheStatsDto.QueryListResponse getQueries(String tableName, Integer limit) {
        try {
            Set<String> queryKeys = redisService.keys(QUERIES_PREFIX + ":*");
            List<CacheStatsDto.QueryInfo> queries = new ArrayList<>();
            
            for (String queryKey : queryKeys) {
                String queryId = queryKey.substring(QUERIES_PREFIX.length() + 1);
                CacheStatsDto.QueryInfo queryInfo = getQueryInfo(queryId);
                if (queryInfo != null) {
                    if (tableName == null || tableName.equals(queryInfo.getTableName())) {
                        queries.add(queryInfo);
                    }
                }
            }

            // 按执行次数排序
            queries.sort((a, b) -> Long.compare(b.getExecutionCount(), a.getExecutionCount()));

            // 限制数量
            if (limit != null && limit > 0) {
                queries = queries.subList(0, Math.min(limit, queries.size()));
            }

            return CacheStatsDto.QueryListResponse.builder()
                .queries(queries)
                .totalCount((long) queries.size())
                .page(1)
                .size(queries.size())
                .tableName(tableName)
                .build();
        } catch (Exception e) {
            log.error("获取查询列表失败", e);
            throw new RuntimeException("获取查询列表失败", e);
        }
    }

    /**
     * 获取表格列表
     */
    public CacheStatsDto.TableListResponse getTables(Integer limit) {
        try {
            Set<String> tableKeys = redisService.keys(TABLES_PREFIX + ":*");
            List<CacheStatsDto.TableInfo> tables = new ArrayList<>();
            
            for (String tableKey : tableKeys) {
                String tableName = tableKey.substring(TABLES_PREFIX.length() + 1);
                CacheStatsDto.TableInfo tableInfo = getTableInfo(tableName);
                if (tableInfo != null) {
                    tables.add(tableInfo);
                }
            }

            // 按访问频率排序
            tables.sort((a, b) -> Long.compare(b.getAccessFrequency(), a.getAccessFrequency()));

            // 限制数量
            if (limit != null && limit > 0) {
                tables = tables.subList(0, Math.min(limit, tables.size()));
            }

            return CacheStatsDto.TableListResponse.builder()
                .tables(tables)
                .totalCount((long) tables.size())
                .page(1)
                .size(tables.size())
                .build();
        } catch (Exception e) {
            log.error("获取表格列表失败", e);
            throw new RuntimeException("获取表格列表失败", e);
        }
    }

    /**
     * 获取表格统计信息
     */
    public CacheStatsDto.TableStatsInfo getTableStats(String tableName) {
        try {
            CacheStatsDto.TableInfo tableInfo = getTableInfo(tableName);
            if (tableInfo == null) {
                throw new RuntimeException("表格不存在: " + tableName);
            }

            // 获取相关查询
            List<CacheStatsDto.QueryInfo> relatedQueries = getQueries(tableName, 10).getQueries();

            return CacheStatsDto.TableStatsInfo.builder()
                .tableName(tableName)
                .queryCount(tableInfo.getAccessFrequency())
                .cacheHitRate(tableInfo.getCacheHitCount() > 0 ? 
                    (double) tableInfo.getCacheHitCount() / (tableInfo.getCacheHitCount() + tableInfo.getCacheMissCount()) * 100 : 0.0)
                .averageExecutionTime(tableInfo.getAverageQueryTime())
                .maxExecutionTime(tableInfo.getMaxQueryTime())
                .minExecutionTime(tableInfo.getMinQueryTime())
                .totalExecutionTime(tableInfo.getTotalQueryTime())
                .lastUpdated(LocalDateTime.now())
                .firstAccessTime(tableInfo.getFirstAccessTime())
                .isCached(tableInfo.getIsCached())
                .currentTtl(tableInfo.getCurrentTtl())
                .cacheHitCount(tableInfo.getCacheHitCount())
                .cacheMissCount(tableInfo.getCacheMissCount())
                .relatedQueries(relatedQueries)
                .build();
        } catch (Exception e) {
            log.error("获取表格统计信息失败: {}", tableName, e);
            throw new RuntimeException("获取表格统计信息失败", e);
        }
    }

    /**
     * 记录查询统计信息
     */
    public void recordQuery(String tableName, boolean isCached, boolean isHit, double executionTime) {
        try {
            String queryId = generateQueryId(tableName, isCached);
            LocalDateTime now = LocalDateTime.now();

            updateQueryStats(queryId, tableName, isCached, isHit, executionTime, now);
            updateTableStats(tableName, isCached, isHit, executionTime, now);
            updateGlobalStats(isCached, isHit, executionTime, now);

            log.debug("记录查询统计: table={}, cached={}, hit={}, time={}", 
                     tableName, isCached, isHit, executionTime);
        } catch (Exception e) {
            log.error("记录查询统计失败", e);
        }
    }

    // ====================== 私有辅助方法 ======================

    private Long getTotalRequests() {
        String key = STATS_PREFIX + ":total:requests";
        Object value = redisService.get(key);
        return value != null ? Long.valueOf(value.toString()) : 0L;
    }

    private Long getCacheHits() {
        String key = STATS_PREFIX + ":total:hits";
        Object value = redisService.get(key);
        return value != null ? Long.valueOf(value.toString()) : 0L;
    }

    private Long getCacheMisses() {
        String key = STATS_PREFIX + ":total:misses";
        Object value = redisService.get(key);
        return value != null ? Long.valueOf(value.toString()) : 0L;
    }

    private Long getActiveRulesCount() {
        Set<String> ruleKeys = redisService.keys(RULES_PREFIX + ":*");
        return (long) ruleKeys.size();
    }

    private Long getCacheSize() {
        String key = STATS_PREFIX + ":cache:size";
        Object value = redisService.get(key);
        return value != null ? Long.valueOf(value.toString()) : 0L;
    }

    private Long getTotalQueries() {
        Set<String> queryKeys = redisService.keys(QUERIES_PREFIX + ":*");
        return (long) queryKeys.size();
    }

    private Long getSlowQueriesCount() {
        Set<String> queryKeys = redisService.keys(QUERIES_PREFIX + ":*");
        long count = 0;
        for (String queryKey : queryKeys) {
            String queryId = queryKey.substring(QUERIES_PREFIX.length() + 1);
            CacheStatsDto.QueryInfo queryInfo = getQueryInfo(queryId);
            if (queryInfo != null && queryInfo.getAverageExecutionTime() > 1000.0) {
                count++;
            }
        }
        return count;
    }

    private Long getHourlyRequests(String hourKey) {
        String key = STATS_PREFIX + ":hourly:" + hourKey + ":requests";
        Object value = redisService.get(key);
        return value != null ? Long.valueOf(value.toString()) : 0L;
    }

    private Long getHourlyHits(String hourKey) {
        String key = STATS_PREFIX + ":hourly:" + hourKey + ":hits";
        Object value = redisService.get(key);
        return value != null ? Long.valueOf(value.toString()) : 0L;
    }

    private Long getHourlyMisses(String hourKey) {
        String key = STATS_PREFIX + ":hourly:" + hourKey + ":misses";
        Object value = redisService.get(key);
        return value != null ? Long.valueOf(value.toString()) : 0L;
    }

    private Double getHourlyAvgResponseTime(String hourKey) {
        String key = STATS_PREFIX + ":hourly:" + hourKey + ":avg_time";
        Object value = redisService.get(key);
        return value != null ? Double.valueOf(value.toString()) : 0.0;
    }

    private CacheStatsDto.QueryInfo getQueryInfo(String queryId) {
        String key = QUERIES_PREFIX + ":" + queryId;
        return redisService.get(key, CacheStatsDto.QueryInfo.class);
    }

    private CacheStatsDto.TableInfo getTableInfo(String tableName) {
        String key = TABLES_PREFIX + ":" + tableName;
        return redisService.get(key, CacheStatsDto.TableInfo.class);
    }

    private String generateQueryId(String tableName, boolean isCached) {
        return tableName + "_" + (isCached ? "cached" : "direct") + "_" + System.currentTimeMillis();
    }

    private void updateQueryStats(String queryId, String tableName, boolean isCached, boolean isHit, 
                                 double executionTime, LocalDateTime now) {
        String key = QUERIES_PREFIX + ":" + queryId;
        CacheStatsDto.QueryInfo queryInfo = redisService.get(key, CacheStatsDto.QueryInfo.class);
        
        if (queryInfo == null) {
            queryInfo = CacheStatsDto.QueryInfo.builder()
                .queryId(queryId)
                .sql("SELECT * FROM " + tableName)
                .tableName(tableName)
                .executionCount(0L)
                .averageExecutionTime(0.0)
                .maxExecutionTime(0.0)
                .minExecutionTime(Double.MAX_VALUE)
                .totalExecutionTime(0L)
                .lastExecuted(now)
                .isCached(isCached)
                .cacheHitCount(0L)
                .cacheMissCount(0L)
                .queryType("SELECT")
                .currentTtl(isCached ? "30m" : "0")
                .build();
        }

        queryInfo.setExecutionCount(queryInfo.getExecutionCount() + 1);
        queryInfo.setTotalExecutionTime(queryInfo.getTotalExecutionTime() + (long) executionTime);
        queryInfo.setAverageExecutionTime((double) queryInfo.getTotalExecutionTime() / queryInfo.getExecutionCount());
        queryInfo.setMaxExecutionTime(Math.max(queryInfo.getMaxExecutionTime(), executionTime));
        queryInfo.setMinExecutionTime(Math.min(queryInfo.getMinExecutionTime(), executionTime));
        queryInfo.setLastExecuted(now);

        if (isHit) {
            queryInfo.setCacheHitCount(queryInfo.getCacheHitCount() + 1);
        } else {
            queryInfo.setCacheMissCount(queryInfo.getCacheMissCount() + 1);
        }

        redisService.set(key, queryInfo, 30, TimeUnit.DAYS);
    }

    private void updateTableStats(String tableName, boolean isCached, boolean isHit, 
                                 double executionTime, LocalDateTime now) {
        String key = TABLES_PREFIX + ":" + tableName;
        CacheStatsDto.TableInfo tableInfo = redisService.get(key, CacheStatsDto.TableInfo.class);
        
        if (tableInfo == null) {
            tableInfo = CacheStatsDto.TableInfo.builder()
                .tableName(tableName)
                .accessFrequency(0L)
                .averageQueryTime(0.0)
                .maxQueryTime(0.0)
                .minQueryTime(Double.MAX_VALUE)
                .totalQueryTime(0L)
                .lastAccessTime(now)
                .firstAccessTime(now)
                .isCached(isCached)
                .currentTtl(isCached ? "30m" : "0")
                .cacheHitCount(0L)
                .cacheMissCount(0L)
                .relatedQueryCount(0L)
                .build();
        }

        tableInfo.setAccessFrequency(tableInfo.getAccessFrequency() + 1);
        tableInfo.setTotalQueryTime(tableInfo.getTotalQueryTime() + (long) executionTime);
        tableInfo.setAverageQueryTime((double) tableInfo.getTotalQueryTime() / tableInfo.getAccessFrequency());
        tableInfo.setMaxQueryTime(Math.max(tableInfo.getMaxQueryTime(), executionTime));
        tableInfo.setMinQueryTime(Math.min(tableInfo.getMinQueryTime(), executionTime));
        tableInfo.setLastAccessTime(now);

        if (isHit) {
            tableInfo.setCacheHitCount(tableInfo.getCacheHitCount() + 1);
        } else {
            tableInfo.setCacheMissCount(tableInfo.getCacheMissCount() + 1);
        }

        redisService.set(key, tableInfo, 7, TimeUnit.DAYS);
    }

    private void updateGlobalStats(boolean isCached, boolean isHit, double executionTime, LocalDateTime now) {
        String totalRequestsKey = STATS_PREFIX + ":total:requests";
        redisService.increment(totalRequestsKey);

        if (isHit) {
            String hitsKey = STATS_PREFIX + ":total:hits";
            redisService.increment(hitsKey);
            redisService.expire(hitsKey, 7, TimeUnit.DAYS);
        } else {
            String missesKey = STATS_PREFIX + ":total:misses";
            redisService.increment(missesKey);
            redisService.expire(missesKey, 7, TimeUnit.DAYS);
        }

        String hourKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH"));
        String hourlyRequestsKey = STATS_PREFIX + ":hourly:" + hourKey + ":requests";
        String hourlyHitsKey = STATS_PREFIX + ":hourly:" + hourKey + ":hits";
        String hourlyMissesKey = STATS_PREFIX + ":hourly:" + hourKey + ":misses";

        redisService.increment(hourlyRequestsKey);
        if (isHit) {
            redisService.increment(hourlyHitsKey);
        } else {
            redisService.increment(hourlyMissesKey);
        }
        
        redisService.expire(totalRequestsKey, 7, TimeUnit.DAYS);
    }
}
