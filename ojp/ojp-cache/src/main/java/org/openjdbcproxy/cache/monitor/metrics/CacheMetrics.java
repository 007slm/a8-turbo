package org.openjdbcproxy.cache.monitor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存决策监控指标
 * 集成 Micrometer 暴露 Prometheus 指标
 */
@Slf4j
@Component
public class CacheMetrics {

    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer decisionTimer;
    private final Timer cachedQueryTimer;
    private final Timer originalQueryTimer;
    private final Counter streamEventsCounter;
    private final Counter apiCallsCounter;

    public CacheMetrics(MeterRegistry meterRegistry) {
        // 缓存决策计数
        this.cacheHitCounter = Counter.builder("ojp.cache.decision")
                .tag("result", "hit")
                .description("Cache decision hit count")
                .register(meterRegistry);
        
        this.cacheMissCounter = Counter.builder("ojp.cache.decision")
                .tag("result", "miss")
                .description("Cache decision miss count")
                .register(meterRegistry);
        
        // 决策延迟
        this.decisionTimer = Timer.builder("ojp.cache.decision.latency")
                .description("Cache decision latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        // 缓存查询延迟 (命中时)
        this.cachedQueryTimer = Timer.builder("ojp.cache.query.latency")
                .tag("type", "cached")
                .description("Cached query latency (StarRocks)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        // 原始查询延迟 (回源时)
        this.originalQueryTimer = Timer.builder("ojp.cache.query.latency")
                .tag("type", "original")
                .description("Original query latency (Source DB)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        // Stream 事件处理计数
        this.streamEventsCounter = Counter.builder("ojp.seatunnel.stream.events")
                .description("SeaTunnel stream events processed")
                .register(meterRegistry);
        
        // REST API 调用计数
        this.apiCallsCounter = Counter.builder("ojp.seatunnel.api.calls")
                .description("SeaTunnel REST API calls")
                .register(meterRegistry);
        
        log.info("CacheMetrics initialized with Micrometer");
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        cacheHitCounter.increment();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        cacheMissCounter.increment();
    }

    /**
     * 记录决策延迟
     */
    public void recordDecisionLatency(long durationMs) {
        decisionTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录缓存查询延迟
     */
    public void recordCachedQueryLatency(long durationMs) {
        cachedQueryTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录原始查询延迟
     */
    public void recordOriginalQueryLatency(long durationMs) {
        originalQueryTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录 Stream 事件处理
     */
    public void recordStreamEvent() {
        streamEventsCounter.increment();
    }

    /**
     * 记录 REST API 调用
     */
    public void recordApiCall() {
        apiCallsCounter.increment();
    }

    /**
     * 获取命中率
     */
    public double getHitRate() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        return total > 0 ? hits / total : 0.0;
    }
}
