package org.openjdbcproxy.grpc.server.chain.spi;

import org.openjdbcproxy.grpc.server.chain.SqlProcessor;
import org.openjdbcproxy.grpc.server.chain.config.ProcessorRegistrationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * SQL处理器提供者SPI接口
 * 
 * 这是SPI机制的核心接口，用于：
 * 1. 通过反射实例化处理器，避免直接依赖具体实现类
 * 2. 支持条件化启用处理器
 * 3. 提供处理器优先级和配置信息
 * 4. 实现完全解耦的架构
 * 
 * 每个具体的处理器都需要对应的Provider实现类，
 * 并在META-INF/services中注册以支持ServiceLoader自动发现
 */
public interface SqlProcessorProvider {
    
    /**
     * 获取处理器类名（用于反射实例化）
     * 
     * @return 完整的处理器类名
     */
    String getProcessorClassName();
    
    /**
     * 获取处理器优先级
     * 数字越大优先级越高，高优先级的处理器先执行
     * 
     * @return 优先级值
     */
    int getPriority();
    
    /**
     * 获取处理器名称（用于日志和调试）
     * 
     * @return 处理器名称
     */
    String getName();
    
    /**
     * 判断处理器是否启用
     * 支持基于配置的条件化启用
     * 
     * @param context 处理器注册上下文
     * @return true 如果启用，false 如果禁用
     */
    boolean isEnabled(ProcessorRegistrationContext context);
    
    /**
     * 获取处理器配置
     * 提供处理器所需的配置参数
     * 
     * @param context 处理器注册上下文
     * @return 处理器配置对象
     */
    default ProcessorConfig getConfiguration(ProcessorRegistrationContext context) {
        return ProcessorConfig.empty();
    }
    
    /**
     * 创建处理器实例
     * 使用反射实例化处理器，支持自定义实例化逻辑
     * 
     * @param context 处理器注册上下文
     * @return 处理器实例
     * @throws Exception 实例化过程中的异常
     */
    default SqlProcessor createProcessor(ProcessorRegistrationContext context) throws Exception {
        String className = getProcessorClassName();
        Class<?> processorClass = Class.forName(className);
        
        // 使用反射创建实例
        SqlProcessor processor = (SqlProcessor) processorClass.getDeclaredConstructor().newInstance();
        
        // 如果处理器需要配置，可以在这里进行配置注入
        ProcessorConfig config = getConfiguration(context);
        if (config != null && !config.isEmpty()) {
            configureProcessor(processor, config, context);
        }
        
        return processor;
    }
    
    /**
     * 配置处理器实例
     * 子类可以重写此方法来实现自定义的配置注入逻辑
     * 
     * @param processor 处理器实例
     * @param config 配置对象
     * @param context 注册上下文
     */
    default void configureProcessor(SqlProcessor processor, ProcessorConfig config, ProcessorRegistrationContext context) {
        // 默认实现：通过反射设置字段
        // 子类可以重写来实现更复杂的配置逻辑
    }
    
    /**
     * 处理器配置类
     * 封装处理器所需的配置参数
     */
    class ProcessorConfig {
        private final Map<String, Object> properties = new HashMap<>();
        
        public static ProcessorConfig empty() {
            return new ProcessorConfig();
        }
        
        public static ProcessorConfig of(String key, Object value) {
            ProcessorConfig config = new ProcessorConfig();
            config.properties.put(key, value);
            return config;
        }
        
        public ProcessorConfig put(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }
        
        public Object get(String key) {
            return properties.get(key);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            return (T) properties.get(key);
        }
        
        public boolean isEmpty() {
            return properties.isEmpty();
        }
        
        public Map<String, Object> getProperties() {
            return new HashMap<>(properties);
        }
    }
}