package org.openjdbcproxy.grpc.server.smartcache.examples;

import org.openjdbcproxy.grpc.server.smartcache.SmartCacheManager;
import org.openjdbcproxy.grpc.server.smartcache.cache.StarRocksCacheConfig;
import org.openjdbcproxy.grpc.server.smartcache.config.CacheRuleConfigEntry;
import org.openjdbcproxy.grpc.server.smartcache.config.SmartCacheConfig;
import org.openjdbcproxy.grpc.server.smartcache.rule.QueryContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/**
 * Smart Cache Configuration Examples
 * 
 * This class demonstrates various ways to configure the smart cache functionality
 * for different use cases and environments.
 */
public class SmartCacheConfigurationExamples {

    /**
     * Example 1: Basic configuration for development environment
     */
    public static SmartCacheManager createDevelopmentConfig() {
        // StarRocks configuration for development
        StarRocksCacheConfig starRocksConfig = StarRocksCacheConfig.builder()
                .jdbcUrl("jdbc:mysql://localhost:9030/smart_cache_dev")
                .username("root")
                .password("password")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .maximumPoolSize(5)
                .minimumIdle(2)
                .cacheDatabaseName("smart_cache_dev")
                .cacheTableName("query_cache")
                .build();

        // Basic cache rules for development
        SmartCacheConfig config = SmartCacheConfig.builder()
                .enabled(true)
                .starRocksConfig(starRocksConfig)
                .defaultCacheTtl(Duration.ofMinutes(5))
                .maxCacheTtl(Duration.ofHours(1))
                .compressionEnabled(true)
                .metricsEnabled(true)
                .transactionAwareEnabled(true)
                .cacheRules(Arrays.asList(
                        // Cache all SELECT queries for 5 minutes
                        CacheRuleConfigEntry.queryTypeRule("dev_select_cache", 
                                QueryContext.QueryType.SELECT, Duration.ofMinutes(5)),
                        
                        // Transaction-aware rule (highest priority)
                        CacheRuleConfigEntry.transactionAwareRule()
                ))
                .build();

        SmartCacheManager cacheManager = new SmartCacheManager(config);
        return cacheManager;
    }

    /**
     * Example 2: Production configuration with comprehensive rules
     */
    public static SmartCacheManager createProductionConfig() {
        // StarRocks configuration for production
        StarRocksCacheConfig starRocksConfig = StarRocksCacheConfig.builder()
                .jdbcUrl("jdbc:mysql://prod-starrocks-cluster:9030/smart_cache_prod")
                .username("cache_user")
                .password("secure_password")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .maximumPoolSize(50)
                .minimumIdle(10)
                .connectionTimeout(Duration.ofSeconds(30))
                .idleTimeout(Duration.ofMinutes(10))
                .maxLifetime(Duration.ofMinutes(30))
                .cacheDatabaseName("smart_cache_prod")
                .cacheTableName("query_cache")
                .compressionEnabled(true)
                .build();

        // Comprehensive cache rules for production
        SmartCacheConfig config = SmartCacheConfig.builder()
                .enabled(true)
                .starRocksConfig(starRocksConfig)
                .defaultCacheTtl(Duration.ofMinutes(15))
                .maxCacheTtl(Duration.ofHours(4))
                .cleanupInterval(Duration.ofMinutes(30))
                .compressionEnabled(true)
                .metricsEnabled(true)
                .transactionAwareEnabled(true)
                .cacheRules(Arrays.asList(
                        // High priority: Transaction safety
                        CacheRuleConfigEntry.transactionAwareRule(),
                        
                        // User data - short cache
                        CacheRuleConfigEntry.tableRule("users_cache", "users", 
                                Duration.ofMinutes(5)),
                        
                        // Product catalog - medium cache
                        CacheRuleConfigEntry.tableRule("products_cache", "products", 
                                Duration.ofMinutes(30)),
                        
                        // Reports and analytics - long cache
                        CacheRuleConfigEntry.regexRule("analytics_cache", 
                                ".*report.*|.*analytics.*|.*dashboard.*", 
                                Duration.ofHours(2)),
                        
                        // Read-only reference data - very long cache
                        CacheRuleConfigEntry.tableRule("reference_cache", "countries", 
                                Duration.ofHours(4)),
                        
                        // General SELECT queries - default cache
                        CacheRuleConfigEntry.queryTypeRule("select_cache", 
                                QueryContext.QueryType.SELECT, Duration.ofMinutes(10)),
                        
                        // Default rule - disable caching for everything else
                        CacheRuleConfigEntry.defaultRule()
                ))
                .build();

        SmartCacheManager cacheManager = new SmartCacheManager(config);
        return cacheManager;
    }

