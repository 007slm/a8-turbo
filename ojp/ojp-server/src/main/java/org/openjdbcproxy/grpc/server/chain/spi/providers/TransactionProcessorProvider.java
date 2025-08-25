package org.openjdbcproxy.grpc.server.chain.spi.providers;

import org.openjdbcproxy.grpc.server.chain.config.ProcessorRegistrationContext;
import org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider;

/**
 * 事务处理器提供者
 */
public class TransactionProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.TransactionProcessor";
    }
    
    @Override
    public int getPriority() {
        return 110;
    }
    
    @Override
    public String getName() {
        return "TransactionProcessor";
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        return true; // 事务处理器始终启用
    }
}








