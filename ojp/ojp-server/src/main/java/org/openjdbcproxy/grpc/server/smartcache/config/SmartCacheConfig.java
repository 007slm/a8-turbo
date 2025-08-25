package org.openjdbcproxy.grpc.server.smartcache.config;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.smartcache.cache.StarRocksCacheConfig;
import org.openjdbcproxy.grpc.server.smartcache.key.CacheKeyBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuration management for smart cache functionality.
 * This centralizes all configuration aspects and provides validation.
 */
@Data
@Builder
@Slf4j
public class SmartCacheConfig {
    
    /**
     * Whether smart cache is enabled
     */
    @Builder.Default
    private boolean enabled = false;
    
    /**
     * StarRocks cache database configuration
     */
    private StarRocksCacheConfig starRocksConfig;
    
    /**
     * Cache key generation configuration
     */
    @Builder.Default
    private CacheKeyBuilder.CacheKeyConfig keyConfig = CacheKeyBuilder.CacheKeyConfig.builder().build();
    
    /**
     * Default cache TTL for rules that don't specify one
     */
    @Builder.Default
    private Duration defaultCacheTtl = Duration.ofMinutes(10);
    
    /**
     * Maximum cache TTL allowed
     */
    @Builder.Default
    private Duration maxCacheTtl = Duration.ofHours(24);
    
    /**
     * Whether to enable compression for cached results
     */
    @Builder.Default
    private boolean compressionEnabled = true;
    
    /**
     * Whether to enable metrics collection
     */
    @Builder.Default
    private boolean metricsEnabled = true;
    
    /**
     * Cache cleanup interval (for expired entries)
     */
    @Builder.Default
    private Duration cleanupInterval = Duration.ofHours(1);
    
    /**
     * Maximum number of cache rules allowed
     */
    @Builder.Default
    private int maxCacheRules = 100;
    
    /**
     * Whether to enable transaction awareness
     */
    @Builder.Default
    private boolean transactionAwareEnabled = true;
    
    /**
     * List of cache rule configurations
     */
    @Builder.Default
    private List<CacheRuleConfigEntry> cacheRules = new ArrayList<>();
    
    /**
     * Validates the configuration
     */
    public void validate() throws IllegalArgumentException {
        if (enabled) {
            if (starRocksConfig == null) {
                throw new IllegalArgumentException("StarRocks configuration is required when smart cache is enabled");
            }
            
            if (defaultCacheTtl.isNegative() || defaultCacheTtl.isZero()) {
                throw new IllegalArgumentException("Default cache TTL must be positive");
            }
            
            if (maxCacheTtl.compareTo(defaultCacheTtl) < 0) {
                throw new IllegalArgumentException("Maximum cache TTL must be >= default cache TTL");
            }
            
            if (cleanupInterval.isNegative() || cleanupInterval.isZero()) {
                throw new IllegalArgumentException("Cleanup interval must be positive");
            }
            
            if (maxCacheRules <= 0) {
                throw new IllegalArgumentException("Maximum cache rules must be positive");
            }
            
            if (cacheRules.size() > maxCacheRules) {
                throw new IllegalArgumentException("Number of cache rules exceeds maximum allowed: " + maxCacheRules);
            }
            
            // Validate each rule
            for (int i = 0; i < cacheRules.size(); i++) {
                try {
                    cacheRules.get(i).validate();
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid cache rule at index " + i + ": " + e.getMessage());
                }
            }
            
            log.info("Smart cache configuration validated successfully");
        }
    }
    
