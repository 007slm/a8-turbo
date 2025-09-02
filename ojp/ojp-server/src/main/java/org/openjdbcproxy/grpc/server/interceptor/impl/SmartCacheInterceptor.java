package org.openjdbcproxy.grpc.server.interceptor.impl;

import com.openjdbcproxy.grpc.SessionInfo;
import com.openjdbcproxy.grpc.StatementRequest;
import com.openjdbcproxy.grpc.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import lombok.SneakyThrows;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptContext;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptor;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.engine.CacheRuleEngine;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.metrics.MetricsCollector;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.key.CacheKeyGenerator;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.CacheDataSourceProvider;
import org.openjdbcproxy.grpc.server.service.CacheStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智能缓存拦截器
 * 根据Redis中的缓存规则引擎计算当前查询SQL是否要走缓存服务
 * 参考Smart Redis Cache实现友好的metric数据
 */
@Slf4j
@Component
public class SmartCacheInterceptor implements StatementServiceInterceptor {

    @Autowired
    private CacheRuleEngine cacheRuleEngine;
    
    @Autowired
    private MetricsCollector metricsCollector;
    
    @Autowired
    private CacheKeyGenerator cacheKeyGenerator;

    @Autowired
    private CacheDataSourceProvider cacheDataSourceProvider;
    
    @Autowired
    private CacheStatsService cacheStatsService;

    /**
     * executeQuery方法前置处理：根据缓存规则匹配情况设置上下文属性
     */
    @Override
    @SneakyThrows
    public void preProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {
        StatementRequest request = context.requestToStatementRequest();
        SessionInfo session = request.getSession();
        String sql = request.getSql().trim();

        // 记录查询开始时间
        long startTime = System.currentTimeMillis();
        context.setAttribute("cache.startTime", startTime);

        // 生成缓存键
        String cacheKey = cacheKeyGenerator.generateCacheKey(request, session);

        // 检查事务状态
        boolean isInTransaction = TransactionStatus.TRX_ACTIVE.equals(
            session.getTransactionInfo().getTransactionStatus());
        
        if (isInTransaction) {
            // 事务中的查询不缓存
            context.setAttribute("cacheSkipped", true);
            context.setAttribute("cacheSkipReason", "Query is in active transaction");
            context.setAttribute("cacheKey", cacheKey);
            metricsCollector.recordCacheSkip("transaction", sql);
            log.info("Cache SKIP for SQL: {}, reason: transaction", sql);
            return;
        }

        // 使用缓存规则引擎判断是否应该缓存
        var decision = cacheRuleEngine.shouldCacheQuery(request, session, cacheKey);

        // 根据缓存决策结果在上下文中设置相应属性
        if (decision.isHit()) {
            context.setAttribute("cacheHit", true);
            context.setAttribute("cacheKey", cacheKey);
            context.setAttribute("cacheTtl", decision.getTtl());
            context.setAttribute("cacheRuleName", decision.getRuleName());
            metricsCollector.recordCacheHit(sql, decision.getRuleName());
            log.info("Cache HIT for SQL: {}, rule: {}", sql, decision.getRuleName());

            // 命中缓存规则：根据 connHash 获取缓存连接并设置拦截
            String connHash = session.getConnHash();
            java.sql.Connection cacheConn = cacheDataSourceProvider.acquireConnectionByConnHash(connHash);
            context.setCurrentInterceptedConnection(cacheConn);
            context.setAttribute("cache.intercepted", true);
        } else if (decision.isMiss()) {
            context.setAttribute("cacheMiss", true);
            context.setAttribute("cacheKey", cacheKey);
            context.setAttribute("cacheTtl", decision.getTtl());
            context.setAttribute("cacheRuleName", decision.getRuleName());
            metricsCollector.recordCacheMiss(sql, decision.getRuleName());
            log.info("Cache MISS for SQL: {}, rule: {}", sql, decision.getRuleName());
        } else if (decision.isSkip()) {
            context.setAttribute("cacheSkipped", true);
            context.setAttribute("cacheSkipReason", decision.getReason());
            context.setAttribute("cacheKey", cacheKey);
            metricsCollector.recordCacheSkip(decision.getReason(), sql);
            log.info("Cache SKIP for SQL: {}, reason: {}", sql, decision.getReason());
        }
    }

    /**
     * executeQuery方法后置处理：记录缓存相关指标
     */
    @Override
    public void postProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {
        // 计算缓存处理时间
        Object startTimeObj = context.getAttribute("cache.startTime");
        if (startTimeObj instanceof Long) {
            Long startTime = (Long) startTimeObj;
            if (startTime > 0) {
                long duration = System.currentTimeMillis() - startTime;
                metricsCollector.recordCacheProcessingTime(duration);
            }
        }

        // 记录查询执行结果
        String sql = context.requestToStatementRequest().getSql();
        boolean success = context.getError() == null;
        metricsCollector.recordQueryExecution(sql, success);

        // 记录统计信息
        recordQueryStats(context, sql, success);

        // 若使用了拦截连接，查询后关闭并复位
        try {
            Object intercepted = context.getAttribute("cache.intercepted");
            if (Boolean.TRUE.equals(intercepted)) {
                java.sql.Connection conn = context.getCurrentConnection();
                if (conn != null) {
                    try { conn.close(); } catch (Exception ignore) {}
                }
                context.setCurrentInterceptedConnection(null);
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * connect 前置：当存在缓存URL配置时，按业务数据库名维度确保缓存连接池
     */
    @Override
    public void preProcessConnect(StatementServiceInterceptContext<?, ?> context) {
        try {
            var cd = context.requestToConnectionDetails();
            String connHash = org.openjdbcproxy.grpc.server.utils.ConnectionHashGenerator.hashConnectionDetails(cd);
            cacheDataSourceProvider.ensureDataSourceByConnHash(connHash);
        } catch (Exception ignore) {
        }
    }

    /**
     * 记录查询统计信息
     */
    private void recordQueryStats(StatementServiceInterceptContext<?, ?> context, String sql, boolean success) {
        try {
            // 生成查询ID
            String queryId = generateQueryId(sql);
            
            // 提取表格信息
            List<String> tables = cacheRuleEngine.extractTablesFromSql(sql);
            
            // 计算执行时间
            Object startTimeObj = context.getAttribute("cache.startTime");
            long executionTime = 0;
            if (startTimeObj instanceof Long) {
                Long startTime = (Long) startTimeObj;
                if (startTime > 0) {
                    executionTime = System.currentTimeMillis() - startTime;
                }
            }
            
            // 判断是否缓存
            boolean isCached = Boolean.TRUE.equals(context.getAttribute("cacheHit"));
            
            // 记录查询统计
            cacheStatsService.recordQueryStats(queryId, sql, tables, executionTime, isCached);
            
            // 记录慢查询（超过1秒的查询）
            if (executionTime > 1000) {
                cacheStatsService.recordSlowQuery(sql, executionTime);
            }
            
        } catch (Exception e) {
            log.warn("Failed to record query stats", e);
        }
    }

    /**
     * 生成查询ID（CRC32哈希）
     */
    private String generateQueryId(String sql) {
        // 简单的哈希实现，实际项目中可以使用更复杂的算法
        return Integer.toHexString(sql.hashCode());
    }
}