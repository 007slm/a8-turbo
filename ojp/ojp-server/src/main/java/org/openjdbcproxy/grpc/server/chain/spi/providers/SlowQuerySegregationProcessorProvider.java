package org.openjdbcproxy.grpc.server.chain.spi.providers;

import org.openjdbcproxy.grpc.server.chain.config.ProcessorRegistrationContext;
import org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider;

/**
 * 慢查询隔离处理器提供者
 */
public class SlowQuerySegregationProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.SlowQuerySegregationProcessor";
    }
    
    @Override
    public int getPriority() {
        return 70;
    }
    
    @Override
    public String getName() {
        return "SlowQuerySegregationProcessor";
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        return context.getServerConfiguration() != null &&
               context.getServerConfiguration().isSlowQuerySegregationEnabled();
    }
}