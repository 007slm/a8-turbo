package org.openjdbcproxy.grpc.server.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.processor.ProcessorContext;
import org.openjdbcproxy.grpc.server.processor.StatementServiceProcessor;

/**
 * 性能监控处理器
 * 
 * 负责记录各种性能指标和计时信息，包括：
 * - 方法执行时间
 * - 成功率统计
 * - 自定义业务指标
 * - 会话级别的性能数据
 */
@Slf4j
public class MetricsProcessor implements StatementServiceProcessor {
    
    @Override
    public String getName() {
        return "MetricsProcessor";
    }
    
    @Override
    public int getOrder() {
        return 100; // 高优先级，确保在其他处理器之前执行
    }
    
    @Override
    public void preProcess(ProcessorContext<?, ?> context) {
        // 记录请求开始时间
        context.setAttribute("request.startTime", System.currentTimeMillis());

        log.debug("Metrics collection started for method: {}", context.getMethodType());
    }
    
    @Override
    public void postProcess(ProcessorContext<?, ?> context) {
        // 计算总执行时间
        Long startTime = context.getAttribute("request.startTime", 0L);
        if (startTime != null && startTime > 0) {
            long totalDuration = System.currentTimeMillis() - startTime;
            context.setAttribute("request.totalDuration", totalDuration);
            
            // 记录总执行时间
            log.info("Method {} completed in {}ms", context.getMethodType(), totalDuration);
        }
        
        // 记录成功率
        boolean success = context.isSuccess();
        context.setAttribute("request.success", success);
        
        if (success) {
            log.debug("Method {} executed successfully", context.getMethodType());
        } else {
            log.warn("Method {} execution failed", context.getMethodType());
        }
    }
    
    @Override
    public void onException(ProcessorContext<?, ?> context) {
        // 记录异常信息
        Exception exception = context.getException();
        if (exception != null) {
            context.setAttribute("request.error", exception.getClass().getSimpleName());
            context.setAttribute("request.errorMessage", exception.getMessage());
            
            log.error("Method {} failed with error: {}", 
                     context.getMethodType(), exception.getMessage());
        }
    }
    
    // ========== 方法级别监控 ==========
    
    @Override
    public void preExecuteQuery(ProcessorContext<?, ?> context) {
        startTiming(context, "executeQuery");
        log.debug("Query execution started");
    }
    
    @Override
    public void postExecuteQuery(ProcessorContext<?, ?> context) {
        long duration = endTiming(context, "executeQuery");
        recordMetric(context, "query.duration", duration);
        recordMetric(context, "query.success", context.isSuccess());
        
        log.debug("Query execution completed in {}ms", duration);
    }
    
    @Override
    public void preExecuteUpdate(ProcessorContext<?, ?> context) {
        startTiming(context, "executeUpdate");
        log.debug("Update execution started");
    }
    
    @Override
    public void postExecuteUpdate(ProcessorContext<?, ?> context) {
        long duration = endTiming(context, "executeUpdate");
        recordMetric(context, "update.duration", duration);
        recordMetric(context, "update.success", context.isSuccess());
        
        log.debug("Update execution completed in {}ms", duration);
    }
    
    @Override
    public void preStartTransaction(ProcessorContext<?, ?> context) {
        startTiming(context, "startTransaction");
        log.debug("Transaction start initiated");
    }
    
    @Override
    public void postStartTransaction(ProcessorContext<?, ?> context) {
        long duration = endTiming(context, "startTransaction");
        recordMetric(context, "transaction.start.duration", duration);
        recordMetric(context, "transaction.start.success", context.isSuccess());
        
        log.debug("Transaction started in {}ms", duration);
    }
    
    @Override
    public void preCommitTransaction(ProcessorContext<?, ?> context) {
        startTiming(context, "commitTransaction");
        log.debug("Transaction commit initiated");
    }
    
    @Override
    public void postCommitTransaction(ProcessorContext<?, ?> context) {
        long duration = endTiming(context, "commitTransaction");
        recordMetric(context, "transaction.commit.duration", duration);
        recordMetric(context, "transaction.commit.success", context.isSuccess());
        
        log.debug("Transaction committed in {}ms", duration);
    }
    
    @Override
    public void preRollbackTransaction(ProcessorContext<?, ?> context) {
        startTiming(context, "rollbackTransaction");
        log.debug("Transaction rollback initiated");
    }
    
    @Override
    public void postRollbackTransaction(ProcessorContext<?, ?> context) {
        long duration = endTiming(context, "rollbackTransaction");
        recordMetric(context, "transaction.rollback.duration", duration);
        recordMetric(context, "transaction.rollback.success", context.isSuccess());
        
        log.debug("Transaction rolled back in {}ms", duration);
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 开始计时
     */
    private void startTiming(ProcessorContext<?, ?> context, String operation) {
        String key = operation + ".startTime";
        context.setAttribute(key, System.currentTimeMillis());
    }
    
    /**
     * 结束计时并返回耗时
     */
    private long endTiming(ProcessorContext<?, ?> context, String operation) {
        String startKey = operation + ".startTime";
        Long startTime = context.getAttribute(startKey, 0L);
        
        if (startTime != null && startTime > 0) {
            long duration = System.currentTimeMillis() - startTime;
            context.setAttribute(operation + ".duration", duration);
            return duration;
        }
        
        return 0;
    }
    
    /**
     * 记录性能指标
     */
    private void recordMetric(ProcessorContext<?, ?> context, String metricName, Object value) {
        String key = "metrics." + metricName;
        context.setAttribute(key, value);
    }
}
