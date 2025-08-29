package org.openjdbcproxy.grpc.server.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 处理器注册表
 * 自动扫描和注册所有的 GrpcMethodProcessor Bean
 * 
 * 使用 Spring 的依赖注入机制，自动发现所有实现了 GrpcMethodProcessor 接口的 Bean
 * 处理器的配置由各自的 Bean 内部管理，不依赖外部配置文件
 */
@Slf4j
@Component
public class ProcessorRegistry {
    
    /**
     * 所有已注册的处理器
     * Spring 会自动注入所有实现了 GrpcMethodProcessor 接口的 Bean
     */
    @Autowired(required = false)
    private List<StatementServiceProcessor> processors = new ArrayList<>();
    
    /**
     * 初始化处理器注册表
     * 在 Spring 完成依赖注入后，对处理器进行排序和日志记录
     */
    @PostConstruct
    public void initialize() {
        if (processors == null || processors.isEmpty()) {
            log.info("No GrpcMethodProcessor beans found. Processor chain will be disabled.");
            processors = new ArrayList<>();
            return;
        }
        
        // 按优先级排序
        processors.sort(Comparator.comparingInt(StatementServiceProcessor::getOrder));
        
        log.info("Initialized ProcessorRegistry with {} processors:", processors.size());
        for (StatementServiceProcessor processor : processors) {
            log.info("  - {} (order: {}, supports: {})", 
                    processor.getName(), 
                    processor.getOrder(), 
                    getSupportedMethods(processor));
        }
    }
    
    /**
     * 获取所有已注册的处理器
     */
    public List<StatementServiceProcessor> getProcessors() {
        return new ArrayList<>(processors);
    }
    
    /**
     * 获取支持指定方法类型的处理器
     */
    public List<StatementServiceProcessor> getProcessorsForMethod(StatementServiceMethodName methodType) {
        return processors.stream()
                .filter(processor -> processor.supports(methodType))
                .sorted(Comparator.comparingInt(StatementServiceProcessor::getOrder))
                .toList();
    }
    
    /**
     * 获取指定名称的处理器
     */
    public StatementServiceProcessor getProcessor(String name) {
        return processors.stream()
                .filter(processor -> processor.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 检查是否有处理器支持指定的方法类型
     */
    public boolean hasProcessorForMethod(StatementServiceMethodName methodType) {
        return processors.stream()
                .anyMatch(processor -> processor.supports(methodType));
    }
    
    /**
     * 获取处理器总数量
     */
    public int getProcessorCount() {
        return processors.size();
    }
    
    /**
     * 获取支持指定方法的处理器数量
     */
    public int getProcessorCount(StatementServiceMethodName methodType) {
        return (int) processors.stream()
                .filter(processor -> processor.supports(methodType))
                .count();
    }
    
    /**
     * 动态注册处理器（用于测试或特殊情况）
     * 注意：这不会影响已经通过 Spring 注入的处理器列表
     */
    public void registerProcessor(StatementServiceProcessor processor) {
        if (processor == null) {
            log.warn("Attempted to register null processor");
            return;
        }
        
        // 检查是否已经存在同名处理器
        boolean exists = processors.stream()
                .anyMatch(p -> p.getName().equals(processor.getName()));
        
        if (exists) {
            log.warn("Processor with name '{}' already exists, skipping registration", 
                    processor.getName());
            return;
        }
        
        processors.add(processor);
        // 重新排序
        processors.sort(Comparator.comparingInt(StatementServiceProcessor::getOrder));
        
        log.info("Dynamically registered processor: {} (order: {})", 
                processor.getName(), processor.getOrder());
    }
    
    /**
     * 动态注销处理器（用于测试或特殊情况）
     */
    public void unregisterProcessor(String name) {
        boolean removed = processors.removeIf(processor -> processor.getName().equals(name));
        if (removed) {
            log.info("Dynamically unregistered processor: {}", name);
        } else {
            log.warn("Processor with name '{}' not found for unregistration", name);
        }
    }
    
    /**
     * 获取处理器支持的方法列表（用于日志记录）
     */
    private String getSupportedMethods(StatementServiceProcessor processor) {
        List<String> supportedMethods = new ArrayList<>();
        
        for (StatementServiceMethodName methodType : StatementServiceMethodName.values()) {
            if (processor.supports(methodType)) {
                supportedMethods.add(methodType.name());
            }
        }
        
        return supportedMethods.isEmpty() ? "NONE" : String.join(", ", supportedMethods);
    }
    
    /**
     * 获取已启用的处理器数量
     */
    private int getEnabledProcessorCount() {
        return processors.size(); // 现在所有注册的处理器都被认为是启用的
    }
    
    /**
     * 获取支持指定方法类型的处理器数量
     */
    private int getEnabledProcessorCount(StatementServiceMethodName methodType) {
        return (int) processors.stream()
                .filter(processor -> processor.supports(methodType))
                .count();
    }
    
    /**
     * 获取处理器注册统计信息
     */
    public RegistryStats getStats() {
        int totalProcessors = processors.size();
        int enabledProcessors = getEnabledProcessorCount();
        
        // 统计每种方法类型的处理器数量
        int[] methodSupport = new int[StatementServiceMethodName.values().length];
        for (int i = 0; i < StatementServiceMethodName.values().length; i++) {
            methodSupport[i] = getEnabledProcessorCount(StatementServiceMethodName.values()[i]);
        }
        
        return new RegistryStats(totalProcessors, enabledProcessors, methodSupport);
    }
    
    /**
     * 处理器注册统计信息
     */
    public static class RegistryStats {
        private final int totalProcessors;
        private final int enabledProcessors;
        private final int[] methodSupport;
        
        public RegistryStats(int totalProcessors, int enabledProcessors, int[] methodSupport) {
            this.totalProcessors = totalProcessors;
            this.enabledProcessors = enabledProcessors;
            this.methodSupport = methodSupport;
        }
        
        public int getTotalProcessors() {
            return totalProcessors;
        }
        
        public int getEnabledProcessors() {
            return enabledProcessors;
        }
        
        public int getMethodSupport(StatementServiceMethodName methodType) {
            return methodSupport[methodType.ordinal()];
        }
        
        @Override
        public String toString() {
            return String.format("ProcessorRegistry Stats: total=%d, enabled=%d", 
                    totalProcessors, enabledProcessors);
        }
    }
}