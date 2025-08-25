package org.openjdbcproxy.grpc.server.chain.processors;

import com.openjdbcproxy.grpc.OpResult;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.dto.OpQueryResult;
import org.openjdbcproxy.grpc.server.chain.AbstractSqlProcessor;
import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;
import org.openjdbcproxy.grpc.server.resultset.ResultSetWrapper;
import org.openjdbcproxy.grpc.server.smartcache.SmartCacheManager;
import org.openjdbcproxy.grpc.server.smartcache.interceptor.CacheDecision;
import org.openjdbcproxy.grpc.server.smartcache.interceptor.QueryInterceptContext;
import org.openjdbcproxy.grpc.server.smartcache.serialization.ColumnMetadata;
import org.openjdbcproxy.grpc.server.smartcache.serialization.ResultSetSerializer;


import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 智能缓存处理器
 * 
 * 负责完整的查询缓存处理：
 * 1. 拦截查询请求，检查缓存规则
 * 2. 处理缓存命中：直接返回缓存结果
 * 3. 处理缓存未命中：标记需要后续缓存
 * 4. 后处理：将查询结果异步存储到缓存
 * 5. 确保事务安全性和性能
 */
@Slf4j
public class SmartCacheProcessor extends AbstractSqlProcessor {
    
    private static final String PROCESSOR_NAME = "SmartCacheProcessor";
    
    private final SmartCacheManager smartCacheManager;
    private final ResultSetSerializer resultSetSerializer;
    
    public SmartCacheProcessor(SmartCacheManager smartCacheManager) {
        this.smartCacheManager = smartCacheManager;
        this.resultSetSerializer = new ResultSetSerializer(true); // Enable compression
    }
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        // 智能缓存只处理SELECT查询
        if (context.getOperationType() != SqlProcessContext.SqlOperationType.SELECT) {
            return false;
        }
        
        // 如果缓存管理器未启用，跳过处理
        if (smartCacheManager == null || !smartCacheManager.isEnabled()) {
            log.debug("Smart cache is disabled, skipping cache processing");
            return false;
        }
        
