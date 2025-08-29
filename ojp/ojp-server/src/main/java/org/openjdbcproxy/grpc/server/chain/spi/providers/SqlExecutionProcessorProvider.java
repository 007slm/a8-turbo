package org.openjdbcproxy.grpc.server.chain.spi.providers;

import org.openjdbcproxy.grpc.server.chain.config.ProcessorRegistrationContext;
import org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider;

/**
 * SQL执行处理器提供者
 */
public class SqlExecutionProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.SqlExecutionProcessor";
    }
    
    @Override
    public int getPriority() {
        return -100; // 最低优先级，最后执行
    }
    
    @Override
    public String getName() {
        return "SqlExecutionProcessor";
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        return true; // SQL执行处理器必须启用
    }
}