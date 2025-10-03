package org.openjdbcproxy.cache.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheDecision;
import org.openjdbcproxy.cache.service.AsyncStatsService;
import org.openjdbcproxy.cache.service.CacheDecisionService;
import org.openjdbcproxy.cache.service.CacheInterceptorService;
import org.openjdbcproxy.cache.service.CacheKeyGenerator;
import org.openjdbcproxy.cache.service.CacheMetricsCollector;
import org.openjdbcproxy.cache.service.CacheDataSourceProvider;
import org.openjdbcproxy.cache.service.PerformanceMonitoringService;
import org.openjdbcproxy.cache.util.ReadableKeyGenerator;
import com.openjdbcproxy.grpc.SessionInfo;
import com.openjdbcproxy.grpc.StatementRequest;
import com.openjdbcproxy.grpc.TransactionStatus;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存拦截器服务实现
 * 整合缓存决策、键生成、数据源管理等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInterceptorServiceImpl implements CacheInterceptorService {
    
    private final CacheDecisionService cacheDecisionService;
    private final CacheKeyGenerator cacheKeyGenerator;
    private final CacheMetricsCollector metricsCollector;
    private final CacheDataSourceProvider dataSourceProvider;
    private final AsyncStatsService asyncStatsService;
    private final PerformanceMonitoringService performanceMonitoringService;
    private final ReadableKeyGenerator readableKeyGenerator;
    private final AtomicLong requestCounter = new AtomicLong(0);
    
    @Override
    public CacheProcessResult preProcessQuery(StatementRequest request, SessionInfo sessionInfo) {
        long startTime = System.currentTimeMillis();
        long requestId = requestCounter.incrementAndGet();
        String connHash = sessionInfo.getConnHash();
        
        try {
            // 检查事务状态
            if (isInTransaction(sessionInfo)) {
                log.debug("[{}] Skipping cache due to active transaction", requestId);
                
                // 异步记录事务跳过统计
                asyncStatsService.recordCacheOperation(connHash, "TRANSACTION_SKIP", 
                    request.getSql().length(), System.currentTimeMillis() - startTime, false);
                
                // 性能监控记录
                performanceMonitoringService.recordCacheSkip(connHash, "active transaction", request.getSql());
                
                return new CacheProcessResult(null, null, null, startTime);
            }
            
            // 进行缓存决策
            CacheDecision decision = cacheDecisionService.makeDecision(connHash, request.getSql());
            
            if (!decision.isUseCache()) {
                log.debug("[{}] Cache decision: skip - {}", requestId, decision.getReason());
                recordCacheSkip(decision.getReason(), request.getSql());
                
                // 异步记录决策跳过统计
                asyncStatsService.recordCacheOperation(connHash, "DECISION_SKIP", 
                    request.getSql().length(), System.currentTimeMillis() - startTime, false);
                
                // 性能监控记录
                performanceMonitoringService.recordCacheSkip(connHash, decision.getReason(), request.getSql());
                
                return new CacheProcessResult(decision, null, null, startTime);
            }
            
            // 生成缓存键
            String cacheKey = cacheKeyGenerator.generateCacheKey(sessionInfo, request);
            decision.setCacheKey(cacheKey);
            
            // 尝试从缓存获取数据
            long cacheRetrieveStart = System.currentTimeMillis();
            ResultSet cachedResult = getCachedResult(cacheKey, sessionInfo);
            long cacheRetrieveTime = System.currentTimeMillis() - cacheRetrieveStart;
            
            if (cachedResult != null) {
                log.debug("[{}] Cache hit for key: {}, retrieve time: {}ms", requestId, cacheKey, cacheRetrieveTime);
                recordCacheHit(request.getSql(), decision.getMatchedRuleId());
                
                // 异步记录缓存命中统计
                asyncStatsService.recordCacheOperation(connHash, "HIT", 
                    request.getSql().length(), cacheRetrieveTime, true);
                
                // 性能监控记录
                performanceMonitoringService.recordCacheHit(connHash, request.getSql(), cacheRetrieveTime);
                
            } else {
                log.debug("[{}] Cache miss for key: {}, retrieve time: {}ms", requestId, cacheKey, cacheRetrieveTime);
                recordCacheMiss(request.getSql(), decision.getMatchedRuleId());
                
                // 异步记录缓存未命中统计
                asyncStatsService.recordCacheOperation(connHash, "MISS", 
                    request.getSql().length(), cacheRetrieveTime, false);
                
                // 性能监控记录
                performanceMonitoringService.recordCacheMiss(connHash, request.getSql(), cacheRetrieveTime);
            }
            
            Connection cacheConnection = getCacheDataSourceConnection(sessionInfo);
            return new CacheProcessResult(decision, cachedResult, cacheConnection, startTime);
            
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            log.error("[{}] Error in cache pre-processing, error time: {}ms", requestId, errorTime, e);
            recordCacheSkip("Error: " + e.getMessage(), request.getSql());
            
            // 异步记录错误统计
            asyncStatsService.recordCacheOperation(connHash, "ERROR", 
                request.getSql().length(), errorTime, false);
            
            return new CacheProcessResult(null, null, null, startTime);
        }
    }
    
    @Override
    public void postProcessQuery(StatementRequest request, long executionTimeMs, boolean success) {
        try {
            // 记录处理时间
            metricsCollector.recordCacheProcessingTime(executionTimeMs);
            
            // 记录查询执行结果
            String sql = request.getSql();
            metricsCollector.recordQueryExecution(sql, success);
            
            // 异步记录查询执行统计
            String connHash = request.getSession().getConnHash();
             asyncStatsService.recordCacheOperation(connHash, "QUERY_EXECUTION", 
                 sql != null ? sql.length() : 0, executionTimeMs, success);
             
             // 性能监控记录
             performanceMonitoringService.recordQueryExecution(connHash, sql, executionTimeMs, success);
             
             log.debug("Post-processed query: {}, execution time: {}ms, success: {}", sql, executionTimeMs, success);
            
        } catch (Exception e) {
            log.error("Error in cache post-processing", e);
            
            // 异步记录错误统计
            String connHash = request.getSession().getConnHash();
            String sql = request != null ? request.getSql() : null;
            asyncStatsService.recordCacheOperation(connHash, "POST_PROCESS_ERROR", 
                sql != null ? sql.length() : 0, executionTimeMs, false);
            
            // 性能监控记录错误
            performanceMonitoringService.recordQueryExecution(connHash, sql, executionTimeMs, false);
        }
    }
    
    @Override
    public String generateCacheKey(StatementRequest request, SessionInfo sessionInfo) {
        return cacheKeyGenerator.generateCacheKey(sessionInfo, request);
    }
    
    @Override
    public Connection getCacheDataSourceConnection(SessionInfo sessionInfo) {
        try {
            return dataSourceProvider.acquireConnectionByDbName(sessionInfo.getConnHash());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void ensureDataSource(String connHash) {
        dataSourceProvider.ensureDataSourceByConnHash(connHash);
    }
    
    @Override
    public List<String> extractTables(String sql) {
        return cacheDecisionService.extractTables(sql);
    }
    
    private boolean isInTransaction(SessionInfo sessionInfo) {
        // 检查是否在事务中
        if (sessionInfo.hasTransactionInfo()) {
            return sessionInfo.getTransactionInfo().getTransactionStatus() == TransactionStatus.TRX_ACTIVE;
        }
        return false;
    }
    
    private ResultSet getCachedResult(String cacheKey, SessionInfo sessionInfo) {
        // 临时实现，后续会完善缓存查询逻辑
        return null;
    }
    
    private void storeCacheResult(String cacheKey, ResultSet resultSet, int ttlSeconds) {
        // 临时实现，后续会完善缓存存储逻辑
        log.debug("Storing cache result for key: {} with TTL: {}s", cacheKey, ttlSeconds);
    }
    
    private void recordCacheHit(String sql, String ruleName) {
        metricsCollector.recordCacheHit(sql, ruleName);
    }
    
    private void recordCacheMiss(String sql, String ruleName) {
        metricsCollector.recordCacheMiss(sql, ruleName);
    }
    
    private void recordCacheSkip(String reason, String sql) {
        metricsCollector.recordCacheSkip(reason, sql);
    }
    
    private void recordCacheProcessingTime(long durationMs) {
        metricsCollector.recordCacheProcessingTime(durationMs);
    }
    
    private void recordQueryExecution(boolean success) {
        metricsCollector.recordQueryExecution(null, success);
    }

}