    /**
     * Example 3: Configuration from properties file
     */
    public static SmartCacheManager createFromProperties() {
        Properties props = new Properties();
        
        // Load from properties file or system properties
        props.setProperty("smart.cache.enabled", "true");
        props.setProperty("smart.cache.starrocks.url", "jdbc:mysql://localhost:9030/smart_cache");
        props.setProperty("smart.cache.starrocks.username", "root");
        props.setProperty("smart.cache.starrocks.password", "password");
        props.setProperty("smart.cache.default.ttl", "10m");
        props.setProperty("smart.cache.max.ttl", "2h");
        props.setProperty("smart.cache.compression.enabled", "true");
        props.setProperty("smart.cache.metrics.enabled", "true");
        
        SmartCacheConfig config = SmartCacheConfig.fromProperties(props);
        
        // Add rules programmatically
        config.getCacheRules().addAll(Arrays.asList(
                CacheRuleConfigEntry.transactionAwareRule(),
                CacheRuleConfigEntry.queryTypeRule("select_cache", 
                        QueryContext.QueryType.SELECT, Duration.ofMinutes(10))
        ));
        
        SmartCacheManager cacheManager = new SmartCacheManager(config);
        return cacheManager;
    }

    /**
     * Example 4: High-performance configuration for read-heavy workloads
     */
    public static SmartCacheManager createHighPerformanceConfig() {
        StarRocksCacheConfig starRocksConfig = StarRocksCacheConfig.builder()
                .jdbcUrl("jdbc:mysql://starrocks-cluster:9030/smart_cache")
                .username("cache_user")
                .password("password")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .maximumPoolSize(100)
                .minimumIdle(20)
                .connectionTimeout(Duration.ofSeconds(10))
                .compressionEnabled(false) // Disable compression for speed
                .build();

        SmartCacheConfig config = SmartCacheConfig.builder()
                .enabled(true)
                .starRocksConfig(starRocksConfig)
                .defaultCacheTtl(Duration.ofMinutes(30))
                .maxCacheTtl(Duration.ofHours(6))
                .cleanupInterval(Duration.ofMinutes(15))
                .compressionEnabled(false) // Prioritize speed over storage
                .metricsEnabled(true)
                .transactionAwareEnabled(true)
                .keyConfig(org.openjdbcproxy.grpc.server.smartcache.key.CacheKeyBuilder.CacheKeyConfig.builder()
                        .strategy(org.openjdbcproxy.grpc.server.smartcache.key.CacheKeyBuilder.KeyGenerationStrategy.QUERY_HASH_ONLY)
                        .build())
                .cacheRules(Arrays.asList(
                        CacheRuleConfigEntry.transactionAwareRule(),
                        
                        // Cache everything SELECT for 30 minutes
                        CacheRuleConfigEntry.queryTypeRule("aggressive_select_cache", 
                                QueryContext.QueryType.SELECT, Duration.ofMinutes(30))
                ))
                .build();

        SmartCacheManager cacheManager = new SmartCacheManager(config);
        return cacheManager;
    }

