package org.openjdbcproxy.cache.interceptor;

import com.openjdbcproxy.grpc.SessionInfo;
import com.openjdbcproxy.grpc.StatementRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.service.CacheInterceptorService;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptContext;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptor;
import org.openjdbcproxy.grpc.server.utils.JdbcUrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;

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
        context.setAttribute("cache.intercepted", false);
        Connection cacheConn = cacheInterceptorService.preProcessQuery(request, session);
        if (cacheConn != null) {
            context.setCurrentInterceptedConnection(cacheConn);
            context.setAttribute("cache.intercepted", true);
        }
    }

    /**
     * executeQuery方法后置处理：记录缓存相关指标
     */
    @Override
    public void postProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {
        StatementRequest request = context.requestToStatementRequest();
        
        // 计算执行时间
        long startTime = (long)context.getAttribute("cache.startTime");
        long executionTime = System.currentTimeMillis() - startTime;

        boolean success = context.getError() == null;
        
        // 使用缓存拦截器服务进行后处理
        cacheInterceptorService.postProcessQuery(request, executionTime, success);
        
        // 若使用了拦截连接，查询后关闭并复位
        boolean intercepted = (boolean)context.getAttribute("cache.intercepted");
        if (intercepted) {
            Connection conn = context.getCurrentInterceptedConnection();
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignore) {}
            }
            context.setCurrentInterceptedConnection(null);
        }
    }

}