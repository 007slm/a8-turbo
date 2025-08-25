package org.openjdbcproxy.grpc.server.smartcache.interceptor;

import lombok.Data;
import org.openjdbcproxy.grpc.server.smartcache.cache.CacheEntry;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRule;

/**
 * Represents the decision made by the cache interceptor
 */
@Data
public class CacheDecision {
    
    public enum Type {
        HIT,    // Cache hit - use cached result
        MISS,   // Cache miss - execute query and cache result
        SKIP    // Skip caching - execute query without caching
    }
    
    private final Type type;
    private final String reason;
    private final CacheEntry cacheEntry;
    private final String cacheKey;
    private final CacheRule.CacheAction action;
    
    private CacheDecision(Type type, String reason, CacheEntry cacheEntry, 
                         String cacheKey, CacheRule.CacheAction action) {
        this.type = type;
        this.reason = reason;
        this.cacheEntry = cacheEntry;
        this.cacheKey = cacheKey;
        this.action = action;
    }
    
    /**
     * Creates a cache hit decision
     */
    public static CacheDecision hit(CacheEntry cacheEntry, String cacheKey) {
        return new CacheDecision(Type.HIT, "Cache hit", cacheEntry, cacheKey, null);
    }
    
    /**
     * Creates a cache miss decision
     */
    public static CacheDecision miss(String cacheKey, CacheRule.CacheAction action) {
        return new CacheDecision(Type.MISS, "Cache miss", null, cacheKey, action);
    }
    
    /**
     * Creates a skip decision
     */
    public static CacheDecision skip(String reason) {
        return new CacheDecision(Type.SKIP, reason, null, null, null);
    }
    
    /**
     * Checks if this is a cache hit
     */
    public boolean isHit() {
        return type == Type.HIT;
    }
    
    /**
     * Checks if this is a cache miss
     */
    public boolean isMiss() {
        return type == Type.MISS;
    }
    
    /**
     * Checks if caching should be skipped
     */
    public boolean isSkip() {
        return type == Type.SKIP;
    }
}