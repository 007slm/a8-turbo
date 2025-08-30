package org.openjdbcproxy.grpc.server.interceptor.impl;

import com.alibaba.fastjson.JSON;
import com.openjdbcproxy.grpc.SessionInfo;
import com.openjdbcproxy.grpc.StatementRequest;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.SerializationHandler;
import org.openjdbcproxy.grpc.dto.Parameter;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptContext;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptor;
import org.openjdbcproxy.grpc.server.smartcache.parser.SqlParser;
import org.openjdbcproxy.grpc.server.smartcache.rule.QueryContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 慢查询日志拦截器
 * 记录执行时间超过阈值的SQL查询到Redis中
 */
@Slf4j
@Component
public class SlowQueryLoggingInterceptor implements StatementServiceInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final SqlParser sqlParser;
    
    /**
     * 慢查询阈值（毫秒），可通过配置文件ojp.server.interceptors.slow-query-threshold进行配置
     */
    @Value("${ojp.server.interceptors.slow-query-threshold:1000}")
    private long slowQueryThreshold;

    public SlowQueryLoggingInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sqlParser = new SqlParser();
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

    /**
     * 记录慢查询到Redis
     * @param context 拦截器上下文
     * @param executionTime 执行时间
     * @param hasError 是否有错误
     */
    private void logSlowQuery(StatementServiceInterceptContext<?, ?> context, long executionTime, boolean hasError) {
        try {
            String methodName = context.getMethodName();


            StatementRequest request = context.getStatementRequest();
            SessionInfo session = request.getSession();

            // 构建慢查询信息
            Map<String, String> slowQueryInfo = new HashMap<>();
            
            // 基本信息
            slowQueryInfo.put("sql", request.getSql());
            
            // 使用fastjson序列化参数为JSON字符串
            String parametersStr = "[]";
            if (!request.getParameters().isEmpty()) {
                try {
                    List<Parameter> parameters = SerializationHandler.deserialize(
                        request.getParameters().toByteArray(), List.class);
                    parametersStr = parameters != null ? JSON.toJSONString(parameters) : "[]";
                } catch (Exception e) {
                    parametersStr = "JSON serialization failed: " + e.getMessage();
                }
            }
            slowQueryInfo.put("parameters", parametersStr);
            
            slowQueryInfo.put("executionTime", String.valueOf(executionTime));
            slowQueryInfo.put("inTransaction", String.valueOf(session.hasTransactionInfo()));
            slowQueryInfo.put("hasError", String.valueOf(hasError));
            slowQueryInfo.put("timestamp", String.valueOf(Instant.now().toEpochMilli()));
            slowQueryInfo.put("clientUUID", session.getClientUUID());
            slowQueryInfo.put("connHash", session.getConnHash());
            slowQueryInfo.put("methodName", methodName);
            
            // SQL解析信息
            String normalizedSql = sqlParser.normalizeSql(request.getSql());
            slowQueryInfo.put("normalizedSql", normalizedSql);
            
            QueryContext.QueryType queryType = sqlParser.getQueryType(request.getSql());
            slowQueryInfo.put("queryType", queryType.name());
            
            List<String> tableNames = sqlParser.extractTableNames(request.getSql());
            slowQueryInfo.put("tableNames", tableNames.stream().collect(Collectors.joining(",")));

            // 生成唯一的key
            String key = "slow_query:" + System.currentTimeMillis() + ":" + session.getClientUUID();

            // 将慢查询信息存储到Redis中，设置过期时间（例如：24小时）
            stringRedisTemplate.opsForHash().putAll(key, slowQueryInfo);
            stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);

            log.info("Logged slow query to Redis: {} ms, SQL: {}", executionTime, request.getSql());
        } catch (Exception e) {
            log.error("Failed to log slow query to Redis", e);
        }
    }
}