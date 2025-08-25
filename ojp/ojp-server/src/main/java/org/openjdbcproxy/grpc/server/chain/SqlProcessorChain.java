package org.openjdbcproxy.grpc.server.chain;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.chain.processors.DataPermissionProcessor;
import org.openjdbcproxy.grpc.server.chain.processors.ShardingProcessor;
import org.openjdbcproxy.grpc.server.chain.processors.SmartCacheProcessor;
import org.openjdbcproxy.grpc.server.chain.processors.SqlExecutionProcessor;
import org.openjdbcproxy.grpc.server.chain.processors.TransactionProcessor;
import org.openjdbcproxy.grpc.server.chain.processors.CrudOperationProcessor;
import org.openjdbcproxy.grpc.server.chain.processors.SlowQuerySegregationProcessor;
import org.openjdbcproxy.grpc.server.chain.processors.CircuitBreakerProcessor;
import org.openjdbcproxy.grpc.server.smartcache.SmartCacheManager;
import org.openjdbcproxy.grpc.server.CircuitBreaker;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SQL处理责任链管理器
 * 
 * 负责：
 * 1. 组装和管理SQL处理责任链
 * 2. 根据优先级排序处理器
 * 3. 执行完整的处理流程
 * 4. 处理异常和回滚
 */
@Slf4j
public class SqlProcessorChain {
    
    private final List<SqlProcessor> processors = new ArrayList<>();
    private SqlProcessor chainHead;


    /**
     * 添加处理器到责任链
     */
    public SqlProcessorChain addProcessor(SqlProcessor processor) {
        if (processor != null && processor.isEnabled()) {
            processors.add(processor);
            log.debug("Added processor: {} with priority: {}", 
                     processor.getProcessorName(), processor.getPriority());
        }
        return this;
    }

    public SqlProcessorChain addProcessor(List<SqlProcessor> processors) {
        processors.addAll( processors);
        return this;
    }
    
    /**
     * 构建责任链（根据优先级排序）
     */
    public void buildChain() {
        if (processors.isEmpty()) {
            log.warn("No processors configured in the chain");
            return;
        }
        
        // 按优先级降序排序（优先级高的先执行）
        processors.sort(Comparator.comparingInt(SqlProcessor::getPriority).reversed());
        
        // 构建责任链
        for (int i = 0; i < processors.size() - 1; i++) {
            processors.get(i).setNext(processors.get(i + 1));
        }
        
        chainHead = processors.get(0);
        
        log.info("SQL processor chain built with {} processors: {}", 
                processors.size(), 
                processors.stream()
                    .map(p -> p.getProcessorName() + "(" + p.getPriority() + ")")
                    .reduce((a, b) -> a + " -> " + b)
                    .orElse("empty"));
    }
    
