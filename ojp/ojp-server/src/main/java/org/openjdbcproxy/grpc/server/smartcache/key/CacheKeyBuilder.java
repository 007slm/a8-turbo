package org.openjdbcproxy.grpc.server.smartcache.key;

import lombok.Builder;
import lombok.Data;
import org.openjdbcproxy.grpc.server.smartcache.rule.QueryContext;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cache key builder that generates consistent cache keys for queries.
 * This supports different key generation strategies for various caching scenarios.
 */
public class CacheKeyBuilder {
    
    private static final String DEFAULT_SEPARATOR = ":";
    private static final String DEFAULT_PREFIX = "ojp_cache";
    
    private final String prefix;
    private final String separator;
    private final KeyGenerationStrategy strategy;
    
    public CacheKeyBuilder() {
        this(DEFAULT_PREFIX, DEFAULT_SEPARATOR, KeyGenerationStrategy.QUERY_HASH_WITH_PARAMS);
    }
    
    public CacheKeyBuilder(String prefix, String separator, KeyGenerationStrategy strategy) {
        this.prefix = prefix;
        this.separator = separator;
        this.strategy = strategy;
    }
    
    /**
     * Builds a cache key for the given query context
     */
    public String build(QueryContext queryContext) {
        return build(queryContext, null);
    }
    
    /**
     * Builds a cache key with additional suffix
     */
    public String build(QueryContext queryContext, String suffix) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // Add prefix
        keyBuilder.append(prefix);
        
        // Add connection-specific components
        if (queryContext.getConnectionHash() != null) {
            keyBuilder.append(separator).append("conn").append(separator)
                     .append(hashString(queryContext.getConnectionHash(), 8));
        }
        
        // Add database-specific components
        if (queryContext.getDatabaseName() != null) {
            keyBuilder.append(separator).append("db").append(separator)
                     .append(queryContext.getDatabaseName());
        }
        
        // Apply generation strategy
        String strategyKey = generateStrategyKey(queryContext);
        keyBuilder.append(separator).append(strategyKey);
        
        // Add suffix if provided
        if (suffix != null && !suffix.isEmpty()) {
            keyBuilder.append(separator).append(suffix);
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Generates the strategy-specific part of the key
     */
    private String generateStrategyKey(QueryContext queryContext) {
        return switch (strategy) {
            case QUERY_HASH_ONLY -> generateQueryHashKey(queryContext);
            case QUERY_HASH_WITH_PARAMS -> generateQueryHashWithParamsKey(queryContext);
            case TABLE_BASED -> generateTableBasedKey(queryContext);
            case QUERY_TYPE_BASED -> generateQueryTypeBasedKey(queryContext);
            case FULL_CONTEXT -> generateFullContextKey(queryContext);
        };
    }
    
    /**
     * Generates key based only on SQL query hash
     */
    private String generateQueryHashKey(QueryContext queryContext) {
        return "qh" + separator + hashString(queryContext.getNormalizedSql(), 16);
    }
    
    /**
     * Generates key based on query hash and parameters
     */
    private String generateQueryHashWithParamsKey(QueryContext queryContext) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("qhp").append(separator)
                 .append(hashString(queryContext.getNormalizedSql(), 16));
        
        if (queryContext.getParameters() != null && !queryContext.getParameters().isEmpty()) {
            String paramsHash = hashParameters(queryContext.getParameters());
            keyBuilder.append(separator).append("p").append(separator).append(paramsHash);
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Generates key based on table names
     */
    private String generateTableBasedKey(QueryContext queryContext) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("tb").append(separator);
        
        if (queryContext.getTableNames() != null && !queryContext.getTableNames().isEmpty()) {
            String tablesString = String.join(",", queryContext.getTableNames());
            keyBuilder.append(hashString(tablesString, 12));
        } else {
            keyBuilder.append("notbl");
        }
        
        keyBuilder.append(separator).append(hashString(queryContext.getNormalizedSql(), 12));
        
        return keyBuilder.toString();
    }
    
    /**
     * Generates key based on query type
     */
    private String generateQueryTypeBasedKey(QueryContext queryContext) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("qt").append(separator)
                 .append(queryContext.getQueryType().name().toLowerCase())
                 .append(separator)
                 .append(hashString(queryContext.getNormalizedSql(), 16));
        
        return keyBuilder.toString();
    }
    
    /**
     * Generates key using full context information
     */
    private String generateFullContextKey(QueryContext queryContext) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("fc").append(separator);
        
        // Add query type
        keyBuilder.append(queryContext.getQueryType().name().toLowerCase()).append(separator);
        
        // Add table names hash
        if (queryContext.getTableNames() != null && !queryContext.getTableNames().isEmpty()) {
            String tablesString = String.join(",", queryContext.getTableNames());
            keyBuilder.append("t").append(hashString(tablesString, 8)).append(separator);
        }
        
        // Add query hash
        keyBuilder.append("q").append(hashString(queryContext.getNormalizedSql(), 12));
        
        // Add parameters hash if present
        if (queryContext.getParameters() != null && !queryContext.getParameters().isEmpty()) {
            String paramsHash = hashParameters(queryContext.getParameters());
            keyBuilder.append(separator).append("p").append(paramsHash);
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Hashes a string to a specified length
     */
    private String hashString(String input, int length) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            String hexString = bytesToHex(hash);
            return hexString.substring(0, Math.min(length, hexString.length()));
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            String hexString = Integer.toHexString(input.hashCode());
            return hexString.substring(0, Math.min(length, hexString.length()));
        }
    }
    
    /**
     * Hashes query parameters
     */
    private String hashParameters(Map<Integer, Object> parameters) {
        String paramString = parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining(","));
        
        return hashString(paramString, 8);
    }
    
    /**
     * Converts bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Key generation strategies
     */
    public enum KeyGenerationStrategy {
        /**
         * Use only the normalized SQL query hash
         */
        QUERY_HASH_ONLY,
        
        /**
         * Use SQL query hash plus parameters (default)
         */
        QUERY_HASH_WITH_PARAMS,
        
        /**
         * Include table names in the key
         */
        TABLE_BASED,
        
        /**
         * Include query type in the key
         */
        QUERY_TYPE_BASED,
        
        /**
         * Use full context for maximum differentiation
         */
        FULL_CONTEXT
    }
    
    /**
     * Configuration for cache key building
     */
    @Data
    @Builder
    public static class CacheKeyConfig {
        @Builder.Default
        private String prefix = DEFAULT_PREFIX;
        
        @Builder.Default
        private String separator = DEFAULT_SEPARATOR;
        
        @Builder.Default
        private KeyGenerationStrategy strategy = KeyGenerationStrategy.QUERY_HASH_WITH_PARAMS;
        
        @Builder.Default
        private boolean includeConnectionInfo = true;
        
        @Builder.Default
        private boolean includeDatabaseInfo = true;
        
        @Builder.Default
        private int hashLength = 16;
    }
    
    /**
     * Creates a CacheKeyBuilder from configuration
     */
    public static CacheKeyBuilder fromConfig(CacheKeyConfig config) {
        return new CacheKeyBuilder(config.prefix, config.separator, config.strategy);
    }
}