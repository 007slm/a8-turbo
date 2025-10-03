package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 性能监控服务
 * 参考e:\ojp项目的监控机制，提供查询性能和缓存效果统计
 * 集成Micrometer指标收集，支持Prometheus导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMonitoringService {
    
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 性能阈值常量
    private static final long SLOW_QUERY_THRESHOLD_MS = 5000; // 5秒
    private static final long SLOW_CACHE_THRESHOLD_MS = 1000; // 1秒
    
    // Micrometer指标
    private Counter cacheHitCounter;
    private Counter cacheMissCounter;
    private Counter cacheSkipCounter;
    private Counter queryExecutionCounter;
    private Counter queryErrorCounter;
    private Timer cacheProcessingTimer;
    private Timer queryExecutionTimer;
    
    // 实时统计数据
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong slowQueries = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final Map<String, AtomicLong> datasourceStats = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeMetrics() {
        // 初始化Micrometer指标
        cacheHitCounter = Counter.builder("ojp.cache.hit")
            .description("缓存命中次数")
            .register(meterRegistry);
            
        cacheMissCounter = Counter.builder("ojp.cache.miss")
            .description("缓存未命中次数")
            .register(meterRegistry);
            
        cacheSkipCounter = Counter.builder("ojp.cache.skip")
            .description("缓存跳过次数")
            .register(meterRegistry);
            
        queryExecutionCounter = Counter.builder("ojp.query.execution")
            .description("查询执行次数")
            .register(meterRegistry);
            
        queryErrorCounter = Counter.builder("ojp.query.error")
            .description("查询错误次数")
            .register(meterRegistry);
            
        cacheProcessingTimer = Timer.builder("ojp.cache.processing.time")
            .description("缓存处理时间")
            .register(meterRegistry);
            
        queryExecutionTimer = Timer.builder("ojp.query.execution.time")
            .description("查询执行时间")
            .register(meterRegistry);
            
        // 注册Gauge指标
        Gauge.builder("ojp.cache.hit.rate", this, PerformanceMonitoringService::getCacheHitRate)
            .description("缓存命中率")
            .register(meterRegistry);
            
        Gauge.builder("ojp.query.slow.rate", this, PerformanceMonitoringService::getSlowQueryRate)
            .description("慢查询比例")
            .register(meterRegistry);
            
        log.info("性能监控指标初始化完成");
    }
    
    /**
     * 记录缓存命中
     */
    @Async
    public void recordCacheHit(String datasource, String sql, long processingTime) {
        cacheHitCounter.increment();
        cacheHits.incrementAndGet();
        cacheProcessingTimer.record(processingTime, TimeUnit.MILLISECONDS);
        
        updateDatasourceStats(datasource, "hit");
        
        if (processingTime > SLOW_CACHE_THRESHOLD_MS) {
            log.warn("慢缓存操作检测: datasource={}, processingTime={}ms, sql={}", 
                datasource, processingTime, truncateSql(sql));
        }
        
        log.debug("缓存命中记录: datasource={}, processingTime={}ms", datasource, processingTime);
    }
    
    /**
     * 记录缓存未命中
     */
    @Async
    public void recordCacheMiss(String datasource, String sql, long processingTime) {
        cacheMissCounter.increment();
        cacheMisses.incrementAndGet();
        cacheProcessingTimer.record(processingTime, TimeUnit.MILLISECONDS);
        
        updateDatasourceStats(datasource, "miss");
        
        log.debug("缓存未命中记录: datasource={}, processingTime={}ms", datasource, processingTime);
    }
    
    /**
     * 记录缓存跳过
     */
    @Async
    public void recordCacheSkip(String datasource, String reason, String sql) {
        cacheSkipCounter.increment();
        updateDatasourceStats(datasource, "skip");
        
        log.debug("缓存跳过记录: datasource={}, reason={}", datasource, reason);
    }
    
    /**
     * 记录查询执行
     */
    @Async
    public void recordQueryExecution(String datasource, String sql, long executionTime, boolean success) {
        queryExecutionCounter.increment();
        totalQueries.incrementAndGet();
        queryExecutionTimer.record(executionTime, TimeUnit.MILLISECONDS);
        
        if (!success) {
            queryErrorCounter.increment();
        }
        
        if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
            slowQueries.incrementAndGet();
            log.warn("慢查询检测: datasource={}, executionTime={}ms, success={}, sql={}", 
                datasource, executionTime, success, truncateSql(sql));
        }
        
        updateDatasourceStats(datasource, success ? "success" : "error");
        
        log.debug("查询执行记录: datasource={}, executionTime={}ms, success={}", 
            datasource, executionTime, success);
    }
    
    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    /**
     * 获取慢查询比例
     */
    public double getSlowQueryRate() {
        long total = totalQueries.get();
        long slow = slowQueries.get();
        return total > 0 ? (double) slow / total : 0.0;
    }
    
    /**
     * 获取缓存性能统计数据
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("cacheHits", cacheHits.get());
        stats.put("cacheMisses", cacheMisses.get());
        stats.put("cacheHitRate", getCacheHitRate());
        stats.put("totalCacheOperations", cacheHits.get() + cacheMisses.get());
        
        return stats;
    }
    
    /**
     * 获取查询性能统计数据
     */
    public Map<String, Object> getQueryStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("totalQueries", totalQueries.get());
        stats.put("slowQueries", slowQueries.get());
        stats.put("slowQueryRate", getSlowQueryRate());
        stats.put("averageQueryTime", getAverageQueryTime());
        
        return stats;
    }
    
    /**
     * 获取指定数据源的性能统计
     */
    public Map<String, Object> getDatasourceStats(String datasource) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        // 获取该数据源的统计数据
        long queries = datasourceStats.getOrDefault(datasource + ".query", new AtomicLong(0)).get();
        long hits = datasourceStats.getOrDefault(datasource + ".hit", new AtomicLong(0)).get();
        long misses = datasourceStats.getOrDefault(datasource + ".miss", new AtomicLong(0)).get();
        
        stats.put("datasource", datasource);
        stats.put("totalQueries", queries);
        stats.put("cacheHits", hits);
        stats.put("cacheMisses", misses);
        stats.put("hitRate", hits + misses > 0 ? (double) hits / (hits + misses) : 0.0);
        
        return stats;
    }
    
    /**
     * 获取性能阈值告警信息
     */
    public Map<String, Object> getPerformanceAlerts() {
        Map<String, Object> alerts = new ConcurrentHashMap<>();
        
        // 检查慢查询告警
        double slowQueryRate = getSlowQueryRate();
        if (slowQueryRate > 0.1) { // 超过10%
            alerts.put("slowQueryAlert", Map.of(
                "level", "warning",
                "message", "慢查询比例过高: " + String.format("%.2f%%", slowQueryRate * 100),
                "threshold", "10%",
                "current", String.format("%.2f%%", slowQueryRate * 100)
            ));
        }
        
        // 检查缓存命中率告警
        double hitRate = getCacheHitRate();
        if (hitRate < 0.8 && (cacheHits.get() + cacheMisses.get()) > 100) { // 命中率低于80%且有足够样本
            alerts.put("cacheHitRateAlert", Map.of(
                "level", "warning",
                "message", "缓存命中率过低: " + String.format("%.2f%%", hitRate * 100),
                "threshold", "80%",
                "current", String.format("%.2f%%", hitRate * 100)
            ));
        }
        
        return alerts;
    }
    
    /**
     * 获取平均查询时间
     */
    private double getAverageQueryTime() {
        if (queryExecutionTimer != null) {
            return queryExecutionTimer.mean(TimeUnit.MILLISECONDS);
        }
        return 0.0;
    }
    
    /**
     * 获取性能统计摘要
     */
    public Map<String, Object> getPerformanceSummary() {
        Map<String, Object> summary = new ConcurrentHashMap<>();
        
        summary.put("totalQueries", totalQueries.get());
        summary.put("slowQueries", slowQueries.get());
        summary.put("cacheHits", cacheHits.get());
        summary.put("cacheMisses", cacheMisses.get());
        summary.put("cacheHitRate", getCacheHitRate());
        summary.put("slowQueryRate", getSlowQueryRate());
        summary.put("datasourceStats", datasourceStats);
        
        return summary;
    }
    
    /**
     * 重置统计数据
     */
    public void resetStats() {
        totalQueries.set(0);
        slowQueries.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        datasourceStats.clear();
        
        log.info("性能统计数据已重置");
    }
    
    /**
     * 更新数据源统计
     */
    private void updateDatasourceStats(String datasource, String operation) {
        String key = datasource + "." + operation;
        datasourceStats.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 截断SQL用于日志记录
     */
    private String truncateSql(String sql) {
        if (sql == null) return "null";
        return sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
    }
}