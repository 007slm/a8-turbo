package org.openjdbcproxy.cache.interceptor;

import org.openjdbcproxy.grpc.SessionInfo;
import org.openjdbcproxy.grpc.StatementRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.service.CacheInterceptorService;
import org.openjdbcproxy.grpc.server.Session;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceGrpcInterceptor;
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

        Session sessionContext = context.getSessionManager().getSession(session);
        Connection existingCacheConn = null;
        if (sessionContext != null) {
            existingCacheConn = (Connection) sessionContext.getAttr("cache.intercepted.conn");
        }

        Connection cacheConn = cacheInterceptorService.preProcessQuery(request, session, existingCacheConn);
        if (cacheConn != null) {
            String connHash = session.getConnHash();
            boolean isOracle = connHash != null && (connHash.contains("oracle:") || connHash.contains("jdbc:oracle:"));

            Connection effectiveConn = cacheConn;
            if (isOracle) {
                // Wrap the connection to emulate Oracle metadata behavior only for Oracle
                // clients
                effectiveConn = new org.openjdbcproxy.cache.emulator.OracleCompatibleConnection(cacheConn);
                log.info("缓存命中 (Oracle Metadata Emulator Enabled)");
            } else {
                log.info("缓存命中");
            }

            context.setCurrentInterceptedConnection(effectiveConn);
            context.setAttribute("cache.intercepted", true);
            if (sessionContext != null && existingCacheConn == null) {
                // Store the connection so subsequent reused calls also get it
                sessionContext.addAttr("cache.intercepted.conn", effectiveConn);
            }
        }
    }

    /**
     * executeQuery方法后置处理：记录缓存相关指标
     */
    @Override
    public void postProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {
        StatementRequest request = context.requestToStatementRequest();
        SessionInfo sessionInfo = request.getSession();
        // 计算执行时间
        long startTime = (long) context.getAttribute("cache.startTime");
        long executionTime = System.currentTimeMillis() - startTime;

        boolean success = context.getError() == null;

        // 使用缓存拦截器服务进行后处理
        cacheInterceptorService.postProcessQuery(request, executionTime, success);

        // 清理上下文中的缓存连接引用，由Session生命周期统一管理连接释放
        context.setCurrentInterceptedConnection(null);

        // 若使用了拦截连接，查询后关闭并复位
        // boolean intercepted = (boolean)context.getAttribute("cache.intercepted");
        // if (intercepted) {
        // Connection conn = context.getCurrentInterceptedConnection();
        // if (conn != null) {
        // try {
        // conn.close();
        // } catch (Exception ignore) {}
        // }
        // context.setCurrentInterceptedConnection(null);
        // }
    }

}
