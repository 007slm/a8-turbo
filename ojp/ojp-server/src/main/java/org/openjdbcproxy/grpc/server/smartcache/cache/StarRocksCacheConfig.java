package org.openjdbcproxy.grpc.server.smartcache.cache;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Configuration for StarRocks cache database connection pool.
 * This configuration is independent from the main database connections.
 */
@Data
@Builder
public class StarRocksCacheConfig {
    
    /**
     * StarRocks JDBC URL
     */
    private String jdbcUrl;
    
    /**
     * Username for StarRocks connection
     */
    private String username;
    
    /**
     * Password for StarRocks connection
     */
    private String password;
    
    /**
     * Driver class name (default: com.mysql.cj.jdbc.Driver)
     */
    @Builder.Default
    private String driverClassName = "com.mysql.cj.jdbc.Driver";
    
    /**
     * Maximum pool size
     */
    @Builder.Default
    private int maximumPoolSize = 10;
    
    /**
     * Minimum idle connections
     */
    @Builder.Default
    private int minimumIdle = 2;
    
    /**
     * Connection timeout
     */
    @Builder.Default
    private Duration connectionTimeout = Duration.ofSeconds(30);
    
    /**
     * Idle timeout
     */
    @Builder.Default
    private Duration idleTimeout = Duration.ofMinutes(10);
    
    /**
     * Maximum lifetime
     */
    @Builder.Default
    private Duration maxLifetime = Duration.ofMinutes(30);
    
    /**
     * Validation timeout
     */
    @Builder.Default
    private Duration validationTimeout = Duration.ofSeconds(5);
    
    /**
     * Cache database name
     */
    @Builder.Default
    private String cacheDatabaseName = "smart_cache";
    
    /**
     * Cache table name
     */
    @Builder.Default
    private String cacheTableName = "query_cache";
    
    /**
     * Whether to enable cache compression
     */
    @Builder.Default
    private boolean compressionEnabled = true;
    
    /**
     * Pool name for monitoring
     */
    @Builder.Default
    private String poolName = "StarRocksCachePool";
    
    /**
     * Test query for connection validation
     */
    @Builder.Default
    private String connectionTestQuery = "SELECT 1";
    
    /**
     * Default configuration for development
     */
    public static StarRocksCacheConfig defaultConfig() {
        return StarRocksCacheConfig.builder()
                .jdbcUrl("jdbc:mysql://localhost:9030/smart_cache")
                .username("root")
                .password("")
                .build();
    }
}