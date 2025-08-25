package org.openjdbcproxy.grpc.server.smartcache.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable snapshot of smart cache metrics at a point in time
 */
@Data
@Builder
public class SmartCacheMetricsSnapshot {
    
    // Cache operation counts
    private final long cacheHitCount;
    private final long cacheMissCount;
    private final long cacheSkipCount;
    private final long cacheStoreCount;
    private final long cacheErrorCount;
    
    // Transaction counts
    private final long transactionStartCount;
    private final long transactionCommitCount;
    private final long transactionRollbackCount;
    private final long writeOperationCount;
    
    // Timing metrics
    private final long totalInterceptionTime;
    private final long interceptionCount;
    private final long maxInterceptionTime;
    private final long minInterceptionTime;
    
    // Calculated metrics
    private final double cacheHitRatio;
    private final double cacheUtilizationRatio;
    private final double averageInterceptionTime;
    
    // Skip reason breakdown
    private final ConcurrentMap<String, AtomicLong> skipReasons;
    
    /**
     * Gets total cache attempts (hits + misses)
     */
    public long getTotalCacheAttempts() {
        return cacheHitCount + cacheMissCount;
    }
    
    /**
     * Gets total queries processed
     */
    public long getTotalQueries() {
        return cacheHitCount + cacheMissCount + cacheSkipCount;
    }
    
    /**
     * Gets cache miss ratio
     */
    public double getCacheMissRatio() {
        return 1.0 - cacheHitRatio;
    }
    
    /**
     * Gets error ratio
     */
    public double getErrorRatio() {
        long totalOperations = getTotalQueries() + cacheStoreCount;
        if (totalOperations == 0) return 0.0;
        return (double) cacheErrorCount / totalOperations;
    }
}

/**
 * Exports metrics to various monitoring systems
 */
class MetricsExporter {
    
    /**
     * Exports metrics in JSON format
     */
    public static String toJson(SmartCacheMetricsSnapshot snapshot) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"cacheMetrics\": {\n");
        json.append("    \"hitCount\": ").append(snapshot.getCacheHitCount()).append(",\n");
        json.append("    \"missCount\": ").append(snapshot.getCacheMissCount()).append(",\n");
        json.append("    \"skipCount\": ").append(snapshot.getCacheSkipCount()).append(",\n");
        json.append("    \"storeCount\": ").append(snapshot.getCacheStoreCount()).append(",\n");
        json.append("    \"errorCount\": ").append(snapshot.getCacheErrorCount()).append(",\n");
        json.append("    \"hitRatio\": ").append(snapshot.getCacheHitRatio()).append(",\n");
        json.append("    \"utilizationRatio\": ").append(snapshot.getCacheUtilizationRatio()).append("\n");
        json.append("  },\n");
        json.append("  \"transactionMetrics\": {\n");
        json.append("    \"startCount\": ").append(snapshot.getTransactionStartCount()).append(",\n");
        json.append("    \"commitCount\": ").append(snapshot.getTransactionCommitCount()).append(",\n");
        json.append("    \"rollbackCount\": ").append(snapshot.getTransactionRollbackCount()).append(",\n");
        json.append("    \"writeCount\": ").append(snapshot.getWriteOperationCount()).append("\n");
        json.append("  },\n");
        json.append("  \"performanceMetrics\": {\n");
        json.append("    \"avgInterceptionTime\": ").append(snapshot.getAverageInterceptionTime()).append(",\n");
        json.append("    \"maxInterceptionTime\": ").append(snapshot.getMaxInterceptionTime()).append(",\n");
        json.append("    \"minInterceptionTime\": ").append(snapshot.getMinInterceptionTime()).append(",\n");
        json.append("    \"totalInterceptions\": ").append(snapshot.getInterceptionCount()).append("\n");
        json.append("  }\n");
        json.append("}\n");
        return json.toString();
    }
    
    /**
     * Exports metrics in InfluxDB line protocol format
     */
    public static String toInfluxDB(SmartCacheMetricsSnapshot snapshot, String measurement) {
        long timestamp = System.currentTimeMillis() * 1000000; // Convert to nanoseconds
        StringBuilder influx = new StringBuilder();
        
        // Cache metrics
        influx.append(measurement).append(",type=cache ")
              .append("hit_count=").append(snapshot.getCacheHitCount()).append(",")
              .append("miss_count=").append(snapshot.getCacheMissCount()).append(",")
              .append("skip_count=").append(snapshot.getCacheSkipCount()).append(",")
              .append("store_count=").append(snapshot.getCacheStoreCount()).append(",")
              .append("error_count=").append(snapshot.getCacheErrorCount()).append(",")
              .append("hit_ratio=").append(snapshot.getCacheHitRatio()).append(",")
              .append("utilization_ratio=").append(snapshot.getCacheUtilizationRatio())
              .append(" ").append(timestamp).append("\n");
        
        // Transaction metrics
        influx.append(measurement).append(",type=transaction ")
              .append("start_count=").append(snapshot.getTransactionStartCount()).append(",")
              .append("commit_count=").append(snapshot.getTransactionCommitCount()).append(",")
              .append("rollback_count=").append(snapshot.getTransactionRollbackCount()).append(",")
              .append("write_count=").append(snapshot.getWriteOperationCount())
              .append(" ").append(timestamp).append("\n");
        
        // Performance metrics
        influx.append(measurement).append(",type=performance ")
              .append("avg_interception_time=").append(snapshot.getAverageInterceptionTime()).append(",")
              .append("max_interception_time=").append(snapshot.getMaxInterceptionTime()).append(",")
              .append("min_interception_time=").append(snapshot.getMinInterceptionTime()).append(",")
              .append("total_interceptions=").append(snapshot.getInterceptionCount())
              .append(" ").append(timestamp).append("\n");
        
        return influx.toString();
    }
    
    /**
     * Exports metrics in CSV format for reports
     */
    public static String toCsv(SmartCacheMetricsSnapshot snapshot) {
        StringBuilder csv = new StringBuilder();
        csv.append("metric,value\n");
        csv.append("cache_hit_count,").append(snapshot.getCacheHitCount()).append("\n");
        csv.append("cache_miss_count,").append(snapshot.getCacheMissCount()).append("\n");
        csv.append("cache_skip_count,").append(snapshot.getCacheSkipCount()).append("\n");
        csv.append("cache_store_count,").append(snapshot.getCacheStoreCount()).append("\n");
        csv.append("cache_error_count,").append(snapshot.getCacheErrorCount()).append("\n");
        csv.append("cache_hit_ratio,").append(snapshot.getCacheHitRatio()).append("\n");
        csv.append("cache_utilization_ratio,").append(snapshot.getCacheUtilizationRatio()).append("\n");
        csv.append("transaction_start_count,").append(snapshot.getTransactionStartCount()).append("\n");
        csv.append("transaction_commit_count,").append(snapshot.getTransactionCommitCount()).append("\n");
        csv.append("transaction_rollback_count,").append(snapshot.getTransactionRollbackCount()).append("\n");
        csv.append("write_operation_count,").append(snapshot.getWriteOperationCount()).append("\n");
        csv.append("avg_interception_time,").append(snapshot.getAverageInterceptionTime()).append("\n");
        csv.append("max_interception_time,").append(snapshot.getMaxInterceptionTime()).append("\n");
        csv.append("min_interception_time,").append(snapshot.getMinInterceptionTime()).append("\n");
        csv.append("total_interceptions,").append(snapshot.getInterceptionCount()).append("\n");
        return csv.toString();
    }
}