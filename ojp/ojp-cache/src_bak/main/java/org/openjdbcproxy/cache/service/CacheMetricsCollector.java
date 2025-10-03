package org.openjdbcproxy.cache.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存指标收集器
 * 负责收集和记录缓存相关的指标数据
 */
@Slf4j
@Component
public class CacheMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheSkipCounter;
    private final Counter queryExecutionCounter;
    private final Counter queryErrorCounter;
    private final Timer cacheProcessingTimer;
    private final Timer cacheStoreTimer;
    private final Timer cacheRetrieveTimer;

    @Autowired
    public CacheMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.cacheHitCounter = Counter.builder("ojp.cache.hit")
            .description("Cache hit count")
            .register(meterRegistry);
            
        this.cacheMissCounter = Counter.builder("ojp.cache.miss")
            .description("Cache miss count")
            .register(meterRegistry);
            
        this.cacheSkipCounter = Counter.builder("ojp.cache.skip")
            .description("Cache skip count")
            .register(meterRegistry);
            
        this.queryExecutionCounter = Counter.builder("ojp.query.execution")
            .description("Query execution count")
            .register(meterRegistry);
            
        this.queryErrorCounter = Counter.builder("ojp.query.error")
            .description("Query error count")
            .register(meterRegistry);
            
        this.cacheProcessingTimer = Timer.builder("ojp.cache.processing.time")
            .description("Cache processing time")
            .register(meterRegistry);
            
        this.cacheStoreTimer = Timer.builder("ojp.cache.store.time")
            .description("Cache store operation time")
            .register(meterRegistry);
            
        this.cacheRetrieveTimer = Timer.builder("ojp.cache.retrieve.time")
            .description("Cache retrieve operation time")
            .register(meterRegistry);
    }
    
    /**
     * 记录缓存命中
     */
    public void recordCacheHit(String sql, String ruleName) {
        cacheHitCounter.increment();
        log.debug("Cache hit recorded - Rule: {}, SQL: {}", ruleName, truncateSql(sql));
    }
    
    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss(String sql, String ruleName) {
        cacheMissCounter.increment();
        log.debug("Cache miss recorded - Rule: {}, SQL: {}", ruleName, truncateSql(sql));
    }
    
    /**
     * 记录缓存跳过
     */
    public void recordCacheSkip(String reason, String sql) {
        cacheSkipCounter.increment();
        log.debug("Cache skip recorded - Reason: {}, SQL: {}", reason, truncateSql(sql));
    }
    
    /**
     * 记录查询执行
     */
    public void recordQueryExecution(String sql, boolean success) {
        if (success) {
            queryExecutionCounter.increment();
            log.debug("Query execution recorded - Success, SQL: {}", truncateSql(sql));
        } else {
            queryErrorCounter.increment();
            log.debug("Query error recorded - SQL: {}", truncateSql(sql));
        }
    }
    
    /**
     * 记录缓存处理时间
     */
    public void recordCacheProcessingTime(long durationMs) {
        cacheProcessingTimer.record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Cache processing time recorded: {}ms", durationMs);
    }
    
    /**
     * 记录缓存存储时间
     */
    public void recordCacheStoreTime(long durationMs) {
        cacheStoreTimer.record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Cache store time recorded: {}ms", durationMs);
    }
    
    /**
     * 记录缓存检索时间
     */
    public void recordCacheRetrieveTime(long durationMs) {
        cacheRetrieveTimer.record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Cache retrieve time recorded: {}ms", durationMs);
    }
    
    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate() {
        double hits = cacheHitCounter.count();
        double total = hits + cacheMissCounter.count();
        return total > 0 ? hits / total : 0.0;
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheMetrics getCacheMetrics() {
        return CacheMetrics.builder()
            .hitCount((long) cacheHitCounter.count())
            .missCount((long) cacheMissCounter.count())
            .skipCount((long) cacheSkipCounter.count())
            .queryExecutionCount((long) queryExecutionCounter.count())
            .queryErrorCount((long) queryErrorCounter.count())
            .hitRate(getCacheHitRate())
            .avgProcessingTime(cacheProcessingTimer.mean(TimeUnit.MILLISECONDS))
            .avgStoreTime(cacheStoreTimer.mean(TimeUnit.MILLISECONDS))
            .avgRetrieveTime(cacheRetrieveTimer.mean(TimeUnit.MILLISECONDS))
            .build();
    }
    
    /**
     * 截断SQL用于日志记录
     */
    private String truncateSql(String sql) {
        if (sql == null) return "null";
        return sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
    }
    
    /**
     * 缓存指标数据类
     */
    public static class CacheMetrics {
        private final long hitCount;
        private final long missCount;
        private final long skipCount;
        private final long queryExecutionCount;
        private final long queryErrorCount;
        private final double hitRate;
        private final double avgProcessingTime;
        private final double avgStoreTime;
        private final double avgRetrieveTime;
        
        private CacheMetrics(Builder builder) {
            this.hitCount = builder.hitCount;
            this.missCount = builder.missCount;
            this.skipCount = builder.skipCount;
            this.queryExecutionCount = builder.queryExecutionCount;
            this.queryErrorCount = builder.queryErrorCount;
            this.hitRate = builder.hitRate;
            this.avgProcessingTime = builder.avgProcessingTime;
            this.avgStoreTime = builder.avgStoreTime;
            this.avgRetrieveTime = builder.avgRetrieveTime;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getSkipCount() { return skipCount; }
        public long getQueryExecutionCount() { return queryExecutionCount; }
        public long getQueryErrorCount() { return queryErrorCount; }
        public double getHitRate() { return hitRate; }
        public double getAvgProcessingTime() { return avgProcessingTime; }
        public double getAvgStoreTime() { return avgStoreTime; }
        public double getAvgRetrieveTime() { return avgRetrieveTime; }
        
        public static class Builder {
            private long hitCount;
            private long missCount;
            private long skipCount;
            private long queryExecutionCount;
            private long queryErrorCount;
            private double hitRate;
            private double avgProcessingTime;
            private double avgStoreTime;
            private double avgRetrieveTime;
            
            public Builder hitCount(long hitCount) { this.hitCount = hitCount; return this; }
            public Builder missCount(long missCount) { this.missCount = missCount; return this; }
            public Builder skipCount(long skipCount) { this.skipCount = skipCount; return this; }
            public Builder queryExecutionCount(long count) { this.queryExecutionCount = count; return this; }
            public Builder queryErrorCount(long count) { this.queryErrorCount = count; return this; }
            public Builder hitRate(double hitRate) { this.hitRate = hitRate; return this; }
            public Builder avgProcessingTime(double time) { this.avgProcessingTime = time; return this; }
            public Builder avgStoreTime(double time) { this.avgStoreTime = time; return this; }
            public Builder avgRetrieveTime(double time) { this.avgRetrieveTime = time; return this; }
            
            public CacheMetrics build() {
                return new CacheMetrics(this);
            }
        }
    }
}