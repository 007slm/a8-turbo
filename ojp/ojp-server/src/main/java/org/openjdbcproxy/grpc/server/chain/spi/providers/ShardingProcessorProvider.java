package org.openjdbcproxy.grpc.server.chain.spi.providers;

import org.openjdbcproxy.grpc.server.chain.config.ProcessorRegistrationContext;
import org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider;

/**
 * 分库分表处理器提供者
 */
public class ShardingProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.ShardingProcessor";
    }
    
    @Override
    public int getPriority() {
        return 80;
    }
    
    @Override
    public String getName() {
        return "ShardingProcessor";
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        // 先简单启用，后续可以添加更精细的配置检查
        return true;
    }
}