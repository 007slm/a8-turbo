package org.openjdbcproxy.grpc.server.smartcache.interceptor;

/**
 * Metrics interface for smart cache operations
 */
public interface SmartCacheMetrics {
    
    void recordCacheHit();
    void recordCacheMiss();
    void recordCacheSkip(String reason);
    void recordCacheStore();
    void recordCacheError();
    void recordInterceptionTime(long milliseconds);
    void recordTransactionStart();
    void recordTransactionCommit();
    void recordTransactionRollback();
    void recordWriteOperation();
    
    /**
     * No-op implementation for when metrics are disabled
     */
    SmartCacheMetrics NOOP = new SmartCacheMetrics() {
        @Override public void recordCacheHit() {}
        @Override public void recordCacheMiss() {}
        @Override public void recordCacheSkip(String reason) {}
        @Override public void recordCacheStore() {}
        @Override public void recordCacheError() {}
        @Override public void recordInterceptionTime(long milliseconds) {}
        @Override public void recordTransactionStart() {}
        @Override public void recordTransactionCommit() {}
        @Override public void recordTransactionRollback() {}
        @Override public void recordWriteOperation() {}
    };
}