    /**
     * Creates configuration from properties
     */
    public static SmartCacheConfig fromProperties(Properties props) {
        SmartCacheConfigBuilder builder = SmartCacheConfig.builder();
        
        // Basic settings
        builder.enabled(Boolean.parseBoolean(props.getProperty("smart.cache.enabled", "false")));
        builder.compressionEnabled(Boolean.parseBoolean(props.getProperty("smart.cache.compression.enabled", "true")));
        builder.metricsEnabled(Boolean.parseBoolean(props.getProperty("smart.cache.metrics.enabled", "true")));
        builder.transactionAwareEnabled(Boolean.parseBoolean(props.getProperty("smart.cache.transaction.aware", "true")));
        
        // TTL settings
        String defaultTtl = props.getProperty("smart.cache.default.ttl", "10m");
        builder.defaultCacheTtl(parseDuration(defaultTtl));
        
        String maxTtl = props.getProperty("smart.cache.max.ttl", "24h");
        builder.maxCacheTtl(parseDuration(maxTtl));
        
        String cleanupInterval = props.getProperty("smart.cache.cleanup.interval", "1h");
        builder.cleanupInterval(parseDuration(cleanupInterval));
        
        // Limits
        builder.maxCacheRules(Integer.parseInt(props.getProperty("smart.cache.max.rules", "100")));
        
        // StarRocks configuration
        if (builder.build().enabled) {
            StarRocksCacheConfig starRocksConfig = StarRocksCacheConfig.builder()
                    .jdbcUrl(props.getProperty("smart.cache.starrocks.url"))
                    .username(props.getProperty("smart.cache.starrocks.username"))
                    .password(props.getProperty("smart.cache.starrocks.password"))
                    .driverClassName(props.getProperty("smart.cache.starrocks.driver", "com.mysql.cj.jdbc.Driver"))
                    .maximumPoolSize(Integer.parseInt(props.getProperty("smart.cache.starrocks.pool.max", "10")))
                    .minimumIdle(Integer.parseInt(props.getProperty("smart.cache.starrocks.pool.min", "2")))
                    .cacheDatabaseName(props.getProperty("smart.cache.starrocks.database", "smart_cache"))
                    .cacheTableName(props.getProperty("smart.cache.starrocks.table", "query_cache"))
                    .build();
            builder.starRocksConfig(starRocksConfig);
        }
        
        // Cache key configuration
        CacheKeyBuilder.CacheKeyConfig keyConfig = CacheKeyBuilder.CacheKeyConfig.builder()
                .prefix(props.getProperty("smart.cache.key.prefix", "ojp_cache"))
                .separator(props.getProperty("smart.cache.key.separator", ":"))
                .strategy(CacheKeyBuilder.KeyGenerationStrategy.valueOf(
                        props.getProperty("smart.cache.key.strategy", "QUERY_HASH_WITH_PARAMS")))
                .build();
        builder.keyConfig(keyConfig);
        
        return builder.build();
    }
    
    /**
     * Parses duration from string (e.g., "10m", "1h", "30s")
     */
    private static Duration parseDuration(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            throw new IllegalArgumentException("Duration cannot be null or empty");
        }
        
        String trimmed = duration.trim().toLowerCase();
        
        if (trimmed.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        } else if (trimmed.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        } else if (trimmed.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        } else if (trimmed.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        } else {
            // Assume seconds if no unit specified
            return Duration.ofSeconds(Long.parseLong(trimmed));
        }
    }
    
    /**
     * Exports configuration to properties
     */
    public Properties toProperties() {
        Properties props = new Properties();
        
        props.setProperty("smart.cache.enabled", String.valueOf(enabled));
        props.setProperty("smart.cache.compression.enabled", String.valueOf(compressionEnabled));
        props.setProperty("smart.cache.metrics.enabled", String.valueOf(metricsEnabled));
        props.setProperty("smart.cache.transaction.aware", String.valueOf(transactionAwareEnabled));
        props.setProperty("smart.cache.default.ttl", formatDuration(defaultCacheTtl));
        props.setProperty("smart.cache.max.ttl", formatDuration(maxCacheTtl));
        props.setProperty("smart.cache.cleanup.interval", formatDuration(cleanupInterval));
        props.setProperty("smart.cache.max.rules", String.valueOf(maxCacheRules));
        
        if (starRocksConfig != null) {
            props.setProperty("smart.cache.starrocks.url", starRocksConfig.getJdbcUrl());
            props.setProperty("smart.cache.starrocks.username", starRocksConfig.getUsername());
            props.setProperty("smart.cache.starrocks.password", starRocksConfig.getPassword());
            props.setProperty("smart.cache.starrocks.driver", starRocksConfig.getDriverClassName());
            props.setProperty("smart.cache.starrocks.pool.max", String.valueOf(starRocksConfig.getMaximumPoolSize()));
            props.setProperty("smart.cache.starrocks.pool.min", String.valueOf(starRocksConfig.getMinimumIdle()));
            props.setProperty("smart.cache.starrocks.database", starRocksConfig.getCacheDatabaseName());
            props.setProperty("smart.cache.starrocks.table", starRocksConfig.getCacheTableName());
        }
        
        if (keyConfig != null) {
            props.setProperty("smart.cache.key.prefix", keyConfig.getPrefix());
            props.setProperty("smart.cache.key.separator", keyConfig.getSeparator());
            props.setProperty("smart.cache.key.strategy", keyConfig.getStrategy().name());
        }
        
        return props;
    }
    
    /**
     * Formats duration to string
     */
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds % 86400 == 0) {
            return (seconds / 86400) + "d";
        } else if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        } else if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Default configuration for development
     */
    public static SmartCacheConfig defaultConfig() {
        return SmartCacheConfig.builder()
                .enabled(false)
                .starRocksConfig(StarRocksCacheConfig.defaultConfig())
                .build();
    }
}