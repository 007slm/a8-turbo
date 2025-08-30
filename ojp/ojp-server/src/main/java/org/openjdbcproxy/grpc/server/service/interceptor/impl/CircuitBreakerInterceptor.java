package org.openjdbcproxy.grpc.server.service.interceptor.impl;

import com.openjdbcproxy.grpc.StatementRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.CircuitBreaker;
import org.openjdbcproxy.grpc.server.SqlStatementXXHash;
import org.openjdbcproxy.grpc.server.service.interceptor.context.CurrentRequestContext;
import org.openjdbcproxy.grpc.server.service.interceptor.StatementServiceInterceptor;
import org.springframework.stereotype.Component;

import java.sql.SQLDataException;
import java.sql.SQLException;

/**
 * 熔断器拦截器，实现与业务代码中相同的熔断器逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerInterceptor implements StatementServiceInterceptor {

    private final CircuitBreaker circuitBreaker;

    /**
     * 前置处理：执行熔断检查
     */
    @SneakyThrows
    @Override
    public void preProcessExecuteUpdate(CurrentRequestContext<?, ?> context) {
        // 获取请求参数
        StatementRequest request = context.getStatementRequest();
        String stmtHash = SqlStatementXXHash.hashSqlQuery(request.getSql());

        // 执行熔断前置检查
        circuitBreaker.preCheck(stmtHash);

        // 将stmtHash存入上下文供后续使用
        context.setAttribute("stmtHash", stmtHash);
    }

    /**
     * 后置处理：标记成功状态
     */
    @Override
    public void postProcessExecuteUpdate(CurrentRequestContext<?, ?> context) {
        String stmtHash = (String) context.getAttribute("stmtHash");
        if (stmtHash != null) {
            circuitBreaker.onSuccess(stmtHash);
        }
    }

    /**
     * 异常处理：根据异常类型标记失败状态
     */
    @Override
    public void onError(CurrentRequestContext<?, ?> context, Throwable error) {
        // 只处理executeUpdate方法的异常
        if (!"executeUpdate".equals(context.getMethodName()) && !"executeQuery".equals(context.getMethodName())) {
            return;
        }

        String stmtHash = (String) context.getAttribute("stmtHash");
        if (stmtHash == null) {
            log.warn("Missing stmtHash in error handling for executeUpdate");
            return;
        }

        // 根据异常类型处理
        if (error instanceof SQLDataException) {
            circuitBreaker.onFailure(stmtHash, (SQLException) error);
            log.error("SQL data failure during update execution: {}", error.getMessage(), error);
        } else if (error instanceof SQLException) {
            circuitBreaker.onFailure(stmtHash, (SQLException) error);
            log.error("Failure during update execution: {}", error.getMessage(), error);
        } else if (error.getCause() instanceof SQLException) {
            circuitBreaker.onFailure(stmtHash, (SQLException) error.getCause());
            log.error("Wrapped SQL failure during update execution: {}", error.getMessage(), error);
        } else {
            SQLException sqlException = new SQLException("Unexpected error: " + error.getMessage(), error);
            circuitBreaker.onFailure(stmtHash, sqlException);
            log.error("Unexpected failure during update execution: {}", error.getMessage(), error);
        }
    }
}
