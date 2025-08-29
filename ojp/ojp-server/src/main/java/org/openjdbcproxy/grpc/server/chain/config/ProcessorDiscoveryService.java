package org.openjdbcproxy.grpc.server.chain.config;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.chain.SqlProcessor;
import org.openjdbcproxy.grpc.server.chain.SqlProcessorChain;
import org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 基于SPI的处理器发现服务
 * 
 * 完全解耦架构：
 * 1. 使用ServiceLoader自动发现SqlProcessorProvider实现
 * 2. 使用反射实例化处理器，无需直接依赖具体类
 * 3. 支持条件化启用和优先级排序
 * 4. 完全移除对具体处理器类的硬编码依赖
 */
@Slf4j
public class ProcessorDiscoveryService {
    
    private final ServiceLoader<SqlProcessorProvider> serviceLoader;
    
    public ProcessorDiscoveryService() {
        this.serviceLoader = ServiceLoader.load(SqlProcessorProvider.class);
    }
    
    /**
     * 构建处理器责任链
     * 使用SPI机制自动发现并实例化处理器
     */
    public SqlProcessorChain buildProcessorChain(ProcessorRegistrationContext context) {
        log.debug("Building SQL processor chain using SPI discovery...");
        
        List<SqlProcessor> processors = new ArrayList<>();
        
        // 使用ServiceLoader加载所有SqlProcessorProvider实现
        for (SqlProcessorProvider provider : serviceLoader) {
            try {
                log.debug("Discovered processor provider: {}", provider.getName());
                
                // 检查处理器是否启用
                if (!provider.isEnabled(context)) {
                    log.debug("Processor {} is disabled, skipping", provider.getName());
                    continue;
                }
                
                // 使用反射实例化处理器
                SqlProcessor processor = provider.createProcessor(context);
                processors.add(processor);
                
                log.info("Successfully loaded processor: {} (priority: {})", 
                        provider.getName(), provider.getPriority());
                        
            } catch (Exception e) {
                log.error("Failed to create processor from provider: {}", 
                         provider.getName(), e);
                // 继续处理其他处理器，而不是完全失败
            }
        }
        
        // 按优先级排序（优先级高的先执行）
        processors.sort((p1, p2) -> {
            int priority1 = getProcessorPriority(p1);
            int priority2 = getProcessorPriority(p2);
            return Integer.compare(priority2, priority1); // 降序排列
        });
        
        log.info("Built processor chain with {} processors", processors.size());
        SqlProcessorChain sqlProcessorChain = new SqlProcessorChain();
        sqlProcessorChain.addProcessor(processors);
        return sqlProcessorChain;
    }
    
    /**
     * 获取处理器优先级
     * 通过SPI提供者获取，避免直接依赖处理器类
     */
    private int getProcessorPriority(SqlProcessor processor) {
        String processorClassName = processor.getClass().getName();
        
        for (SqlProcessorProvider provider : serviceLoader) {
            if (processorClassName.equals(provider.getProcessorClassName())) {
                return provider.getPriority();
            }
        }
        
        return 0; // 默认优先级
    }
    
    /**
     * 获取所有可用的处理器提供者
     * 用于诊断和调试
     */
    public List<SqlProcessorProvider> getAvailableProviders() {
        List<SqlProcessorProvider> providers = new ArrayList<>();
        for (SqlProcessorProvider provider : serviceLoader) {
            providers.add(provider);
        }
        
        // 按优先级排序
        providers.sort(Comparator.comparingInt(SqlProcessorProvider::getPriority).reversed());
        return providers;
    }
    
    /**
     * 刷新ServiceLoader，重新加载SPI实现
     * 用于动态加载新的处理器提供者
     */
    public void refresh() {
        serviceLoader.reload();
        log.info("Refreshed SPI service loader for SqlProcessorProvider");
    }
}