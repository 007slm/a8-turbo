package org.openjdbcproxy.grpc.server.chain.spi.providers;

import org.openjdbcproxy.grpc.server.chain.config.ProcessorRegistrationContext;
import org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider;

/**
 * 数据权限处理器提供者
 */
public class DataPermissionProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.DataPermissionProcessor";
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String getName() {
        return "DataPermissionProcessor";
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        // 先简单启用，后续可以添加更精细的配置检查
        return true;
    }
}