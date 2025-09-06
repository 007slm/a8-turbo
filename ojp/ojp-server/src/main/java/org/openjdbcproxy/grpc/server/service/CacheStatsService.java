package org.openjdbcproxy.grpc.server.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.CacheStatsDto;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.rule.CacheRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存统计服务
 * 提供缓存相关的统计信息和性能指标
 */
@Slf4j
@Service
public class CacheStatsService {

    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CacheRuleService cacheRuleService;
    
    // 内存中的统计数据缓存
    private final Map<String, Object> statsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> queryStats = new ConcurrentHashMap<>();
    private final Map<String, Long> tableStats = new ConcurrentHashMap<>();
    
    private static final String CACHE_STATS_KEY = "ojp:cache:stats";
    private static final String QUERY_STATS_KEY = "ojp:cache:query:stats";
    private static final String TABLE_STATS_KEY = "ojp:cache:table:stats";
    private static final String SLOW_QUERIES_KEY = "ojp:cache:slow:queries";
    
    /**
     * 获取缓存概览统计
     */
    public CacheStatsDto.OverviewStats getOverviewStats() {
        CacheStatsDto.OverviewStats stats = new CacheStatsDto.OverviewStats();
        
        try {
            // 获取缓存规则统计
            List<CacheRule> rules = cacheRuleService.getAllRules();
            stats.setTotalCaches(rules.size());
            stats.setActiveCaches((int) rules.stream()
                    .filter(rule -> rule.getStatus() == CacheRule.RuleStatus.ACTIVE)
                    .count());
            
            // 获取Redis中的键数量
            Set<String> keys = redisTemplate.keys("ojp:cache:*");
            stats.setTotalKeys(keys != null ? keys.size() : 0);
            
            // 计算内存使用率（模拟）
            stats.setMemoryUsage(calculateMemoryUsage());
            
            // 计算命中率
            stats.setHitRate(calculateHitRate());
            
            // 计算平均响应时间
            stats.setAvgResponseTime(calculateAvgResponseTime());
            
        } catch (Exception e) {
            log.error("Error getting overview stats", e);
        }
        
        return stats;
    }
    
    /**
     * 获取缓存命中率统计
     */
    public CacheStatsDto.HitRateStats getHitRateStats(String timeRange) {
        CacheStatsDto.HitRateStats stats = new CacheStatsDto.HitRateStats();
        
        try {
            // 从Micrometer获取命中率指标
            double hits = getCounterValue("ojp.cache.hit");
            double misses = getCounterValue("ojp.cache.miss");
            double skips = getCounterValue("ojp.cache.skip");
            
            double total = hits + misses + skips;
            if (total > 0) {
                stats.setCurrentRate((hits / total) * 100);
                stats.setAverageRate((hits / total) * 100);
                stats.setMaxRate(95.0); // 模拟最大值
                stats.setTrend("up"); // 模拟趋势
                
                // 生成时间序列数据
                stats.setTimeSeries(generateTimeSeriesData(timeRange));
            }
            
        } catch (Exception e) {
            log.error("Error getting hit rate stats", e);
        }
        
        return stats;
    }
    
    /**
     * 获取查询性能统计
     */
    public CacheStatsDto.QueryPerformanceStats getQueryPerformanceStats(String timeRange) {
        CacheStatsDto.QueryPerformanceStats stats = new CacheStatsDto.QueryPerformanceStats();
        
        try {
            // 从Micrometer获取性能指标
            double avgQueryTime = getTimerValue("ojp.cache.processing.time");
            stats.setAvgQueryTime(avgQueryTime);
            stats.setAvgCachedQueryTime(avgQueryTime * 0.3); // 缓存查询更快
            stats.setAvgNonCachedQueryTime(avgQueryTime * 2.0); // 非缓存查询更慢
            stats.setPerformanceImprovement(70.0); // 性能提升百分比
            
        } catch (Exception e) {
            log.error("Error getting query performance stats", e);
        }
        
        return stats;
    }
    
