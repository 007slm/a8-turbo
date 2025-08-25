package org.openjdbcproxy.grpc.server.smartcache.metrics;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.smartcache.interceptor.SmartCacheMetrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements metrics collection for smart cache operations.
 * This provides comprehensive metrics that can be exported to monitoring systems.
 */
@Slf4j
public class SmartCacheMetricsCollector implements SmartCacheMetrics {
    
    // Cache operation counters
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong cacheMissCount = new AtomicLong(0);
    private final AtomicLong cacheSkipCount = new AtomicLong(0);
    private final AtomicLong cacheStoreCount = new AtomicLong(0);
    private final AtomicLong cacheErrorCount = new AtomicLong(0);
    
    // Cache skip reasons
    private final ConcurrentMap<String, AtomicLong> skipReasons = new ConcurrentHashMap<>();
    
    // Transaction counters
    private final AtomicLong transactionStartCount = new AtomicLong(0);
    private final AtomicLong transactionCommitCount = new AtomicLong(0);
    private final AtomicLong transactionRollbackCount = new AtomicLong(0);
    private final AtomicLong writeOperationCount = new AtomicLong(0);
    
    // Timing metrics
    private final AtomicLong totalInterceptionTime = new AtomicLong(0);
    private final AtomicLong interceptionCount = new AtomicLong(0);
    
    // Performance metrics
    private final AtomicLong maxInterceptionTime = new AtomicLong(0);
    private final AtomicLong minInterceptionTime = new AtomicLong(Long.MAX_VALUE);
    
    @Override
    public void recordCacheHit() {
        cacheHitCount.incrementAndGet();
        log.debug("Cache hit recorded, total: {}", cacheHitCount.get());
    }
    
    @Override
    public void recordCacheMiss() {
        cacheMissCount.incrementAndGet();
        log.debug("Cache miss recorded, total: {}", cacheMissCount.get());
    }
    
    @Override
    public void recordCacheSkip(String reason) {
        cacheSkipCount.incrementAndGet();
        skipReasons.computeIfAbsent(reason, k -> new AtomicLong(0)).incrementAndGet();
        log.debug("Cache skip recorded for reason '{}', total skips: {}", reason, cacheSkipCount.get());
    }
    
    @Override
    public void recordCacheStore() {
        cacheStoreCount.incrementAndGet();
        log.debug("Cache store recorded, total: {}", cacheStoreCount.get());
    }
    
    @Override
    public void recordCacheError() {
        cacheErrorCount.incrementAndGet();
        log.warn("Cache error recorded, total: {}", cacheErrorCount.get());
    }
    
    @Override
    public void recordInterceptionTime(long milliseconds) {
        totalInterceptionTime.addAndGet(milliseconds);
        interceptionCount.incrementAndGet();
        
        // Update min/max
        updateMinTime(milliseconds);
        updateMaxTime(milliseconds);
        
        log.debug("Interception time recorded: {}ms, avg: {}ms", 
                 milliseconds, getAverageInterceptionTime());
    }
    
    @Override
    public void recordTransactionStart() {
        transactionStartCount.incrementAndGet();
        log.debug("Transaction start recorded, total: {}", transactionStartCount.get());
    }
    
    @Override
    public void recordTransactionCommit() {
        transactionCommitCount.incrementAndGet();
        log.debug("Transaction commit recorded, total: {}", transactionCommitCount.get());
    }
    
    @Override
    public void recordTransactionRollback() {
        transactionRollbackCount.incrementAndGet();
        log.debug("Transaction rollback recorded, total: {}", transactionRollbackCount.get());
    }
    
    @Override
    public void recordWriteOperation() {
        writeOperationCount.incrementAndGet();
        log.debug("Write operation recorded, total: {}", writeOperationCount.get());
    }
    
    /**
     * Gets cache hit ratio (0.0 to 1.0)
     */
    public double getCacheHitRatio() {
        long totalCacheAttempts = cacheHitCount.get() + cacheMissCount.get();
        if (totalCacheAttempts == 0) return 0.0;
        return (double) cacheHitCount.get() / totalCacheAttempts;
    }
    
    /**
     * Gets cache utilization ratio (cache attempts vs total queries)
     */
    public double getCacheUtilizationRatio() {
        long totalQueries = cacheHitCount.get() + cacheMissCount.get() + cacheSkipCount.get();
        if (totalQueries == 0) return 0.0;
        long cacheAttempts = cacheHitCount.get() + cacheMissCount.get();
        return (double) cacheAttempts / totalQueries;
    }
    
    /**
     * Gets average interception time in milliseconds
     */
    public double getAverageInterceptionTime() {
        long count = interceptionCount.get();
        if (count == 0) return 0.0;
        return (double) totalInterceptionTime.get() / count;
    }
    
