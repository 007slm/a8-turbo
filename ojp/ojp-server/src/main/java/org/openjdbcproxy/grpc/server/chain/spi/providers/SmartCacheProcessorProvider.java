package org.openjdbcproxy.grpc.server.chain.spi.providers;

import org.openjdbcproxy.grpc.server.chain.config.ProcessorRegistrationContext;
import org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider;

/**
 * 智能缓存处理器提供者
 */
public class SmartCacheProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.SmartCacheProcessor";
    }
    
    @Override
    public int getPriority() {
        return 50;
    }
    
    @Override
    public String getName() {
        return "SmartCacheProcessor";
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        return context.getSmartCacheManager() != null &&
               context.getSmartCacheManager().isEnabled();
    }
    
    @Override
    public ProcessorConfig getConfiguration(ProcessorRegistrationContext context) {
        return ProcessorConfig.of("smartCacheManager", context.getSmartCacheManager());
    }
}