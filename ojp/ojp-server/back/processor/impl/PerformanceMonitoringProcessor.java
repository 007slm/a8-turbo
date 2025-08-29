package org.openjdbcproxy.grpc.server.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.processor.AbstractGrpcMethodProcessor;
import org.openjdbcproxy.grpc.server.processor.StatementServiceMethodName;
import org.openjdbcproxy.grpc.server.processor.ProcessorContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 性能监控处理器
 * 为每个 gRPC 方法提供专门的性能监控逻辑
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ojp.processor.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceMonitoringProcessor extends AbstractGrpcMethodProcessor {

    // 性能阈值常量
    private static final long SLOW_OPERATION_THRESHOLD_MS = 1000; // 1秒
    private static final long SLOW_QUERY_THRESHOLD_MS = 5000; // 5秒

    @Override
    public boolean supports(StatementServiceMethodName methodType) {
        return true; // 支持所有方法类型
    }

    @Override
    public int getOrder() {
        return 200; // 在 SQL 分析处理器(100)之后执行，获取分析结果
    }

    // ========== CONNECT 方法处理 ==========
    
    @Override
    public void preConnect(ProcessorContext<?, ?> context) {
        safeExecute("preConnect", () -> {
            startTiming(context, "connect");
            logWithSession(context, "Starting connection establishment");
        });
    }

    @Override
    public void postConnect(ProcessorContext<?, ?> context) {
        safeExecute("postConnect", () -> {
            long duration = endTiming(context, "connect");
            logWithSession(context, "Connection established successfully in {}ms", duration);
            
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                log.warn("Slow connection detected: {}ms > {}ms threshold", 
                        duration, SLOW_OPERATION_THRESHOLD_MS);
            }
        });
    }

    @Override
    public void onConnectException(ProcessorContext<?, ?> context) {
        safeExecute("onConnectException", () -> {
            long duration = endTiming(context, "connect");
            logWithSession(context, "Connection failed after {}ms: {}", 
                    duration, context.getException().getMessage());
        });
    }

    // ========== EXECUTE_QUERY 方法处理 ==========
    
    @Override
    public void preExecuteQuery(ProcessorContext<?, ?> context) {
        safeExecute("preExecuteQuery", () -> {
            startTiming(context, "query");
            
            // 从 SQL 分析处理器获取 SQL 类型信息
            Object sqlType = context.getAttribute("SqlAnalysis.sqlType");
            String sqlTypeDesc = (String) context.getAttribute("SqlAnalysis.sqlTypeDescription");
            String complexityLevel = (String) context.getAttribute("SqlAnalysis.sqlComplexityLevel");
            
            recordMetric(context, "queryType", sqlType != null ? sqlType.toString() : "UNKNOWN");
            recordMetric(context, "sqlComplexity", complexityLevel != null ? complexityLevel : "UNKNOWN");
            
            if (log.isDebugEnabled()) {
                logWithSession(context, "Starting query execution, SQL type: {}, complexity: {}", 
                    sqlTypeDesc != null ? sqlTypeDesc : "unknown", 
                    complexityLevel != null ? complexityLevel : "unknown");
            }
        });
    }

    @Override
    public void postExecuteQuery(ProcessorContext<?, ?> context) {
        safeExecute("postExecuteQuery", () -> {
            long duration = endTiming(context, "query");
            recordMetric(context, "queryDuration", duration);
            
            // 获取 SQL 分析结果进行智能判断
            String complexityLevel = (String) context.getAttribute("SqlAnalysis.sqlComplexityLevel");
            Integer complexityScore = (Integer) context.getAttribute("SqlAnalysis.sqlComplexityScore");
            Boolean isAnalytical = (Boolean) context.getAttribute("SqlAnalysis.isAnalytical");
            
            // 根据 SQL 复杂度调整慢查询阈值
            long dynamicThreshold = calculateDynamicThreshold(complexityLevel, complexityScore, isAnalytical);
            
            // 检查是否为慢查询
            boolean isSlowQuery = duration > dynamicThreshold;
            recordMetric(context, "isSlowQuery", isSlowQuery);
            recordMetric(context, "dynamicThreshold", dynamicThreshold);
            
            if (isSlowQuery) {
                log.warn("Slow query detected: {}ms > {}ms dynamic threshold (complexity: {}, score: {})", 
                        duration, dynamicThreshold, complexityLevel, complexityScore);
            }
            
            // 计算性能评级
            String performanceRating = calculatePerformanceRating(duration, dynamicThreshold);
            recordMetric(context, "performanceRating", performanceRating);
            
            logWithSession(context, "Query executed successfully in {}ms, rating: {}, complexity: {}", 
                duration, performanceRating, complexityLevel != null ? complexityLevel : "unknown");
        });
    }

    @Override
    public void onExecuteQueryException(ProcessorContext<?, ?> context) {
        safeExecute("onExecuteQueryException", () -> {
            long duration = endTiming(context, "query");
            recordMetric(context, "queryFailed", true);
            
            logWithSession(context, "Query failed after {}ms: {}", 
                    duration, context.getException().getMessage());
        });
    }

    // ========== EXECUTE_UPDATE 方法处理 ==========
    
    @Override
    public void preExecuteUpdate(ProcessorContext<?, ?> context) {
        safeExecute("preExecuteUpdate", () -> {
            startTiming(context, "update");
            recordMetric(context, "operationType", "UPDATE");
            
            if (log.isDebugEnabled()) {
                logWithSession(context, "Starting update execution");
            }
        });
    }

    @Override
    public void postExecuteUpdate(ProcessorContext<?, ?> context) {
        safeExecute("postExecuteUpdate", () -> {
            long duration = endTiming(context, "update");
            recordMetric(context, "updateDuration", duration);
            
            // 检查是否为慢更新
            if (duration > SLOW_QUERY_THRESHOLD_MS) {
                log.warn("Slow update detected: {}ms > {}ms threshold", 
                        duration, SLOW_QUERY_THRESHOLD_MS);
            }
            
            logWithSession(context, "Update executed successfully in {}ms", duration);
        });
    }

    @Override
    public void onExecuteUpdateException(ProcessorContext<?, ?> context) {
        safeExecute("onExecuteUpdateException", () -> {
            long duration = endTiming(context, "update");
            recordMetric(context, "updateFailed", true);
            
            logWithSession(context, "Update failed after {}ms: {}", 
                    duration, context.getException().getMessage());
        });
    }

    // ========== FETCH_NEXT_ROWS 方法处理 ==========
    
    @Override
    public void preFetchNextRows(ProcessorContext<?, ?> context) {
        safeExecute("preFetchNextRows", () -> {
            startTiming(context, "fetch");
            
            if (log.isTraceEnabled()) {
                logWithSession(context, "Starting result set fetch");
            }
        });
    }

    @Override
    public void postFetchNextRows(ProcessorContext<?, ?> context) {
        safeExecute("postFetchNextRows", () -> {
            long duration = endTiming(context, "fetch");
            
            if (log.isTraceEnabled()) {
                logWithSession(context, "Result set fetched in {}ms", duration);
            }
        });
    }

    // ========== TRANSACTION 方法处理 ==========
    
    @Override
    public void preStartTransaction(ProcessorContext<?, ?> context) {
        safeExecute("preStartTransaction", () -> {
            startTiming(context, "transaction_start");
            logWithSession(context, "Starting transaction");
        });
    }

    @Override
    public void postStartTransaction(ProcessorContext<?, ?> context) {
        safeExecute("postStartTransaction", () -> {
            long duration = endTiming(context, "transaction_start");
            logWithSession(context, "Transaction started in {}ms", duration);
        });
    }

    @Override
    public void preCommitTransaction(ProcessorContext<?, ?> context) {
        safeExecute("preCommitTransaction", () -> {
            startTiming(context, "transaction_commit");
            logWithSession(context, "Committing transaction");
        });
    }

    @Override
    public void postCommitTransaction(ProcessorContext<?, ?> context) {
        safeExecute("postCommitTransaction", () -> {
            long duration = endTiming(context, "transaction_commit");
            logWithSession(context, "Transaction committed in {}ms", duration);
        });
    }

    @Override
    public void preRollbackTransaction(ProcessorContext<?, ?> context) {
        safeExecute("preRollbackTransaction", () -> {
            startTiming(context, "transaction_rollback");
            logWithSession(context, "Rolling back transaction");
        });
    }

    @Override
    public void postRollbackTransaction(ProcessorContext<?, ?> context) {
        safeExecute("postRollbackTransaction", () -> {
            long duration = endTiming(context, "transaction_rollback");
            logWithSession(context, "Transaction rolled back in {}ms", duration);
        });
    }

    // ========== LOB 方法处理 ==========
    
    @Override
    public void preCreateLob(ProcessorContext<?, ?> context) {
        safeExecute("preCreateLob", () -> {
            startTiming(context, "lob_create");
            logWithSession(context, "Creating LOB");
        });
    }

    @Override
    public void postCreateLob(ProcessorContext<?, ?> context) {
        safeExecute("postCreateLob", () -> {
            long duration = endTiming(context, "lob_create");
            logWithSession(context, "LOB created in {}ms", duration);
        });
    }

    @Override
    public void preReadLob(ProcessorContext<?, ?> context) {
        safeExecute("preReadLob", () -> {
            startTiming(context, "lob_read");
            logWithSession(context, "Reading LOB");
        });
    }

    @Override
    public void postReadLob(ProcessorContext<?, ?> context) {
        safeExecute("postReadLob", () -> {
            long duration = endTiming(context, "lob_read");
            logWithSession(context, "LOB read in {}ms", duration);
        });
    }

    // ========== SESSION 方法处理 ==========
    
    @Override
    public void preTerminateSession(ProcessorContext<?, ?> context) {
        safeExecute("preTerminateSession", () -> {
            startTiming(context, "session_terminate");
            logWithSession(context, "Terminating session");
        });
    }

    @Override
    public void postTerminateSession(ProcessorContext<?, ?> context) {
        safeExecute("postTerminateSession", () -> {
            long duration = endTiming(context, "session_terminate");
            logWithSession(context, "Session terminated in {}ms", duration);
        });
    }
    
    /**
     * 根据 SQL 复杂度计算动态阈值
     */
    private long calculateDynamicThreshold(String complexityLevel, Integer complexityScore, Boolean isAnalytical) {
        long baseThreshold = SLOW_QUERY_THRESHOLD_MS;
        
        // 根据复杂度等级调整阈值
        if (complexityLevel != null) {
            switch (complexityLevel) {
                case "SIMPLE":
                    baseThreshold = baseThreshold / 2; // 简单查询降低阈值
                    break;
                case "MEDIUM":
                    // 保持默认阈值
                    break;
                case "COMPLEX":
                    baseThreshold = baseThreshold * 2; // 复杂查询提高阈值
                    break;
                case "VERY_COMPLEX":
                    baseThreshold = baseThreshold * 3; // 非常复杂的查询大幅提高阈值
                    break;
            }
        }
        
        // 分析型查询额外调整
        if (Boolean.TRUE.equals(isAnalytical)) {
            baseThreshold = (long) (baseThreshold * 1.5);
        }
        
        // 根据复杂度分数微调
        if (complexityScore != null && complexityScore > 0) {
            double scoreMultiplier = 1.0 + (complexityScore / 100.0); // 每100分增加1倍阈值
            baseThreshold = (long) (baseThreshold * scoreMultiplier);
        }
        
        return baseThreshold;
    }
    
    /**
     * 计算性能评级
     */
    private String calculatePerformanceRating(long duration, long threshold) {
        double ratio = (double) duration / threshold;
        
        if (ratio <= 0.25) {
            return "EXCELLENT";
        } else if (ratio <= 0.5) {
            return "GOOD";
        } else if (ratio <= 0.75) {
            return "FAIR";
        } else if (ratio <= 1.0) {
            return "ACCEPTABLE";
        } else if (ratio <= 2.0) {
            return "SLOW";
        } else {
            return "VERY_SLOW";
        }
    }
}