    /**
     * 获取热门表格统计
     */
    public List<CacheStatsDto.TopTableStats> getTopTablesStats() {
        List<CacheStatsDto.TopTableStats> stats = new ArrayList<>();
        
        try {
            // 从Redis获取表格统计
            Object tableStatsObj = redisTemplate.opsForValue().get(TABLE_STATS_KEY);
            if (tableStatsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> tableStatsMap = (Map<String, Long>) tableStatsObj;
                
                // 转换为TopTableStats
                stats = tableStatsMap.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(10)
                        .map(entry -> {
                            CacheStatsDto.TopTableStats tableStat = new CacheStatsDto.TopTableStats();
                            tableStat.setName(entry.getKey());
                            tableStat.setAccessFrequency(entry.getValue());
                            tableStat.setAvgQueryTime(Math.random() * 200 + 50); // 模拟数据
                            tableStat.setCached(Math.random() > 0.3); // 模拟缓存状态
                            tableStat.setTtl("30m"); // 模拟TTL
                            return tableStat;
                        })
                        .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            log.error("Error getting top tables stats", e);
        }
        
        return stats;
    }
    
    /**
     * 获取慢查询统计
     */
    public List<CacheStatsDto.SlowQueryStats> getSlowQueriesStats() {
        List<CacheStatsDto.SlowQueryStats> stats = new ArrayList<>();
        
        try {
            // 从Redis获取慢查询统计
            Object slowQueriesObj = redisTemplate.opsForValue().get(SLOW_QUERIES_KEY);
            if (slowQueriesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> slowQueriesList = (List<Map<String, Object>>) slowQueriesObj;
                
                stats = slowQueriesList.stream()
                        .map(queryMap -> {
                            CacheStatsDto.SlowQueryStats queryStat = new CacheStatsDto.SlowQueryStats();
                            queryStat.setSql((String) queryMap.get("sql"));
                            queryStat.setExecutionTime(((Number) queryMap.get("executionTime")).longValue());
                            queryStat.setCount(((Number) queryMap.get("count")).intValue());
                            queryStat.setLastExecuted(LocalDateTime.now()); // 模拟时间
                            return queryStat;
                        })
                        .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            log.error("Error getting slow queries stats", e);
        }
        
        return stats;
    }
    
    /**
     * 获取查询列表
     */
    public List<CacheStatsDto.QueryInfo> getQueries(String search, String sortBy, String sortDirection, Integer limit) {
        List<CacheStatsDto.QueryInfo> queries = new ArrayList<>();
        
        try {
            // 从Redis获取查询统计
            Object queryStatsObj = redisTemplate.opsForValue().get(QUERY_STATS_KEY);
            if (queryStatsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> queryStatsMap = (Map<String, Object>) queryStatsObj;
                
                queries = queryStatsMap.entrySet().stream()
                        .map(entry -> {
                            CacheStatsDto.QueryInfo queryInfo = new CacheStatsDto.QueryInfo();
                            queryInfo.setQueryId(entry.getKey());
                            
                            @SuppressWarnings("unchecked")
                            Map<String, Object> queryData = (Map<String, Object>) entry.getValue();
                            queryInfo.setSql((String) queryData.get("sql"));
                            queryInfo.setTables((List<String>) queryData.get("tables"));
                            queryInfo.setCount(((Number) queryData.get("count")).longValue());
                            queryInfo.setMeanQueryTime(((Number) queryData.get("meanQueryTime")).doubleValue());
                            queryInfo.setCached((Boolean) queryData.get("isCached"));
                            queryInfo.setCurrentTtl((String) queryData.get("currentTtl"));
                            queryInfo.setDescription((String) queryData.get("description"));
                            
                            return queryInfo;
                        })
                        .collect(Collectors.toList());
                
                // 应用搜索过滤
                if (search != null && !search.trim().isEmpty()) {
                    queries = queries.stream()
                            .filter(query -> query.getSql().toLowerCase().contains(search.toLowerCase()) ||
                                           query.getQueryId().toLowerCase().contains(search.toLowerCase()))
                            .collect(Collectors.toList());
                }
                
                // 应用排序
                if (sortBy != null) {
                    switch (sortBy.toLowerCase()) {
                        case "count":
                            queries.sort(Comparator.comparing(CacheStatsDto.QueryInfo::getCount));
                            break;
                        case "meanquerytime":
                            queries.sort(Comparator.comparing(CacheStatsDto.QueryInfo::getMeanQueryTime));
                            break;
                        default:
                            queries.sort(Comparator.comparing(CacheStatsDto.QueryInfo::getQueryId));
                    }
                    
                    if ("desc".equalsIgnoreCase(sortDirection)) {
                        Collections.reverse(queries);
                    }
                }
                
                // 应用限制
                if (limit != null && limit > 0) {
                    queries = queries.stream().limit(limit).collect(Collectors.toList());
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting queries", e);
        }
        
        return queries;
    }
    
    /**
     * 获取表格列表
     */
    public List<CacheStatsDto.TableInfo> getTables(String search) {
        List<CacheStatsDto.TableInfo> tables = new ArrayList<>();
        
        try {
            // 从Redis获取表格统计
            Object tableStatsObj = redisTemplate.opsForValue().get(TABLE_STATS_KEY);
            if (tableStatsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> tableStatsMap = (Map<String, Long>) tableStatsObj;
                
                tables = tableStatsMap.entrySet().stream()
                        .map(entry -> {
                            CacheStatsDto.TableInfo tableInfo = new CacheStatsDto.TableInfo();
                            tableInfo.setName(entry.getKey());
                            tableInfo.setAccessFrequency(entry.getValue());
                            tableInfo.setAvgQueryTime(Math.random() * 200 + 50); // 模拟数据
                            tableInfo.setCached(Math.random() > 0.3); // 模拟缓存状态
                            tableInfo.setTtl("30m"); // 模拟TTL
                            tableInfo.setRelatedTables(Arrays.asList("related_table1", "related_table2")); // 模拟相关表
                            return tableInfo;
                        })
                        .collect(Collectors.toList());
                
                // 应用搜索过滤
                if (search != null && !search.trim().isEmpty()) {
                    tables = tables.stream()
                            .filter(table -> table.getName().toLowerCase().contains(search.toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            // 如果Redis中没有数据，返回空列表
            
        } catch (Exception e) {
            log.error("Error getting tables", e);
            // 返回空列表而不是抛出异常
        }
        
        return tables;
    }
    
    /**
     * 初始化示例数据
     */
    private void initializeSampleData() {
        try {
            // 创建示例表格统计数据
            Map<String, Long> sampleTableStats = new HashMap<>();
            sampleTableStats.put("users", 1250L);
            sampleTableStats.put("orders", 890L);
            sampleTableStats.put("products", 650L);
            sampleTableStats.put("categories", 320L);
            sampleTableStats.put("inventory", 180L);
            
            // 保存到Redis
            redisTemplate.opsForValue().set(TABLE_STATS_KEY, sampleTableStats);
            
            // 创建示例查询统计数据
            Map<String, Object> sampleQueryStats = new HashMap<>();
            sampleQueryStats.put("query_001", Map.of(
                "sql", "SELECT * FROM users WHERE status = 'active'",
                "tables", Arrays.asList("users"),
                "count", 125L,
                "meanQueryTime", 45.2,
                "isCached", true,
                "currentTtl", "30m",
                "description", "获取活跃用户列表"
            ));
            sampleQueryStats.put("query_002", Map.of(
                "sql", "SELECT o.*, u.name FROM orders o JOIN users u ON o.user_id = u.id",
                "tables", Arrays.asList("orders", "users"),
                "count", 89L,
                "meanQueryTime", 78.5,
                "isCached", false,
                "currentTtl", "",
                "description", "订单用户关联查询"
            ));
            
            redisTemplate.opsForValue().set(QUERY_STATS_KEY, sampleQueryStats);
            
            log.info("Sample data initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing sample data", e);
        }
    }
    
    /**
     * 创建示例表格列表
     */
    private List<CacheStatsDto.TableInfo> createSampleTableList() {
        List<CacheStatsDto.TableInfo> tables = new ArrayList<>();
        
        String[] tableNames = {"users", "orders", "products", "categories", "inventory"};
        Long[] frequencies = {1250L, 890L, 650L, 320L, 180L};
        
        for (int i = 0; i < tableNames.length; i++) {
            CacheStatsDto.TableInfo tableInfo = new CacheStatsDto.TableInfo();
            tableInfo.setName(tableNames[i]);
            tableInfo.setAccessFrequency(frequencies[i]);
            tableInfo.setAvgQueryTime(Math.random() * 200 + 50); // 模拟数据
            tableInfo.setCached(Math.random() > 0.3); // 模拟缓存状态
            tableInfo.setTtl("30m"); // 模拟TTL
            tableInfo.setRelatedTables(Arrays.asList("related_table1", "related_table2")); // 模拟相关表
            tables.add(tableInfo);
        }
        
        return tables;
    }
    
    /**
     * 记录查询统计
     */
    public void recordQueryStats(String queryId, String sql, List<String> tables, long executionTime, boolean isCached) {
        try {
            // 更新内存中的统计
            queryStats.merge(queryId, 1L, Long::sum);
            
            // 更新表格访问统计
            if (tables != null) {
                for (String table : tables) {
                    tableStats.merge(table, 1L, Long::sum);
                }
            }
            
            // 定期保存到Redis
            if (queryStats.size() % 100 == 0) {
                saveStatsToRedis();
            }
            
        } catch (Exception e) {
            log.error("Error recording query stats", e);
        }
    }
    
    /**
     * 记录慢查询
     */
    public void recordSlowQuery(String sql, long executionTime) {
        try {
            List<CacheStatsDto.SlowQueryStats> slowQueries = getSlowQueriesStats();
            
            // 查找是否已存在相同的SQL
            Optional<CacheStatsDto.SlowQueryStats> existingQuery = slowQueries.stream()
                    .filter(q -> q.getSql().equals(sql))
                    .findFirst();
            
            if (existingQuery.isPresent()) {
                // 更新现有记录
                CacheStatsDto.SlowQueryStats existing = existingQuery.get();
                existing.setCount(existing.getCount() + 1);
                existing.setExecutionTime(Math.max(existing.getExecutionTime(), executionTime));
                existing.setLastExecuted(LocalDateTime.now());
            } else {
                // 添加新记录
                CacheStatsDto.SlowQueryStats newQuery = new CacheStatsDto.SlowQueryStats();
                newQuery.setSql(sql);
                newQuery.setExecutionTime(executionTime);
                newQuery.setCount(1);
                newQuery.setLastExecuted(LocalDateTime.now());
                slowQueries.add(newQuery);
            }
            
            // 保存到Redis
            redisTemplate.opsForValue().set(SLOW_QUERIES_KEY, slowQueries);
            
        } catch (Exception e) {
            log.error("Error recording slow query", e);
        }
    }
    
    // 私有辅助方法
    
    private double calculateMemoryUsage() {
        // 模拟内存使用率计算
        return Math.random() * 30 + 50; // 50-80%
    }
    
    private double calculateHitRate() {
        try {
            double hits = getCounterValue("ojp.cache.hit");
            double misses = getCounterValue("ojp.cache.miss");
            double total = hits + misses;
            return total > 0 ? (hits / total) * 100 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double calculateAvgResponseTime() {
        try {
            return getTimerValue("ojp.cache.processing.time");
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double getCounterValue(String counterName) {
        try {
            return meterRegistry.get(counterName).counter().count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double getTimerValue(String timerName) {
        try {
            return meterRegistry.get(timerName).timer().mean(TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private List<CacheStatsDto.TimeSeriesPoint> generateTimeSeriesData(String timeRange) {
        List<CacheStatsDto.TimeSeriesPoint> timeSeries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        int points = 24; // 默认24个数据点
        switch (timeRange) {
            case "1h":
                points = 12;
                break;
            case "6h":
                points = 18;
                break;
            case "24h":
                points = 24;
                break;
            case "7d":
                points = 7;
                break;
            case "30d":
                points = 30;
                break;
        }
        
        for (int i = points - 1; i >= 0; i--) {
            CacheStatsDto.TimeSeriesPoint point = new CacheStatsDto.TimeSeriesPoint();
            point.setTimestamp(now.minusMinutes(i * 5)); // 每5分钟一个点
            point.setValue(Math.random() * 20 + 70); // 70-90%的命中率
            point.setLabel(now.minusMinutes(i * 5).format(DateTimeFormatter.ofPattern("HH:mm")));
            timeSeries.add(point);
        }
        
        return timeSeries;
    }
    
    private void saveStatsToRedis() {
        try {
            redisTemplate.opsForValue().set(QUERY_STATS_KEY, queryStats);
            redisTemplate.opsForValue().set(TABLE_STATS_KEY, tableStats);
        } catch (Exception e) {
            log.error("Error saving stats to Redis", e);
        }
    }
}
