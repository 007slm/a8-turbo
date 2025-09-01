package org.openjdbcproxy.grpc.server.interceptor.impl.cache.decision;

import java.time.Duration;

/**
 * 缓存决策结果
 * 表示缓存规则引擎的决策结果
 */
public class CacheDecision {
    private final boolean hit;
    private final boolean miss;
    private final boolean skip;
    private final String reason;
    private final String cacheKey;
    private final Duration ttl;
    private final String ruleName;
    
    private CacheDecision(boolean hit, boolean miss, boolean skip, String reason, 
                        String cacheKey, Duration ttl, String ruleName) {
        this.hit = hit;
        this.miss = miss;
        this.skip = skip;
        this.reason = reason;
        this.cacheKey = cacheKey;
        this.ttl = ttl;
        this.ruleName = ruleName;
    }
    
    public static CacheDecision hit(String cacheKey, Duration ttl, String ruleName) {
        return new CacheDecision(true, false, false, null, cacheKey, ttl, ruleName);
    }
    
    public static CacheDecision miss(String cacheKey, Duration ttl, String ruleName) {
        return new CacheDecision(false, true, false, null, cacheKey, ttl, ruleName);
    }
    
    public static CacheDecision skip(String reason) {
        return new CacheDecision(false, false, true, reason, null, null, null);
    }
    
    public boolean isHit() { return hit; }
    public boolean isMiss() { return miss; }
    public boolean isSkip() { return skip; }
    public String getReason() { return reason; }
    public String getCacheKey() { return cacheKey; }
    public Duration getTtl() { return ttl; }
    public String getRuleName() { return ruleName; }
}
