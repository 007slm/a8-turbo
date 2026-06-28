package org.openjdbcproxy.cache.interceptor;

import org.openjdbcproxy.grpc.SessionInfo;
import org.openjdbcproxy.grpc.StatementRequest;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.openjdbcproxy.cache.repository.SlowQueryRepository;
import org.openjdbcproxy.cache.util.JSqlParserUtil;
import org.openjdbcproxy.grpc.SerializationHandler;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptContext;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptor;
import org.openjdbcproxy.grpc.server.utils.QueryType;
import org.openjdbcproxy.grpc.server.utils.SqlParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 慢查询日志拦截器
 * 记录执行时间超过阈值的SQL查询到Redis中
 */
@Slf4j
@Component
public class SlowQueryLoggingInterceptor implements StatementServiceInterceptor {
    private SlowQueryRepository slowQueryRepository;
    private SqlParser sqlParser;
    private final org.openjdbcproxy.cache.service.ConnectionConfigService connectionConfigService;

    /**
     * 慢查询阈值（毫秒），可通过配置文件ojp.server.interceptors.slow-query-threshold进行配置
     */
    @Value("${ojp.server.interceptors.slow-query-threshold:1000}")
    private long slowQueryThreshold;

    @Autowired
    public SlowQueryLoggingInterceptor(SlowQueryRepository slowQueryRepository,
            org.openjdbcproxy.cache.service.ConnectionConfigService connectionConfigService) {
        this.slowQueryRepository = slowQueryRepository;
        this.sqlParser = new SqlParser();
        this.connectionConfigService = connectionConfigService;
    }

    /**
     * executeQuery方法前置处理：记录开始时间
     */
    @Override
    public void preProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {
        // 记录查询开始时间
        context.setAttribute("startTime", System.currentTimeMillis());
    }

    /**
     * executeQuery方法后置处理：检查执行时间并记录慢查询
     */
    @Override
    public void postProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {
        Long startTime = (Long) context.getAttribute("startTime");
        if (startTime != null) {
            long executionTime = System.currentTimeMillis() - startTime;
            // 如果执行时间超过阈值，则记录到Redis
            if (executionTime >= slowQueryThreshold) {
                logSlowQuery(context, executionTime, false);
            }
        }
    }

    /**
     * 异常处理：记录异常的慢查询
     */
    @Override
    public void onError(StatementServiceInterceptContext<?, ?> context, Throwable error) {
        Long startTime = (Long) context.getAttribute("startTime");
        if (startTime != null) {
            long executionTime = System.currentTimeMillis() - startTime;
            // 如果执行时间超过阈值，或者发生异常时，记录到Redis
            // 只处理executeQuery方法
            if ((executionTime >= slowQueryThreshold || error != null) &&
                    "executeQuery".equals(context.getMethodName())) {
                logSlowQuery(context, executionTime, true);
            }
        }
    }

    private void logSlowQuery(StatementServiceInterceptContext<?, ?> context, long executionTime, boolean hasError) {
        String methodName = context.getMethodName();
        StatementRequest request = context.requestToStatementRequest();
        SessionInfo session = request.getSession();

        // 生成唯一的查询ID
        long timestamp = Instant.now().toEpochMilli();
        String slowQueryId = JSqlParserUtil.generateSlowQueryId(session.getConnHash(), request.getSql());

        // 构建慢查询实体
        SlowQuery slowQuery = new SlowQuery();
        slowQuery.setId(slowQueryId);

        // 设置基本信息
        slowQuery.setSql(request.getSql());

        String paramsStr = SerializationHandler.deserializeParams(request.getParameters());
        slowQuery.setParameters(paramsStr);

        slowQuery.setExecutionTime(executionTime);
        slowQuery.setInTransaction(session.hasTransactionInfo());
        slowQuery.setHasError(hasError);
        slowQuery.setTimestamp(timestamp);
        slowQuery.setClientUUID(session.getClientUUID());
        slowQuery.setConnHash(session.getConnHash());
        slowQuery.setMethodName(methodName);

        // 设置SQL解析信息
        QueryType queryType = sqlParser.getQueryType(request.getSql());
        slowQuery.setQueryType(queryType.name());

        List<String> tableNames = sqlParser.extractTableNames(request.getSql());
        slowQuery.setTableNames(tableNames.stream().collect(Collectors.joining(",")));

        // 检查是否已存在该查询的记录，不存在则添加，存在则比较执行时间并更新
        slowQueryRepository.findById(slowQueryId)
                .ifPresentOrElse(existing -> {
                    // 如果当前执行时间更长，则更新记录
                    if (executionTime > existing.getExecutionTime()) {
                        slowQueryRepository.save(slowQuery);
                    }
                }, () -> {
                    // 不存在则新增记录
                    slowQueryRepository.save(slowQuery);
                });

        // 确保连接配置存在
        try {
            connectionConfigService.ensureConnectionConfig(session.getConnHash());
        } catch (Exception e) {
            log.warn("Auto-creation of ConnectionConfig failed", e);
        }
    }
}