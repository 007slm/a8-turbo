package org.openjdbcproxy.grpc.server.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.processor.AbstractGrpcMethodProcessor;
import org.openjdbcproxy.grpc.server.processor.ProcessorContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 全局度量统计处理器
 * 
 * 展示如何使用全局级别的处理方法来实现跨所有 gRPC 方法的统一度量统计
 * 
 * 功能：
 * - 统计所有方法的调用次数
 * - 记录总的执行时间
 * - 统计成功和失败次数
 * - 记录平均响应时间
 */
@Slf4j
@Component
public class GlobalMetricsProcessor extends AbstractGrpcMethodProcessor {
    
    // 全局计数器
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    
    // 度量键常量
    private static final String REQUEST_START_TIME = "request.start.time";
    private static final String REQUEST_COUNT_KEY = "request.count";
    
    @Override
    public int getOrder() {
        return 5; // 较高优先级，早执行
    }
    
    @Override
    public String getName() {
        return "GlobalMetricsProcessor";
    }
    
    // ========== 全局级别处理 ==========
    
    @Override
    public void doGlobalPreProcess(ProcessorContext<?, ?> context) {
        // 记录请求开始时间和计数
        long currentCount = totalRequests.incrementAndGet();
        recordMetric(context, REQUEST_START_TIME, System.currentTimeMillis());
        recordMetric(context, REQUEST_COUNT_KEY, currentCount);
        
        log.debug("Global metrics - Started processing request #{} for method: {}", 
                 currentCount, context.getMethodName());
    }
    
    @Override
    public void doGlobalPostProcess(ProcessorContext<?, ?> context) {
        // 计算执行时间
        Long startTime = getMetric(context, REQUEST_START_TIME, Long.class);
        if (startTime != null) {
            long executionTime = System.currentTimeMillis() - startTime;
            totalExecutionTime.addAndGet(executionTime);
            
            // 记录成功请求
            long successCount = successfulRequests.incrementAndGet();
            
            // 计算平均响应时间
            double avgResponseTime = (double) totalExecutionTime.get() / totalRequests.get();
            
            log.debug("Global metrics - Completed request for method: {}, " +
                     "execution time: {}ms, success count: {}, avg response time: {:.2f}ms", 
                     context.getMethodName(), executionTime, successCount, avgResponseTime);
        }
    }
    
    @Override
    public void doGlobalExceptionProcess(ProcessorContext<?, ?> context) {
        // 记录失败请求
        long failureCount = failedRequests.incrementAndGet();
        
        // 计算执行时间（包括异常情况）
        Long startTime = getMetric(context, REQUEST_START_TIME, Long.class);
        if (startTime != null) {
            long executionTime = System.currentTimeMillis() - startTime;
            totalExecutionTime.addAndGet(executionTime);
        }
        
        log.warn("Global metrics - Request failed for method: {}, " +
                "failure count: {}, error: {}", 
                context.getMethodName(), failureCount, 
                context.getException() != null ? context.getException().getMessage() : "Unknown");
    }
    
    // ========== 公共度量接口 ==========
    
    /**
     * 获取总请求数
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    /**
     * 获取成功请求数
     */
    public long getSuccessfulRequests() {
        return successfulRequests.get();
    }
    
    /**
     * 获取失败请求数
     */
    public long getFailedRequests() {
        return failedRequests.get();
    }
    
    /**
     * 获取平均响应时间（毫秒）
     */
    public double getAverageResponseTime() {
        long total = totalRequests.get();
        return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
    }
    
    /**
     * 获取成功率（百分比）
     */
    public double getSuccessRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) successfulRequests.get() / total * 100 : 0.0;
    }
    
    /**
     * 重置所有度量统计
     */
    public void resetMetrics() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalExecutionTime.set(0);
        log.info("Global metrics have been reset");
    }
    
    /**
     * 输出当前度量统计摘要
     */
    public void logMetricsSummary() {
        log.info("Global Metrics Summary: " +
                "Total Requests: {}, " +
                "Successful: {}, " +
                "Failed: {}, " +
                "Success Rate: {:.2f}%, " +
                "Average Response Time: {:.2f}ms",
                getTotalRequests(),
                getSuccessfulRequests(),
                getFailedRequests(),
                getSuccessRate(),
                getAverageResponseTime());
    }
}