    /**
     * Gets comprehensive metrics snapshot
     */
    public SmartCacheMetricsSnapshot getSnapshot() {
        return SmartCacheMetricsSnapshot.builder()
                .cacheHitCount(cacheHitCount.get())
                .cacheMissCount(cacheMissCount.get())
                .cacheSkipCount(cacheSkipCount.get())
                .cacheStoreCount(cacheStoreCount.get())
                .cacheErrorCount(cacheErrorCount.get())
                .transactionStartCount(transactionStartCount.get())
                .transactionCommitCount(transactionCommitCount.get())
                .transactionRollbackCount(transactionRollbackCount.get())
                .writeOperationCount(writeOperationCount.get())
                .totalInterceptionTime(totalInterceptionTime.get())
                .interceptionCount(interceptionCount.get())
                .maxInterceptionTime(maxInterceptionTime.get())
                .minInterceptionTime(minInterceptionTime.get() == Long.MAX_VALUE ? 0 : minInterceptionTime.get())
                .cacheHitRatio(getCacheHitRatio())
                .cacheUtilizationRatio(getCacheUtilizationRatio())
                .averageInterceptionTime(getAverageInterceptionTime())
                .skipReasons(new ConcurrentHashMap<>(skipReasons))
                .build();
    }
    
    /**
     * Gets skip reason breakdown
     */
    public ConcurrentMap<String, AtomicLong> getSkipReasons() {
        return skipReasons;
    }
    
    /**
     * Resets all metrics (for testing or administrative purposes)
     */
    public void reset() {
        cacheHitCount.set(0);
        cacheMissCount.set(0);
        cacheSkipCount.set(0);
        cacheStoreCount.set(0);
        cacheErrorCount.set(0);
        transactionStartCount.set(0);
        transactionCommitCount.set(0);
        transactionRollbackCount.set(0);
        writeOperationCount.set(0);
        totalInterceptionTime.set(0);
        interceptionCount.set(0);
        maxInterceptionTime.set(0);
        minInterceptionTime.set(Long.MAX_VALUE);
        skipReasons.clear();
        
        log.info("Smart cache metrics reset");
    }
    
    /**
     * Updates minimum interception time
     */
    private void updateMinTime(long milliseconds) {
        long currentMin = minInterceptionTime.get();
        while (milliseconds < currentMin) {
            if (minInterceptionTime.compareAndSet(currentMin, milliseconds)) {
                break;
            }
            currentMin = minInterceptionTime.get();
        }
    }
    
    /**
     * Updates maximum interception time
     */
    private void updateMaxTime(long milliseconds) {
        long currentMax = maxInterceptionTime.get();
        while (milliseconds > currentMax) {
            if (maxInterceptionTime.compareAndSet(currentMax, milliseconds)) {
                break;
            }
            currentMax = maxInterceptionTime.get();
        }
    }
    
    /**
     * Gets metrics in Prometheus format for easy export
     */
    public String getPrometheusMetrics() {
        StringBuilder sb = new StringBuilder();
        SmartCacheMetricsSnapshot snapshot = getSnapshot();
        
        // Basic counters
        sb.append("# HELP ojp_cache_hits_total Total number of cache hits\n");
        sb.append("# TYPE ojp_cache_hits_total counter\n");
        sb.append("ojp_cache_hits_total ").append(snapshot.getCacheHitCount()).append("\n");
        
        sb.append("# HELP ojp_cache_misses_total Total number of cache misses\n");
        sb.append("# TYPE ojp_cache_misses_total counter\n");
        sb.append("ojp_cache_misses_total ").append(snapshot.getCacheMissCount()).append("\n");
        
        sb.append("# HELP ojp_cache_skips_total Total number of cache skips\n");
        sb.append("# TYPE ojp_cache_skips_total counter\n");
        sb.append("ojp_cache_skips_total ").append(snapshot.getCacheSkipCount()).append("\n");
        
        // Ratios
        sb.append("# HELP ojp_cache_hit_ratio Cache hit ratio (0.0 to 1.0)\n");
        sb.append("# TYPE ojp_cache_hit_ratio gauge\n");
        sb.append("ojp_cache_hit_ratio ").append(snapshot.getCacheHitRatio()).append("\n");
        
        // Timing
        sb.append("# HELP ojp_cache_interception_time_avg_ms Average interception time in milliseconds\n");
        sb.append("# TYPE ojp_cache_interception_time_avg_ms gauge\n");
        sb.append("ojp_cache_interception_time_avg_ms ").append(snapshot.getAverageInterceptionTime()).append("\n");
        
        return sb.toString();
    }
}