package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.SessionInfo;
import org.openjdbcproxy.grpc.StatementRequest;
import org.openjdbcproxy.grpc.TransactionStatus;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 缓存拦截器服务实现
 * 整合缓存决策、键生成、数据源管理等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInterceptorService {

    private final CacheDecisionService cacheDecisionService;
    private final CacheDataSourceProvider dataSourceProvider;
    private final PerformanceMonitoringService performanceMonitoringService;

    public Connection preProcessQuery(StatementRequest request, SessionInfo sessionInfo,
            Connection existingCacheConnection, org.openjdbcproxy.grpc.server.Session session) throws SQLException {
        long startTime = System.currentTimeMillis();
        String connHash = sessionInfo.getConnHash();

        // 检查事务状态
        if (isInModifyTransaction(sessionInfo, session)) {
            performanceMonitoringService.recordCacheSkip(connHash, "事务中执行有副作用的sql后不走缓存", request.getSql());
            return null;
        }

        // 进行缓存决策
        boolean useCache = cacheDecisionService.makeDecision(connHash, request.getSql());

        if (useCache) {
            performanceMonitoringService.recordCacheHit(connHash, request.getSql(), startTime);
            Connection cacheConnection = existingCacheConnection != null
                    ? existingCacheConnection
                    : getCacheDataSourceConnection(sessionInfo);
            return cacheConnection;
        } else {
            performanceMonitoringService.recordCacheMiss(connHash, request.getSql(), startTime);
            return null;
        }
    }

    public void postProcessQuery(StatementRequest request, long executionTimeMs, boolean success) {
        String sql = request.getSql();
        String connHash = request.getSession().getConnHash();
        performanceMonitoringService.recordQueryExecution(connHash, sql, executionTimeMs, success);

        log.debug("查询后处理: {}, 执行时间: {}ms, 成功: {}", sql, executionTimeMs, success);
    }

    public Connection getCacheDataSourceConnection(SessionInfo sessionInfo) throws SQLException {
        return dataSourceProvider.acquireConnectionByDbName(sessionInfo.getConnHash());
    }

    private boolean isInModifyTransaction(SessionInfo sessionInfo, org.openjdbcproxy.grpc.server.Session session) {
        // 只读连接始终允许缓存
        if (sessionInfo.getReadOnly()) {
            return false;
        }

        // 如果没有开启事务，允许走缓存
        if (!sessionInfo.hasTransactionInfo()) {
            return false;
        }

        // 检查事务是否为活跃状态
        if (sessionInfo.getTransactionInfo().getTransactionStatus() != TransactionStatus.TRX_ACTIVE) {
            return false;
        }

        // 检查事务是否已经执行过有副作用的SQL（dirty）
        if (session != null) {
            Object isDirty = session.getAttr(org.openjdbcproxy.grpc.server.Constants.TRX_IS_DIRTY);
            return isDirty != null && (Boolean) isDirty;
        }

        // session 为 null 时保守处理：如果在事务中则不走缓存
        return true;
    }

}
