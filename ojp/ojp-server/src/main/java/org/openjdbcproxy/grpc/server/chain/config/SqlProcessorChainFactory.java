package org.openjdbcproxy.grpc.server.chain.config;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.CircuitBreaker;
import org.openjdbcproxy.grpc.server.ServerConfiguration;
import org.openjdbcproxy.grpc.server.chain.SqlProcessorChain;
import org.openjdbcproxy.grpc.server.smartcache.SmartCacheManager;

import java.util.Map;

/**
 * SQL处理器责任链工厂
 * 
 * 完全基于SPI的配置驱动架构：
 * 1. 使用ProcessorDiscoveryService自动发现处理器
 * 2. 通过反射实例化，完全解耦具体实现
 * 3. 支持灵活的配置和条件化启用
 * 4. 无硬编码依赖，完全可扩展
 * 5. SPI提供者自己负责获取所需依赖
 */
@Slf4j
public class SqlProcessorChainFactory {
    
    private final ProcessorDiscoveryService discoveryService;
    
    public SqlProcessorChainFactory() {
        this.discoveryService = new ProcessorDiscoveryService();
    }
    
    /**
     * 创建SQL处理器责任链 - 纯SPI方式
     * 使用SPI机制自动发现和配置处理器，无需显式传递依赖
     */
    public SqlProcessorChain createProcessorChain(ProcessorRegistrationContext context) {
        log.debug("Creating SQL processor chain using pure SPI discovery...");
        
        // 直接使用SPI发现服务构建责任链
        SqlProcessorChain chain = discoveryService.buildProcessorChain(context);
        
        log.info("Successfully created processor chain with pure SPI configuration");
        return chain;
    }
    
    /**
     * 保留兼容性方法 - 但应该避免使用
     * @deprecated 使用 createProcessorChain(ProcessorRegistrationContext) 代替
     */
    @Deprecated
    public SqlProcessorChain createProcessorChain(
            ServerConfiguration serverConfiguration,
            SmartCacheManager smartCacheManager,
            CircuitBreaker circuitBreaker,
            Object sessionManager,
            Map<String, Object> dataSourceMap) {
        
        log.warn("使用了废弃的createProcessorChain方法，建议使用SPI上下文方式");
        
        // 转换为新的SPI上下文方式
        ProcessorRegistrationContext context = ProcessorRegistrationContext.builder()
                .serverConfiguration(serverConfiguration)
                .smartCacheManager(smartCacheManager)
                .circuitBreaker(circuitBreaker)
                .sessionManager(sessionManager)
                .dataSourceMap(dataSourceMap)
                .build();
        
        return createProcessorChain(context);
    }
    
    /**
     * 获取发现服务（用于诊断和调试）
     */
    public ProcessorDiscoveryService getDiscoveryService() {
        return discoveryService;
    }
}