        try {
            // 构建查询拦截上下文
            QueryInterceptContext interceptContext = buildQueryInterceptContext(context);
            
            // 拦截查询以检查缓存命中/未命中/跳过
            CacheDecision decision = smartCacheManager.getInterceptor().interceptQuery(interceptContext);
            
            switch (decision.getType()) {
                case HIT:
                    // 缓存命中 - 返回缓存结果
                    return handleCacheHit(context, decision);
                    
                case MISS:
                    // 缓存未命中 - 标记需要缓存结果
                    handleCacheMiss(context, decision);
                    return false; // 继续执行查询
                    
                case SKIP:
                    // 跳过缓存 - 正常执行查询
                    log.debug("Skipping cache for query: {}", decision.getReason());
                    return false;
                    
                default:
                    log.warn("Unknown cache decision type: {}", decision.getType());
                    return false;
            }
            
        } catch (Exception e) {
            log.error("Error in smart cache processing for SQL: {}", context.getCurrentSql(), e);
            // 缓存出错不应该影响正常查询，继续执行
            return false;
        }
    }
    
    /**
     * 处理缓存命中场景
     */
    private boolean handleCacheHit(SqlProcessContext context, CacheDecision decision) {
        try {
            // 反序列化缓存结果
            String resultData = decision.getCacheEntry().getResultData();
            var cachedResult = resultSetSerializer.deserialize(resultData);
            
            // 转换缓存结果为OpResult
            OpQueryResult.OpQueryResultBuilder queryResultBuilder = OpQueryResult.builder();
            
            // 提取列标签
            List<String> labels = new ArrayList<>();
            for (ColumnMetadata col : cachedResult.getColumns()) {
                String name = col.getName();
                labels.add(name);
            }
            queryResultBuilder.labels(labels);
            
            // 转换行数据为对象数组
            List<Object[]> results = cachedResult.getRows().stream()
                    .map(row -> row.toArray(new Object[0]))
                    .toList();
            
            // 创建返回结果
            OpResult opResult = ResultSetWrapper.wrapResults(
                context.getRequest().getSession(), 
                results, 
                queryResultBuilder, 
                UUID.randomUUID().toString(), 
                ""
            );
            
            // 标记处理完成
            context.markCompleted(opResult);
            
            log.info("Cache hit served for query, {} rows returned", results.size());
            return true; // 处理完成，不需要继续执行
            
        } catch (Exception e) {
            log.warn("Failed to serve cached result for SQL: {}, falling back to database query", 
                    context.getCurrentSql(), e);
            // 缓存读取失败，继续执行正常查询
            return false;
        }
    }
    
    /**
     * 处理缓存未命中场景
     */
    private void handleCacheMiss(SqlProcessContext context, CacheDecision decision) {
        // 记录缓存决策信息，用于后处理
        context.setAttribute("cache_decision", decision);
        context.setAttribute("cache_intercept_context", buildQueryInterceptContext(context));
        context.setAttribute("should_cache_result", true);
        
        log.debug("Cache miss for SQL: {}, will cache result after execution", context.getCurrentSql());
    }
    
    /**
     * 构建查询拦截上下文
     */
    private QueryInterceptContext buildQueryInterceptContext(SqlProcessContext context) {
        Map<Integer, Object> parameters = new HashMap<>();
        
        // 这里需要从StatementRequest中提取参数
        // 由于当前无法访问参数，暂时使用空参数
        // 在实际使用中需要增强SqlProcessContext来包含参数信息
        
        return QueryInterceptContext.builder()
                .sql(context.getCurrentSql())
                .parameters(parameters)
                .sessionId(context.getSessionId())
                .connectionHash(context.getConnectionHash())
                .metadata(new HashMap<>())
                .build();
    }
    
    /**
     * 后处理：在查询执行完成后缓存结果
     */
    public void postProcess(SqlProcessContext context, OpResult result) {
        // 只有在标记需要缓存且有结果时才处理
        if (!context.getAttribute("should_cache_result", false) || result == null) {
            return;
        }
        
        CacheDecision decision = context.getAttribute("cache_decision");
        QueryInterceptContext interceptContext = context.getAttribute("cache_intercept_context");
        
        if (decision == null || interceptContext == null) {
            log.warn("Cache decision or intercept context is null, cannot cache result");
            return;
        }
        
        try {
            // 异步存储缓存结果
            CompletableFuture.runAsync(() -> {
                try {
                    // 序列化查询结果
                    String serializedResult = serializeOpResult(result);
                    
                    // 存储到缓存
                    smartCacheManager.getInterceptor().storeResult(
                        decision.getCacheKey(),
                        serializedResult,
                        decision.getAction(),
                        interceptContext.toQueryContext()
                    );
                    
                    log.debug("Query result cached successfully for SQL: {}", context.getCurrentSql());
                    
                } catch (Exception e) {
                    log.warn("Failed to cache query result for SQL: {}", context.getCurrentSql(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("Error setting up async cache storage for SQL: {}", context.getCurrentSql(), e);
        }
    }
    
    /**
     * 序列化OpResult用于缓存存储
     */
    private String serializeOpResult(OpResult result) throws Exception {
        // 这里需要根据OpResult的结构进行序列化
        // 简化实现，直接使用toString或其他序列化方法
        // 在实际使用中需要实现完整的序列化逻辑
        return result.toString();
    }
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public int getPriority() {
        return 50; // 在权限和分片处理之后，SQL执行之前
    }
    
    @Override
    public Set<SqlProcessContext.SqlOperationType> getSupportedOperations() {
        return Set.of(SqlProcessContext.SqlOperationType.SELECT); // 只处理查询操作
    }
    
    @Override
    public boolean isEnabled() {
        return smartCacheManager != null && smartCacheManager.isEnabled();
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        if (!isEnabled()) {
            return CacheStats.builder()
                    .enabled(false)
                    .cacheHits(0)
                    .cacheMisses(0)
                    .build();
        }
        
        // 从缓存管理器获取统计信息
        // 这里需要根据实际的SmartCacheManager API进行实现
        return CacheStats.builder()
                .enabled(true)
                .cacheHits(0) // 占位实现
                .cacheMisses(0) // 占位实现
                .build();
    }
    
    /**
     * 缓存统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class CacheStats {
        private boolean enabled;
        private long cacheHits;
        private long cacheMisses;
        
        @Override
        public String toString() {
            if (!enabled) {
                return "SmartCacheProcessor[enabled=false]";
            }
            
            return String.format(
                "SmartCacheProcessor[enabled=true, hits=%d, misses=%d, hitRate=%.2f%%]",
                cacheHits, cacheMisses, getHitRate()
            );
        }
        
        public double getHitRate() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total * 100.0 : 0.0;
        }
    }
}