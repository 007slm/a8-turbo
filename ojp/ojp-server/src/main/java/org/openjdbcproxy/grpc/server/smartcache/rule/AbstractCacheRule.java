package org.openjdbcproxy.grpc.server.smartcache.rule;

import java.time.Duration;

/**
 * Abstract base class for cache rules providing common functionality.
 * This simplifies rule implementation by providing default behaviors.
 */
public abstract class AbstractCacheRule implements CacheRule {
    
    protected final CacheAction action;
    protected final Control control;
    protected final int priority;
    
    protected AbstractCacheRule(CacheAction action, Control control, int priority) {
        this.action = action;
        this.control = control != null ? control : Control.CONTINUE;
        this.priority = priority;
    }
    
    protected AbstractCacheRule(CacheAction action) {
        this(action, Control.CONTINUE, 0);
    }
    
    @Override
    public CacheAction getAction() {
        return action;
    }
    
    @Override
    public Control getControl() {
        return control;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    /**
     * Simple cache action implementation
     */
    public static class SimpleCacheAction implements CacheAction {
        private final Duration ttl;
        private final boolean enabled;
        private final String keySuffix;
        
        public SimpleCacheAction(Duration ttl, boolean enabled, String keySuffix) {
            this.ttl = ttl;
            this.enabled = enabled;
            this.keySuffix = keySuffix;
        }
        
        public SimpleCacheAction(Duration ttl, boolean enabled) {
            this(ttl, enabled, null);
        }
        
        public SimpleCacheAction(Duration ttl) {
            this(ttl, true, null);
        }
        
        @Override
        public Duration getTtl() {
            return ttl;
        }
        
        @Override
        public boolean isEnabled() {
            return enabled;
        }
        
        @Override
        public String getKeySuffix() {
            return keySuffix;
        }
    }
    
    /**
     * Helper method to create a disabled cache action
     */
    protected static CacheAction disableCache() {
        return new SimpleCacheAction(Duration.ZERO, false);
    }
    
    /**
     * Helper method to create a cache action with TTL
     */
    protected static CacheAction enableCache(Duration ttl) {
        return new SimpleCacheAction(ttl, true);
    }
    
    /**
     * Helper method to create a cache action with TTL and key suffix
     */
    protected static CacheAction enableCache(Duration ttl, String keySuffix) {
        return new SimpleCacheAction(ttl, true, keySuffix);
    }
}