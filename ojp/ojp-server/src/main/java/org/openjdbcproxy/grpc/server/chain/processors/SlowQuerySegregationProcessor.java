package org.openjdbcproxy.grpc.server.chain.processors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;
import org.openjdbcproxy.grpc.server.chain.PostProcessor;
import org.openjdbcproxy.grpc.server.QueryPerformanceMonitor;
import org.openjdbcproxy.grpc.server.SlotManager;

import java.sql.SQLException;
import java.util.Set;

/**
 * 慢查询隔离处理器
 * 
 * 集成SlowQuerySegregationManager功能到责任链中：
 * 1. 性能监控和分类（快查询 vs 慢查询）
 * 2. 资源槽位管理（防止慢查询占用过多资源）
 * 3. 超时控制（快查询和慢查询不同的超时策略）
 * 4. 性能统计和报告
 */
@Slf4j
@Component
public class SlowQuerySegregationProcessor extends AbstractSqlProcessor implements PostProcessor {
    
    private static final String PROCESSOR_NAME = "SlowQuerySegregationProcessor";
    
    private final QueryPerformanceMonitor performanceMonitor;
    private final SlotManager slotManager;
    private final boolean enabled;
    private final long slowSlotTimeoutMs;
    private final long fastSlotTimeoutMs;
    
    /**
     * 构造函数
     * 
     * @param totalSlots 总槽位数（通常来自HikariCP的最大连接池大小）
     * @param slowSlotPercentage 慢查询槽位百分比（0-100）
     * @param idleTimeoutMs 槽位空闲超时时间
     * @param slowSlotTimeoutMs 慢查询槽位获取超时时间
     * @param fastSlotTimeoutMs 快查询槽位获取超时时间
     * @param enabled 是否启用慢查询隔离
     */
    public SlowQuerySegregationProcessor(int totalSlots, int slowSlotPercentage, long idleTimeoutMs,
                                       long slowSlotTimeoutMs, long fastSlotTimeoutMs, boolean enabled) {
        this.enabled = enabled;
        this.slowSlotTimeoutMs = slowSlotTimeoutMs;
        this.fastSlotTimeoutMs = fastSlotTimeoutMs;
        this.performanceMonitor = new QueryPerformanceMonitor();
        
        if (enabled) {
            this.slotManager = new SlotManager(totalSlots, slowSlotPercentage, idleTimeoutMs);
            log.info("SlowQuerySegregationProcessor initialized: enabled={}, totalSlots={}, slowSlotPercentage={}%, idleTimeout={}ms, slowSlotTimeout={}ms, fastSlotTimeout={}ms",
                    enabled, totalSlots, slowSlotPercentage, idleTimeoutMs, slowSlotTimeoutMs, fastSlotTimeoutMs);
        } else {
            this.slotManager = null;
            log.info("SlowQuerySegregationProcessor initialized: enabled={}", enabled);
        }
    }
    
    /**
     * 默认构造函数（禁用状态）
     */
    public SlowQuerySegregationProcessor() {
        this(1, 0, 0, 0, 0, false);
    }
    

    
    /**
     * 前处理：在SQL执行前进行槽位管理
     */
    @Override
    public void preProcess(SqlProcessContext context) throws SQLException {
        if (!enabled) {
            // 如果禁用，仍然进行性能监控但不做槽位管理
            recordPerformanceMetrics(context);
            return;
        }
        
        String operationHash = generateOperationHash(context);
        
        // 判断是否为慢查询
        boolean isSlowOperation = performanceMonitor.isSlowOperation(operationHash);
        
        // 获取适当的槽位
        boolean slotAcquired = false;
        long startTime = System.currentTimeMillis();
        
        try {
            if (isSlowOperation) {
                slotAcquired = slotManager.acquireSlowSlot(slowSlotTimeoutMs);
                if (!slotAcquired) {
                    throw new SQLException("Timeout waiting for slow operation slot for operation: " + operationHash);
                }
                log.debug("Acquired slow slot for operation: {}", operationHash);
                context.setAttribute("slow_query_slot_type", "SLOW");
            } else {
                slotAcquired = slotManager.acquireFastSlot(fastSlotTimeoutMs);
                if (!slotAcquired) {
                    throw new SQLException("Timeout waiting for fast operation slot for operation: " + operationHash);
                }
                log.debug("Acquired fast slot for operation: {}", operationHash);
                context.setAttribute("slow_query_slot_type", "FAST");
            }
            
            // 记录槽位获取信息
            context.setAttribute("slow_query_slot_acquired", true);
            context.setAttribute("slow_query_slot_acquisition_time", System.currentTimeMillis() - startTime);
            context.setAttribute("slow_query_operation_hash", operationHash);
            context.setAttribute("slow_query_is_slow", isSlowOperation);
            
            // 记录性能监控信息
            recordPerformanceMetrics(context);
            
        } catch (Exception e) {
            log.error("Failed to acquire slot for operation: {}", operationHash, e);
            throw new SQLException("Slow query segregation failed: " + e.getMessage(), e);
        } finally {
            // 注册后处理回调来释放槽位
            if (slotAcquired) {
                registerSlotReleaseCallback(context, isSlowOperation);
            }
        }
    }
    