    /**
     * Example 5: Fine-grained configuration with custom rules
     */
    public static SmartCacheManager createFinegrainedConfig() {
        StarRocksCacheConfig starRocksConfig = StarRocksCacheConfig.defaultConfig();

        SmartCacheConfig config = SmartCacheConfig.builder()
                .enabled(true)
                .starRocksConfig(starRocksConfig)
                .defaultCacheTtl(Duration.ofMinutes(10))
                .maxCacheTtl(Duration.ofHours(24))
                .compressionEnabled(true)
                .metricsEnabled(true)
                .transactionAwareEnabled(true)
                .cacheRules(Arrays.asList(
                        // Highest priority: Transaction safety
                        CacheRuleConfigEntry.builder()
                                .name("transaction_safety")
                                .type(CacheRuleConfigEntry.RuleType.TRANSACTION_AWARE)
                                .priority(Integer.MAX_VALUE)
                                .enabled(true)
                                .build(),
                        
                        // High frequency tables - short cache
                        CacheRuleConfigEntry.builder()
                                .name("session_cache")
                                .type(CacheRuleConfigEntry.RuleType.TABLE_NAME)
                                .pattern("user_sessions")
                                .ttl(Duration.ofMinutes(2))
                                .priority(100)
                                .enabled(true)
                                .build(),
                        
                        // Audit tables - no cache
                        CacheRuleConfigEntry.builder()
                                .name("audit_no_cache")
                                .type(CacheRuleConfigEntry.RuleType.REGEX)
                                .pattern(".*audit.*|.*log.*")
                                .ttl(Duration.ZERO)
                                .priority(90)
                                .enabled(false)
                                .build(),
                        
                        // Complex analytics - long cache
                        CacheRuleConfigEntry.builder()
                                .name("complex_analytics")
                                .type(CacheRuleConfigEntry.RuleType.REGEX)
                                .pattern(".*GROUP BY.*|.*HAVING.*|.*window.*")
                                .ttl(Duration.ofHours(1))
                                .priority(80)
                                .enabled(true)
                                .keySuffix("analytics")
                                .build(),
                        
                        // Default SELECT cache
                        CacheRuleConfigEntry.builder()
                                .name("default_select")
                                .type(CacheRuleConfigEntry.RuleType.QUERY_TYPE)
                                .pattern("SELECT")
                                .ttl(Duration.ofMinutes(10))
                                .priority(1)
                                .enabled(true)
                                .build(),
                        
                        // Fallback rule
                        CacheRuleConfigEntry.defaultRule()
                ))
                .build();

        SmartCacheManager cacheManager = new SmartCacheManager(config);
        return cacheManager;
    }

    /**
     * Example 6: Integration with existing OJP Server
     */
    public static void integrateWithOjpServer() {
        // Create smart cache manager
        SmartCacheManager cacheManager = createProductionConfig();
        
        try {
            // Initialize the cache manager
            cacheManager.initialize();
            
            // Create StatementServiceImpl with smart cache support
            // (Assuming you have existing components)
            /*
            SessionManager sessionManager = ... // your existing session manager
            CircuitBreaker circuitBreaker = ... // your existing circuit breaker
            ServerConfiguration serverConfiguration = ... // your existing config
            
            StatementServiceImpl statementService = new StatementServiceImpl(
                sessionManager, 
                circuitBreaker, 
                serverConfiguration, 
                cacheManager  // Add smart cache manager
            );
            */
            
            System.out.println("Smart cache integrated successfully!");
            
            // Monitor cache health
            SmartCacheManager.CacheHealthStatus health = cacheManager.getHealthStatus();
            System.out.println("Cache health: " + health.getMessage());
            
        } catch (Exception e) {
            System.err.println("Failed to initialize smart cache: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources when shutting down
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (cacheManager != null) {
                    cacheManager.close();
                }
            }));
        }
    }

    /**
     * Example 7: Dynamic rule management
     */
    public static void demonstrateDynamicRuleManagement() {
        SmartCacheManager cacheManager = createDevelopmentConfig();
        
        try {
            cacheManager.initialize();
            
            // Get initial rule count
            int initialRules = cacheManager.getRuleEngine().getRuleCount();
            System.out.println("Initial rules: " + initialRules);
            
            // Add new rules dynamically
            CacheRuleConfigEntry newRule = CacheRuleConfigEntry.tableRule(
                "orders_cache", "orders", Duration.ofMinutes(15));
            
            cacheManager.updateCacheRules(Arrays.asList(
                CacheRuleConfigEntry.transactionAwareRule(),
                newRule,
                CacheRuleConfigEntry.queryTypeRule("select_cache", 
                        QueryContext.QueryType.SELECT, Duration.ofMinutes(10))
            ));
            
            int newRuleCount = cacheManager.getRuleEngine().getRuleCount();
            System.out.println("Updated rules: " + newRuleCount);
            
            // Monitor cache metrics
            var metricsSnapshot = ((org.openjdbcproxy.grpc.server.smartcache.metrics.SmartCacheMetricsCollector) 
                    cacheManager.getMetrics()).getSnapshot();
            
            System.out.println("Cache hit ratio: " + metricsSnapshot.getCacheHitRatio());
            System.out.println("Total queries: " + metricsSnapshot.getTotalQueries());
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cacheManager.close();
        }
    }
}