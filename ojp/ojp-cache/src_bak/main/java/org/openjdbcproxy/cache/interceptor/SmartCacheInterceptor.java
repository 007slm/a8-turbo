package org.openjdbcproxy.cache.interceptor;

import org.openjdbcproxy.grpc.SessionInfo;
import org.openjdbcproxy.grpc.StatementRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.service.CacheInterceptorService;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptContext;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptor;
import org.openjdbcproxy.grpc.server.utils.ConnectionHashGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 智能缓存拦截器
 * 根据Redis中的缓存规则引擎计算当前查询SQL是否要走缓存服务
 * 参考Smart Redis Cache实现友好的metric数据
 */
@Slf4j
@Component
public class SmartCacheInterceptor implements StatementServiceInterceptor {

    @Autowired
    private CacheInterceptorService cacheInterceptorService;
    
    /**
     * executeQuery方法前置处理：根据缓存规则匹配情况设置上下文属性
     */
    @Override
    @SneakyThrows
    public void preProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {
        StatementRequest request = context.requestToStatementRequest();
        SessionInfo session = request.getSession();
        
        // 记录查询开始时间
        long startTime = System.currentTimeMillis();
        context.setAttribute("cache.startTime", startTime);
        
        // 使用缓存拦截器服务进行预处理
        var result = cacheInterceptorService.preProcessQuery(request, session);
        
        // 设置上下文属性
        context.setAttribute("cacheKey", result.getCacheKey());
        
        if (result.isHit()) {
            context.setAttribute("cacheHit", true);
            context.setAttribute("cacheTtl", result.getTtl());
            context.setAttribute("cacheRuleName", result.getRuleName());
            
            // 获取缓存连接并设置拦截
            java.sql.Connection cacheConn = result.getCacheConnection();
            if (cacheConn != null) {
                context.setCurrentInterceptedConnection(cacheConn);
                context.setAttribute("cache.intercepted", true);
            }
        } else if (result.isMiss()) {
            context.setAttribute("cacheMiss", true);
            context.setAttribute("cacheTtl", result.getTtl());
            context.setAttribute("cacheRuleName", result.getRuleName());
        } else if (result.isSkip()) {
            context.setAttribute("cacheSkipped", true);
            context.setAttribute("cacheSkipReason", result.getSkipReason());
        }
    }

    /**
     * executeQuery方法后置处理：记录缓存相关指标
     */
    @Override
    public void postProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {
        StatementRequest request = context.requestToStatementRequest();
        
        // 计算执行时间
        Object startTimeObj = context.getAttribute("cache.startTime");
        long executionTime = 0;
        if (startTimeObj instanceof Long) {
            Long startTime = (Long) startTimeObj;
            if (startTime > 0) {
                executionTime = System.currentTimeMillis() - startTime;
            }
        }
        
        boolean success = context.getError() == null;
        
        // 使用缓存拦截器服务进行后处理
        cacheInterceptorService.postProcessQuery(request, executionTime, success);
        
        // 若使用了拦截连接，查询后关闭并复位
        Object intercepted = context.getAttribute("cache.intercepted");
        if (Boolean.TRUE.equals(intercepted)) {
            java.sql.Connection conn = context.getCurrentConnection();
            if (conn != null) {
                try { conn.close(); } catch (Exception ignore) {}
            }
            context.setCurrentInterceptedConnection(null);
        }
    }

    /**
     * connect 前置：当存在缓存URL配置时，按业务数据库名维度确保缓存连接池
     */
    @Override
    public void preProcessConnect(StatementServiceInterceptContext<?, ?> context) {
        var cd = context.requestToConnectionDetails();
        String connHash = ConnectionHashGenerator.hashConnectionDetails(cd);
        cacheInterceptorService.ensureDataSource(connHash);
    }
}