    /**
     * 执行SQL处理责任链
     */
    public boolean process(SqlProcessContext context) throws SQLException {
        if (chainHead == null) {
            log.warn("Chain is not built or empty, skipping processing");
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Starting SQL processing chain for: {}", context.getCurrentSql());
            
            boolean handled = chainHead.process(context);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("SQL processing chain completed in {}ms, handled: {}", processingTime, handled);
            
            return handled;
            
        } catch (SQLException e) {
            log.error("SQL processing chain failed: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in SQL processing chain: {}", e.getMessage(), e);
            throw new SQLException("SQL processing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行后处理操作（在SQL执行完成后调用）
     */
    public void postProcess(SqlProcessContext context) {
        for (SqlProcessor processor : processors) {
            try {
                // 检查处理器是否支持后处理
                if (processor instanceof SlowQuerySegregationProcessor) {
                    ((SlowQuerySegregationProcessor) processor).postProcess(context);
                } else if (processor instanceof SmartCacheProcessor) {
                    ((SmartCacheProcessor) processor).postProcess(context, context.getResult());
                } else if (processor instanceof CircuitBreakerProcessor) {
                    ((CircuitBreakerProcessor) processor).postProcess(context);
                }
                // 可以继续添加其他处理器的后处理逻辑
            } catch (Exception e) {
                log.warn("Post-processing failed for processor {}: {}", 
                        processor.getProcessorName(), e.getMessage());
            }
        }
    }
    
    /**
     * 获取责任链中的处理器列表
     */
    public List<SqlProcessor> getProcessors() {
        return new ArrayList<>(processors);
    }
    
    /**
     * 根据名称查找处理器
     */
    public SqlProcessor findProcessor(String processorName) {
        return processors.stream()
                .filter(p -> p.getProcessorName().equals(processorName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 检查责任链是否包含指定类型的处理器
     */
    public boolean hasProcessor(Class<? extends SqlProcessor> processorClass) {
        return processors.stream()
                .anyMatch(p -> processorClass.isInstance(p));
    }
    
    /**
     * 获取责任链统计信息
     */
    public ChainStatistics getStatistics() {
        ChainStatistics stats = new ChainStatistics();
        stats.totalProcessors = processors.size();
        stats.enabledProcessors = (int) processors.stream().mapToLong(p -> p.isEnabled() ? 1 : 0).sum();
        
        return stats;
    }
    
    /**
     * 责任链统计信息
     */
    public static class ChainStatistics {
        public int totalProcessors;
        public int enabledProcessors;
        
        @Override
        public String toString() {
            return String.format("Chain Statistics: %d total, %d enabled", 
                               totalProcessors, enabledProcessors);
        }
    }
    
    /**
     * 创建默认的SQL处理责任链
     */
    public static SqlProcessorChain createDefaultChain(SmartCacheManager cacheManager) {
        SqlProcessorChain chain = new SqlProcessorChain();
        
        // 按优先级添加处理器
        // 注意：默认链不包含熔断器，需要在具体使用时添加
        chain.addProcessor(new TransactionProcessor())       // 优先级: 110 (最高)
             .addProcessor(new DataPermissionProcessor())    // 优先级: 100
             .addProcessor(new CrudOperationProcessor())     // 优先级: 95
             .addProcessor(new ShardingProcessor())           // 优先级: 80
             .addProcessor(new SlowQuerySegregationProcessor()) // 优先级: 70
             .addProcessor(new SmartCacheProcessor(cacheManager)) // 优先级: 50
             .addProcessor(new SqlExecutionProcessor());      // 优先级: -100 (最后执行)
        
        chain.buildChain();
        return chain;
    }
    
    /**
     * 创建自定义责任链构建器
     */
    public static ChainBuilder builder() {
        return new ChainBuilder();
    }
    
    /**
     * 责任链构建器
     */
    public static class ChainBuilder {
        private final SqlProcessorChain chain = new SqlProcessorChain();
        
        public ChainBuilder addCircuitBreaker(CircuitBreaker circuitBreaker, boolean enabled) {
            chain.addProcessor(new CircuitBreakerProcessor(circuitBreaker, enabled));
            return this;
        }
        
        public ChainBuilder addCircuitBreaker(CircuitBreaker circuitBreaker) {
            chain.addProcessor(new CircuitBreakerProcessor(circuitBreaker, true));
            return this;
        }
        
        public ChainBuilder addTransaction() {
            chain.addProcessor(new TransactionProcessor());
            return this;
        }
        
        public ChainBuilder addCrudOperation() {
            chain.addProcessor(new CrudOperationProcessor());
            return this;
        }
        
        public ChainBuilder addDataPermission() {
            chain.addProcessor(new DataPermissionProcessor());
            return this;
        }
        
        public ChainBuilder addSlowQuerySegregation() {
            chain.addProcessor(new SlowQuerySegregationProcessor());
            return this;
        }
        
        public ChainBuilder addSlowQuerySegregation(int totalSlots, int slowSlotPercentage, 
                                                   long idleTimeoutMs, long slowSlotTimeoutMs, 
                                                   long fastSlotTimeoutMs, boolean enabled) {
            chain.addProcessor(new SlowQuerySegregationProcessor(
                totalSlots, slowSlotPercentage, idleTimeoutMs, 
                slowSlotTimeoutMs, fastSlotTimeoutMs, enabled));
            return this;
        }
        
        public ChainBuilder addSharding() {
            chain.addProcessor(new ShardingProcessor());
            return this;
        }
        
        public ChainBuilder addSmartCache(SmartCacheManager cacheManager) {
            if (cacheManager != null) {
                chain.addProcessor(new SmartCacheProcessor(cacheManager));
            }
            return this;
        }
        
        public ChainBuilder addSqlExecution() {
            chain.addProcessor(new SqlExecutionProcessor());
            return this;
        }
        
        public ChainBuilder addCustomProcessor(SqlProcessor processor) {
            chain.addProcessor(processor);
            return this;
        }
        
        public SqlProcessorChain build() {
            chain.buildChain();
            return chain;
        }
    }
}