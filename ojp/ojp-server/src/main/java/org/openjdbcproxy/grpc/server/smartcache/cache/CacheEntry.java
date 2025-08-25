package org.openjdbcproxy.grpc.server.smartcache.cache;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a cache entry retrieved from StarRocks
 */
@Data
@Builder
public class CacheEntry {
    
    /**
     * The cached result data (serialized)
     */
    private String resultData;
    
    /**
     * Additional metadata about the cache entry
     */
    private String metadata;
    
    /**
     * When the cache entry was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When the cache entry expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Checks if the cache entry is still valid
     */
    public boolean isValid() {
        return expiresAt != null && LocalDateTime.now().isBefore(expiresAt);
    }
}

/**
 * Cache statistics for monitoring and metrics
 */
@Data
@Builder
class CacheStats {
    
    /**
     * Total number of cache entries
     */
    private long totalEntries;
    
    /**
     * Number of active (non-expired) cache entries
     */
    private long activeEntries;
    
    /**
     * Number of expired cache entries
     */
    private long expiredEntries;
    
    /**
     * Total number of cache hits across all entries
     */
    private long totalHits;
    
    /**
     * Average hits per cache entry
     */
    private double avgHitsPerEntry;
    
    /**
     * Cache hit ratio (calculated field)
     */
    public double getHitRatio() {
        if (totalEntries == 0) return 0.0;
        return (double) totalHits / totalEntries;
    }
    
    /**
     * Active entry ratio (calculated field)
     */
    public double getActiveRatio() {
        if (totalEntries == 0) return 0.0;
        return (double) activeEntries / totalEntries;
    }
}