    /**
     * 生成操作哈希（用于性能监控）
     */
    private String generateOperationHash(SqlProcessContext context) {
        // 简化的哈希生成，实际可以更复杂
        String sql = context.getCurrentSql();
        if (sql == null) return "unknown";
        
        // 移除参数值，只保留SQL结构
        String normalizedSql = sql.replaceAll("'[^']*'", "?")
                                 .replaceAll("\\d+", "?")
                                 .replaceAll("\\s+", " ")
                                 .trim();
        
        return String.valueOf(normalizedSql.hashCode());
    }
    
    /**
     * 记录性能监控指标
     */
    private void recordPerformanceMetrics(SqlProcessContext context) {
        String operationHash = context.getAttribute("slow_query_operation_hash", 
                                                  generateOperationHash(context));
        
        // 将性能监控信息添加到上下文
        context.setAttribute("performance_operation_hash", operationHash);
        context.setAttribute("performance_monitor_start_time", System.currentTimeMillis());
        
        // 获取历史性能数据
        double avgTime = performanceMonitor.getOperationAverageTime(operationHash);
        boolean isSlowOp = performanceMonitor.isSlowOperation(operationHash);
        
        context.setAttribute("historical_avg_time", avgTime);
        context.setAttribute("historically_slow", isSlowOp);
        
        if (avgTime > 0) {
            log.debug("Operation {} historical avg time: {:.2f}ms, classified as: {}", 
                     operationHash, avgTime, isSlowOp ? "SLOW" : "FAST");
        }
    }
    
    /**
     * 注册槽位释放回调
     */
    private void registerSlotReleaseCallback(SqlProcessContext context, boolean isSlowOperation) {
        // 在上下文中注册后处理回调
        context.setAttribute("slow_query_cleanup_callback", (Runnable) () -> {
            try {
                if (isSlowOperation) {
                    slotManager.releaseSlowSlot();
                    log.debug("Released slow slot for operation");
                } else {
                    slotManager.releaseFastSlot();
                    log.debug("Released fast slot for operation");
                }
            } catch (Exception e) {
                log.warn("Failed to release slot: {}", e.getMessage());
            }
        });
        
        // 异步性能记录回调
        context.setAttribute("performance_cleanup_callback", (Runnable) () -> {
            recordExecutionTime(context);
        });
    }
    
    /**
     * 记录执行时间（在SQL执行完成后调用）
     */
    private void recordExecutionTime(SqlProcessContext context) {
        try {
            String operationHash = context.getAttribute("performance_operation_hash");
            Long startTime = context.getAttribute("performance_monitor_start_time");
            
            if (operationHash != null && startTime != null) {
                long executionTime = context.getElapsedTime();
                performanceMonitor.recordExecutionTime(operationHash, executionTime);
                
                log.debug("Recorded execution time for operation {}: {}ms", operationHash, executionTime);
                
                // 更新上下文中的性能信息
                context.setAttribute("recorded_execution_time", executionTime);
                context.setAttribute("new_avg_time", performanceMonitor.getOperationAverageTime(operationHash));
            }
        } catch (Exception e) {
            log.warn("Failed to record execution time: {}", e.getMessage());
        }
    }
    
    /**
     * 后处理方法（在SQL执行完成后调用）
     */
    public void postProcess(SqlProcessContext context) {
        // 执行清理回调
        Runnable slotCleanup = context.getAttribute("slow_query_cleanup_callback");
        if (slotCleanup != null) {
            slotCleanup.run();
        }
        
        Runnable performanceCleanup = context.getAttribute("performance_cleanup_callback");
        if (performanceCleanup != null) {
            performanceCleanup.run();
        }
    }
    
    /**
     * 获取当前状态信息
     */
    public SegregationStats getStats() {
        if (!enabled) {
            return SegregationStats.builder()
                    .enabled(false)
                    .trackedOperationCount(0)
                    .totalExecutionCount(0)
                    .overallAverageTime(0.0)
                    .slotManagerStatus("disabled")
                    .build();
        }
        
        return SegregationStats.builder()
                .enabled(true)
                .trackedOperationCount(performanceMonitor.getTrackedOperationCount())
                .totalExecutionCount(performanceMonitor.getTotalExecutionCount())
                .overallAverageTime(performanceMonitor.getOverallAverageExecutionTime())
                .slotManagerStatus(slotManager.getStatus())
                .build();
    }
    
    /**
     * 检查操作是否被分类为慢查询
     */
    public boolean isSlowOperation(String operationHash) {
        return performanceMonitor.isSlowOperation(operationHash);
    }
    
    /**
     * 获取操作的平均执行时间
     */
    public double getOperationAverageTime(String operationHash) {
        return performanceMonitor.getOperationAverageTime(operationHash);
    }
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public int getPriority() {
        return 70; // 在分片之后，缓存之前执行
    }
    

    
    @Override
    public Set<SqlProcessContext.SqlOperationType> getSupportedOperations() {
        return Set.of(SqlProcessContext.SqlOperationType.values()); // 支持所有操作类型
    }
    
    /**
     * 隔离统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class SegregationStats {
        private boolean enabled;
        private int trackedOperationCount;
        private long totalExecutionCount;
        private double overallAverageTime;
        private String slotManagerStatus;
        
        @Override
        public String toString() {
            if (!enabled) {
                return "SlowQuerySegregationProcessor[enabled=false]";
            }
            
            return String.format(
                "SlowQuerySegregationProcessor[enabled=true, trackedOps=%d, totalExecs=%d, overallAvg=%.2fms, slots=%s]",
                trackedOperationCount, totalExecutionCount, overallAverageTime, slotManagerStatus
            );
        }
    }
}