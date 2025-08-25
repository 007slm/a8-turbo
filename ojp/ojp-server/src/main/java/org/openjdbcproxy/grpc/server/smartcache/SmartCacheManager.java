package org.openjdbcproxy.grpc.server.smartcache;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.smartcache.cache.StarRocksCacheManager;
import org.openjdbcproxy.grpc.server.smartcache.config.CacheRuleConfigEntry;
import org.openjdbcproxy.grpc.server.smartcache.config.SmartCacheConfig;
import org.openjdbcproxy.grpc.server.smartcache.interceptor.SmartCacheMetrics;
import org.openjdbcproxy.grpc.server.smartcache.interceptor.SmartQueryInterceptor;
import org.openjdbcproxy.grpc.server.smartcache.key.CacheKeyBuilder;
import org.openjdbcproxy.grpc.server.smartcache.metrics.SmartCacheMetricsCollector;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRule;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRuleEngine;
import org.openjdbcproxy.grpc.server.smartcache.transaction.TransactionStateTracker;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Main manager for smart cache functionality.
 * This class orchestrates all cache components and provides the main API.
 */
@Slf4j
public class SmartCacheManager implements AutoCloseable {
    
    private final SmartCacheConfig config;
    private final CacheRuleEngine ruleEngine;
    private final StarRocksCacheManager cacheManager;
    private final TransactionStateTracker transactionTracker;
    private final SmartCacheMetrics metrics;
    private final SmartQueryInterceptor interceptor;
    private final ScheduledExecutorService cleanupExecutor;
    
    private volatile boolean initialized = false;
    private volatile boolean closed = false;
    
    public SmartCacheManager(SmartCacheConfig config) {
        this.config = config;
        
        // Validate configuration
        config.validate();
        
        // Initialize components
        this.ruleEngine = new CacheRuleEngine();
        this.transactionTracker = new TransactionStateTracker();
        
        // Initialize metrics
        this.metrics = config.isMetricsEnabled() ? 
                new SmartCacheMetricsCollector() : SmartCacheMetrics.NOOP;
        
        // Initialize cache manager if enabled
        if (config.isEnabled()) {
            this.cacheManager = new StarRocksCacheManager(config.getStarRocksConfig());
            this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "SmartCache-Cleanup");
                t.setDaemon(true);
                return t;
            });
        } else {
            this.cacheManager = null;
            this.cleanupExecutor = null;
        }
        
        // Initialize interceptor
        this.interceptor = new SmartQueryInterceptor(
                ruleEngine, cacheManager, transactionTracker, metrics);
        
        log.info("Smart cache manager created, enabled: {}", config.isEnabled());
    }
    
    /**
     * Initializes the cache manager and all its components
     */
    public void initialize() throws SQLException {
        if (initialized) {
            return;
        }
        
        if (config.isEnabled()) {
            // Initialize cache database
            cacheManager.initialize();
            
            // Load cache rules
            loadCacheRules();
            
            // Schedule cleanup task
            scheduleCleanupTask();
            
            log.info("Smart cache manager initialized successfully");
        } else {
            log.info("Smart cache is disabled, skipping initialization");
        }
        
        initialized = true;
    }
    
    /**
     * Gets the query interceptor
     */
    public SmartQueryInterceptor getInterceptor() {
        return interceptor;
    }
    
    /**
     * Gets the metrics collector
     */
    public SmartCacheMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Gets the rule engine
     */
    public CacheRuleEngine getRuleEngine() {
        return ruleEngine;
    }
    
    /**
     * Gets the cache manager
     */
    public StarRocksCacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * Gets the transaction tracker
     */
    public TransactionStateTracker getTransactionTracker() {
        return transactionTracker;
    }
    
    /**
     * Checks if smart cache is enabled and initialized
     */
    public boolean isEnabled() {
        return config.isEnabled() && initialized;
    }
    
    /**
     * Updates cache rules from configuration
     */
    public void updateCacheRules(List<CacheRuleConfigEntry> ruleConfigs) {
        List<CacheRule> rules = ruleConfigs.stream()
                .filter(CacheRuleConfigEntry::isEnabled)
                .map(CacheRuleConfigEntry::toRuntimeRule)
                .filter(rule -> rule != null)
                .collect(Collectors.toList());
        
        ruleEngine.setRules(rules);
        log.info("Updated cache rules, active rules: {}", rules.size());
    }
    
    /**
     * Loads cache rules from configuration
     */
    private void loadCacheRules() {
        updateCacheRules(config.getCacheRules());
    }
    
    /**
     * Schedules periodic cleanup task
     */
    private void scheduleCleanupTask() {
        if (cleanupExecutor != null && config.getCleanupInterval() != null) {
            long intervalMinutes = config.getCleanupInterval().toMinutes();
            
            cleanupExecutor.scheduleAtFixedRate(
                    this::performCleanup,
                    intervalMinutes,
                    intervalMinutes,
                    TimeUnit.MINUTES
            );
            
            log.info("Scheduled cache cleanup task every {} minutes", intervalMinutes);
        }
    }
    
    /**
     * Performs cache cleanup
     */
    private void performCleanup() {
        try {
            if (cacheManager != null) {
                int deletedEntries = cacheManager.cleanupExpiredEntries();
                log.debug("Cache cleanup completed, deleted {} expired entries", deletedEntries);
            }
        } catch (Exception e) {
            log.warn("Cache cleanup failed: {}", e.getMessage());
        }
    }
    
    /**
     * Gets cache health status
     */
    public CacheHealthStatus getHealthStatus() {
        if (!config.isEnabled()) {
            return CacheHealthStatus.builder()
                    .healthy(true)
                    .enabled(false)
                    .message("Smart cache is disabled")
                    .build();
        }
        
        try {
            boolean cacheHealthy = cacheManager.isHealthy();
            String message = cacheHealthy ? "All systems operational" : "Cache storage unavailable";
            
            return CacheHealthStatus.builder()
                    .healthy(cacheHealthy)
                    .enabled(true)
                    .initialized(initialized)
                    .ruleCount(ruleEngine.getRuleCount())
                    .activeTransactions(transactionTracker.getActiveTransactionCount())
                    .message(message)
                    .build();
                    
        } catch (Exception e) {
            return CacheHealthStatus.builder()
                    .healthy(false)
                    .enabled(true)
                    .initialized(initialized)
                    .message("Health check failed: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        log.info("Closing smart cache manager...");
        
        try {
            // Shutdown cleanup executor
            if (cleanupExecutor != null) {
                cleanupExecutor.shutdown();
                try {
                    if (!cleanupExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        cleanupExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    cleanupExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // Close cache manager
            if (cacheManager != null) {
                cacheManager.close();
            }
            
            // Clear transaction tracker
            transactionTracker.clear();
            
            // Clear rules
            ruleEngine.clearRules();
            
            closed = true;
            log.info("Smart cache manager closed successfully");
            
        } catch (Exception e) {
            log.error("Error closing smart cache manager", e);
        }
    }
    
    /**
     * Health status information
     */
    @lombok.Data
    @lombok.Builder
    public static class CacheHealthStatus {
        private boolean healthy;
        private boolean enabled;
        private boolean initialized;
        private int ruleCount;
        private long activeTransactions;
        private String message;
    }
}