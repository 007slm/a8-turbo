package org.openjdbcproxy.grpc.server.smartcache.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.smartcache.cache.CacheEntry;
import org.openjdbcproxy.grpc.server.smartcache.cache.StarRocksCacheManager;
import org.openjdbcproxy.grpc.server.smartcache.parser.SqlParser;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRule;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRuleEngine;
import org.openjdbcproxy.grpc.server.smartcache.rule.QueryContext;
import org.openjdbcproxy.grpc.server.smartcache.transaction.TransactionStateTracker;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Smart query interceptor that handles cache logic with transaction awareness.
 * This is the main orchestrator that brings together all cache components.
 */
@Slf4j
public class SmartQueryInterceptor {
    
    private final CacheRuleEngine ruleEngine;
    private final StarRocksCacheManager cacheManager;
    private final TransactionStateTracker transactionTracker;
    private final SqlParser sqlParser;
    private final SmartCacheMetrics metrics;
    
    public SmartQueryInterceptor(CacheRuleEngine ruleEngine, 
                                StarRocksCacheManager cacheManager,
                                TransactionStateTracker transactionTracker,
                                SmartCacheMetrics metrics) {
        this.ruleEngine = ruleEngine;
        this.cacheManager = cacheManager;
        this.transactionTracker = transactionTracker;
        this.sqlParser = new SqlParser();
        this.metrics = metrics;
        
        log.info("Smart query interceptor initialized");
    }
    
    /**
     * Intercepts a query and determines cache action
     */
    public CacheDecision interceptQuery(QueryInterceptContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Build query context for rule evaluation
            QueryContext queryContext = buildQueryContext(context);
            
            // Check transaction safety first
            if (!isTransactionSafe(queryContext)) {
                metrics.recordCacheSkip("transaction_unsafe");
                return CacheDecision.skip("Query skipped due to transaction state");
            }
            
            // Evaluate cache rules
            Optional<CacheRule.CacheAction> action = ruleEngine.evaluate(queryContext);
            
            if (action.isEmpty() || !action.get().isEnabled()) {
                metrics.recordCacheSkip("no_rule_match");
                return CacheDecision.skip("No matching cache rule or caching disabled");
            }
            
            // Check cache for existing result
            String cacheKey = generateCacheKey(queryContext, action.get());
            CacheEntry cacheEntry = getCacheEntry(cacheKey);
            
            if (cacheEntry != null && cacheEntry.isValid()) {
                metrics.recordCacheHit();
                return CacheDecision.hit(cacheEntry, cacheKey);
            }
            
            // Cache miss - prepare for caching result
            metrics.recordCacheMiss();
            return CacheDecision.miss(cacheKey, action.get());
            
        } catch (Exception e) {
            log.warn("Error in query interception: {}", e.getMessage(), e);
            metrics.recordCacheError();
            return CacheDecision.skip("Error during cache processing: " + e.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordInterceptionTime(duration);
        }
    }
    
    /**
     * Stores query result in cache
     */
    public void storeResult(String cacheKey, String resultData, 
                          CacheRule.CacheAction action, QueryContext queryContext) {
        try {
            String metadata = buildCacheMetadata(queryContext);
            cacheManager.putCache(cacheKey, 
                                generateQueryHash(queryContext), 
                                resultData, 
                                metadata, 
                                action.getTtl());
            
            metrics.recordCacheStore();
            log.debug("Result stored in cache: key={}, ttl={}", cacheKey, action.getTtl());
            
        } catch (SQLException e) {
            log.warn("Failed to store result in cache: {}", e.getMessage());
            metrics.recordCacheError();
        }
    }
    
    /**
     * Notifies about transaction events
     */
    public void onTransactionStart(String sessionId) {
        transactionTracker.startTransaction(sessionId);
        metrics.recordTransactionStart();
    }
    
    public void onTransactionCommit(String sessionId) {
        transactionTracker.endTransaction(sessionId);
        metrics.recordTransactionCommit();
    }
    
    public void onTransactionRollback(String sessionId) {
        transactionTracker.endTransaction(sessionId);
        metrics.recordTransactionRollback();
    }
    
