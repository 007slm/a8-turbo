package org.openjdbcproxy.grpc.server.chain.spi.providers;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.chain.config.ProcessorRegistrationContext;
import org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider;

/**
 * 熔断器处理器提供者
 * 使用反射实例化，避免直接依赖具体实现类
 */
@Slf4j
public class CircuitBreakerProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.CircuitBreakerProcessor";
    }
    
    @Override
    public int getPriority() {
        return 120; // 最高优先级
    }
    
    @Override
    public String getName() {
        return "CircuitBreakerProcessor";
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        Object circuitBreaker = context.getCircuitBreaker();
        // 根据是否有CircuitBreaker实例和配置来决定是否启用
        return circuitBreaker != null &&
               (context.getServerConfiguration() == null || 
                context.getServerConfiguration().getCircuitBreakerTimeout() > 0);
    }
    
    @Override
    public ProcessorConfig getConfiguration(ProcessorRegistrationContext context) {
        Object circuitBreaker = context.getCircuitBreaker();
        if (circuitBreaker != null) {
            return ProcessorConfig.of("circuitBreaker", circuitBreaker)
                                 .put("enabled", true);
        }
        return ProcessorConfig.empty();
    }
}