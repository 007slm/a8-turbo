package org.openjdbcproxy.grpc.server.interceptor.impl.cache.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 指标收集器
 * 负责收集和记录缓存相关的指标数据
 */
@Component
public class MetricsCollector {
    
    private MeterRegistry meterRegistry;
    
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheSkipCounter;
    private final Counter queryExecutionCounter;
    private final Counter queryErrorCounter;
    private final Timer cacheProcessingTimer;

    @Autowired
    public MetricsCollector(MeterRegistry meterRegistry) {
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
    }
    
    /**
     * 记录缓存命中
     */
    public void recordCacheHit(String sql, String ruleName) {
        cacheHitCounter.increment();
    }
    
    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss(String sql, String ruleName) {
        cacheMissCounter.increment();
    }
    
    /**
     * 记录缓存跳过
     */
    public void recordCacheSkip(String reason, String sql) {
        cacheSkipCounter.increment();
    }
    
    /**
     * 记录查询执行
     */
    public void recordQueryExecution(String sql, boolean success) {
        if (success) {
            queryExecutionCounter.increment();
        } else {
            queryErrorCounter.increment();
        }
    }
    
    /**
     * 记录缓存处理时间
     */
    public void recordCacheProcessingTime(long durationMs) {
        cacheProcessingTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