    public void onWriteOperation(String sessionId) {
        transactionTracker.markWrite(sessionId);
        metrics.recordWriteOperation();
    }
    
    /**
     * Builds query context from intercept context
     */
    private QueryContext buildQueryContext(QueryInterceptContext context) {
        String normalizedSql = sqlParser.normalizeSql(context.getSql());
        List<String> tableNames = sqlParser.extractTableNames(context.getSql());
        QueryContext.QueryType queryType = sqlParser.getQueryType(context.getSql());
        
        boolean inTransaction = transactionTracker.isInTransaction(context.getSessionId());
        boolean hasWrites = transactionTracker.hasWrites(context.getSessionId());
        
        return QueryContext.builder()
                .sql(context.getSql())
                .normalizedSql(normalizedSql)
                .tableNames(tableNames)
                .parameters(context.getParameters())
                .sessionId(context.getSessionId())
                .connectionHash(context.getConnectionHash())
                .databaseName(context.getDatabaseName())
                .schemaName(context.getSchemaName())
                .queryType(queryType)
                .inTransaction(inTransaction)
                .transactionHasWrites(hasWrites)
                .metadata(new HashMap<>())
                .build();
    }
    
    /**
     * Checks if it's safe to use cache given transaction state
     */
    private boolean isTransactionSafe(QueryContext queryContext) {
        // Never cache write operations
        if (sqlParser.isWriteOperation(queryContext.getSql())) {
            return false;
        }
        
        // Don't cache reads in transactions that have performed writes
        if (queryContext.isInTransaction() && queryContext.isTransactionHasWrites()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Generates cache key for the query
     */
    private String generateCacheKey(QueryContext queryContext, CacheRule.CacheAction action) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // Base key components
        keyBuilder.append("query:")
                 .append(queryContext.getConnectionHash())
                 .append(":")
                 .append(generateQueryHash(queryContext));
        
        // Add parameters if present
        if (queryContext.getParameters() != null && !queryContext.getParameters().isEmpty()) {
            keyBuilder.append(":params:")
                     .append(generateParameterHash(queryContext.getParameters()));
        }
        
        // Add rule-specific suffix if provided
        if (action.getKeySuffix() != null) {
            keyBuilder.append(":").append(action.getKeySuffix());
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Generates hash for the SQL query
     */
    private String generateQueryHash(QueryContext queryContext) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(queryContext.getNormalizedSql().getBytes());
            return bytesToHex(hash).substring(0, 16); // Use first 16 characters
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 not available, using fallback hash");
            return String.valueOf(queryContext.getNormalizedSql().hashCode());
        }
    }
    
    /**
     * Generates hash for query parameters
     */
    private String generateParameterHash(Map<Integer, Object> parameters) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder paramString = new StringBuilder();
            
            parameters.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> paramString.append(entry.getKey())
                                               .append("=")
                                               .append(entry.getValue())
                                               .append(";"));
            
            byte[] hash = md.digest(paramString.toString().getBytes());
            return bytesToHex(hash).substring(0, 8); // Use first 8 characters
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(parameters.hashCode());
        }
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
     * Gets cache entry with error handling
     */
    private CacheEntry getCacheEntry(String cacheKey) {
        try {
            return cacheManager.getCache(cacheKey);
        } catch (SQLException e) {
            log.warn("Failed to retrieve cache entry: {}", e.getMessage());
            metrics.recordCacheError();
            return null;
        }
    }
    
    /**
     * Builds metadata for cache entry
     */
    private String buildCacheMetadata(QueryContext queryContext) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("queryType", queryContext.getQueryType().name());
        metadata.put("tableNames", queryContext.getTableNames());
        metadata.put("sessionId", queryContext.getSessionId());
        metadata.put("timestamp", System.currentTimeMillis());
        
        // Convert to JSON string (simplified - could use Jackson)
        return metadata.toString();
    }
    
    /**
     * Cleans up resources for a session
     */
    public void cleanupSession(String sessionId) {
        transactionTracker.removeSession(sessionId);
        log.debug("Cleaned up cache resources for session: {}", sessionId);
    }
}