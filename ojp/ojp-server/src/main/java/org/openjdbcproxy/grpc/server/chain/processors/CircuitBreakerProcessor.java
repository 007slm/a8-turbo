package org.openjdbcproxy.grpc.server.chain.processors;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.chain.AbstractSqlProcessor;
import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;
import org.openjdbcproxy.grpc.server.CircuitBreaker;
import org.openjdbcproxy.grpc.server.SqlStatementXXHash;

import java.sql.SQLException;
import java.util.Set;

/**
 * 熔断器处理器
 * 
 * 提供SQL执行的熔断保护功能：
 * 1. 执行前检查熔断状态
 * 2. 记录执行成功/失败状态
 * 3. 自动熔断频繁失败的SQL语句
 * 4. 支持熔断器状态监控和统计
 */
@Slf4j
public class CircuitBreakerProcessor extends AbstractSqlProcessor {
    
    private static final String PROCESSOR_NAME = "CircuitBreakerProcessor";
    
    private final CircuitBreaker circuitBreaker;
    private final boolean enabled;
    
    /**
     * 构造函数
     * 
     * @param circuitBreaker 熔断器实例
     * @param enabled 是否启用熔断功能
     */
    public CircuitBreakerProcessor(CircuitBreaker circuitBreaker, boolean enabled) {
        this.circuitBreaker = circuitBreaker;
        this.enabled = enabled;
        
        log.info("CircuitBreakerProcessor initialized: enabled={}", enabled);
    }
    
    /**
     * 默认构造函数（禁用状态）
     */
    public CircuitBreakerProcessor() {
        this(null, false);
    }
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        if (!enabled || circuitBreaker == null) {
            // 禁用状态，直接传递给下一个处理器
            return false;
        }
        
        String sql = context.getCurrentSql();
        String stmtHash = SqlStatementXXHash.hashSqlQuery(sql);
        
        try {
            // 执行前检查熔断状态
            circuitBreaker.preCheck(stmtHash);
            
            // 记录熔断器信息到上下文
            context.setAttribute("circuit_breaker_hash", stmtHash);
            context.setAttribute("circuit_breaker_checked", true);
            
            log.debug("Circuit breaker pre-check passed for SQL hash: {}", stmtHash);
            
            // 注册后处理回调，用于记录执行结果
            registerCircuitBreakerCallback(context, stmtHash);
            
            return false; // 继续传递给下一个处理器
            
        } catch (SQLException e) {
            // 熔断器阻止执行
            log.warn("Circuit breaker blocked SQL execution: hash={}, reason={}", stmtHash, e.getMessage());
            
            // 记录熔断信息
            context.setAttribute("circuit_breaker_blocked", true);
            context.setAttribute("circuit_breaker_block_reason", e.getMessage());
            
            // 抛出异常，阻止后续处理器执行
            throw e;
        }
    }
    
    /**
     * 注册熔断器回调，用于处理执行结果
     */
    private void registerCircuitBreakerCallback(SqlProcessContext context, String stmtHash) {
        // 成功回调
        context.setAttribute("circuit_breaker_success_callback", (Runnable) () -> {
            try {
                circuitBreaker.onSuccess(stmtHash);
                log.debug("Circuit breaker recorded success for SQL hash: {}", stmtHash);
            } catch (Exception e) {
                log.warn("Failed to record circuit breaker success: {}", e.getMessage());
            }
        });
        
        // 失败回调
        context.setAttribute("circuit_breaker_failure_callback", 
            (java.util.function.Consumer<SQLException>) (SQLException exception) -> {
                try {
                    circuitBreaker.onFailure(stmtHash, exception);
                    log.debug("Circuit breaker recorded failure for SQL hash: {}, error: {}", stmtHash, exception.getMessage());
                } catch (Exception e) {
                    log.warn("Failed to record circuit breaker failure: {}", e.getMessage());
                }
            });
    }
    
    /**
     * 后处理方法（在SQL执行完成后调用）
     */
    public void postProcess(SqlProcessContext context) {
        if (!enabled || !context.hasAttribute("circuit_breaker_checked")) {
            return;
        }
        
        try {
            // 检查是否有异常
            if (context.getError() != null) {
                // 执行失败，调用失败回调
                @SuppressWarnings("unchecked")
                java.util.function.Consumer<SQLException> failureCallback = 
                    (java.util.function.Consumer<SQLException>) context.getAttribute("circuit_breaker_failure_callback");
                
                if (failureCallback != null && context.getError() instanceof SQLException) {
                    failureCallback.accept((SQLException) context.getError());
                }
            } else {
                // 执行成功，调用成功回调
                Runnable successCallback = context.getAttribute("circuit_breaker_success_callback");
                if (successCallback != null) {
                    successCallback.run();
                }
            }
        } catch (Exception e) {
            log.warn("Circuit breaker post-processing failed: {}", e.getMessage());
        }
    }
    
    /**
     * 获取熔断器统计信息
     */
    public CircuitBreakerStats getStats() {
        if (!enabled || circuitBreaker == null) {
            return CircuitBreakerStats.builder()
                    .enabled(false)
                    .totalCircuits(0)
                    .openCircuits(0)
                    .build();
        }
        
        // 使用反射或公开方法获取熔断器状态
        // 这里简化实现，实际可能需要增强CircuitBreaker类
        return CircuitBreakerStats.builder()
                .enabled(true)
                .totalCircuits(getCircuitCount())
                .openCircuits(getOpenCircuitCount())
                .build();
    }
    
    /**
     * 获取熔断器电路总数（简化实现）
     */
    private int getCircuitCount() {
        try {
            // 简化实现，在实际应用中可以增强CircuitBreaker提供公开的状态接口
            return 0; // 占位实现
        } catch (Exception e) {
            log.debug("Unable to get circuit count: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取打开的熔断器数量（简化实现）
     */
    private int getOpenCircuitCount() {
        try {
            // 简化实现，不使用反射访问内部类型
            // 在实际应用中，可以增强CircuitBreaker类提供公开的状态接口
            return 0; // 占位实现
        } catch (Exception e) {
            log.debug("Unable to get open circuit count: {}", e.getMessage());
            return 0;
        }
    }
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public int getPriority() {
        return 120; // 最高优先级，在所有其他处理器之前执行
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public Set<SqlProcessContext.SqlOperationType> getSupportedOperations() {
        return Set.of(SqlProcessContext.SqlOperationType.values()); // 支持所有操作类型
    }
    
    /**
     * 熔断器统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class CircuitBreakerStats {
        private boolean enabled;
        private int totalCircuits;
        private int openCircuits;
        
        @Override
        public String toString() {
            if (!enabled) {
                return "CircuitBreakerProcessor[enabled=false]";
            }
            
            return String.format(
                "CircuitBreakerProcessor[enabled=true, totalCircuits=%d, openCircuits=%d]",
                totalCircuits, openCircuits
            );
        }
    }
    
    /**
     * 函数式接口，用于失败回调
     */
    @FunctionalInterface
    public interface FailureCallback {
        void onFailure(SQLException exception);
    }
}