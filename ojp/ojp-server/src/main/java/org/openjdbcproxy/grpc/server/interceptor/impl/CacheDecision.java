package org.openjdbcproxy.grpc.server.interceptor.impl;

import lombok.Data;

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
    private final Object cacheEntry; // 使用Object类型避免依赖CacheEntry类
    private final String cacheKey;
    private final Object action; // 使用Object类型避免依赖CacheRule.CacheAction接口
    
    private CacheDecision(Type type, String reason, Object cacheEntry, 
                         String cacheKey, Object action) {
        this.type = type;
        this.reason = reason;
        this.cacheEntry = cacheEntry;
        this.cacheKey = cacheKey;
        this.action = action;
    }
    
    /**
     * Creates a cache hit decision
     */
    public static CacheDecision hit(Object cacheEntry, String cacheKey) {
        return new CacheDecision(Type.HIT, "Cache hit", cacheEntry, cacheKey, null);
    }
    
    /**
     * Creates a cache miss decision
     */
    public static CacheDecision miss(String cacheKey, Object action) {
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