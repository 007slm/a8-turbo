package org.openjdbcproxy.grpc.server.chain.spi.providers;

import org.openjdbcproxy.grpc.server.chain.config.ProcessorRegistrationContext;
import org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider;

/**
 * CRUD操作处理器提供者
 */
public class CrudOperationProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.CrudOperationProcessor";
    }
    
    @Override
    public int getPriority() {
        return 95;
    }
    
    @Override
    public String getName() {
        return "CrudOperationProcessor";
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        // CRUD操作处理器默认启用
        return true;
    }
}