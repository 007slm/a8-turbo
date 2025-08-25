package org.openjdbcproxy.grpc.server.chain.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openjdbcproxy.grpc.server.CircuitBreaker;
import org.openjdbcproxy.grpc.server.ServerConfiguration;
import org.openjdbcproxy.grpc.server.smartcache.SmartCacheManager;
import org.openjdbcproxy.grpc.server.smartcache.interceptor.SmartCacheMetrics;

import java.util.HashMap;
import java.util.Map;

/**
 * 处理器注册上下文
 * 
 * 用于SPI处理器的配置和实例化：
 * 1. 提供所有必要的依赖组件
 * 2. 支持条件化配置
 * 3. 为反射实例化提供参数
 * 4. 支持从服务实例获取依赖（完全解耦）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessorRegistrationContext {
    
    /**
     * 服务器配置
     */
    private ServerConfiguration serverConfiguration;
    
    /**
     * 智能缓存管理器
     */
    private SmartCacheManager smartCacheManager;
    
    /**
     * 熔断器
     */
    private CircuitBreaker circuitBreaker;
    
    /**
     * 会话管理器 - 使用Object类型避免直接依赖
     */
    private Object sessionManager;
    
    /**
     * 数据源映射
     */
    @Builder.Default
    private Map<String, Object> dataSourceMap = new HashMap<>();
    
    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    
    /**
     * 获取会话管理器（类型安全的方式）
     */
    @SuppressWarnings("unchecked")
    public <T> T getSessionManager() {
        return (T) sessionManager;
    }
    
    /**
     * 从服务实例中获取指定字段的值（通过反射）
     */
    @SuppressWarnings("unchecked")
    public <T> T getDependencyFromServiceInstance(String fieldName) {
        Object serviceInstance = getAttribute("statementServiceInstance");
        if (serviceInstance != null) {
            try {
                java.lang.reflect.Field field = serviceInstance.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return (T) field.get(serviceInstance);
            } catch (Exception e) {
                // 静默失败，返回null
                return null;
            }
        }
        return null;
    }
    
    /**
     * 获取CircuitBreaker依赖
     */
    public Object getCircuitBreaker() {
        // 优先从直接字段获取，然后从服务实例反射获取
        if (circuitBreaker != null) {
            return circuitBreaker;
        }
        return getDependencyFromServiceInstance("circuitBreaker");
    }

    /**
     * 获取SmartCacheManager依赖
     */
    public SmartCacheManager getSmartCacheManager() {
        // 优先从直接字段获取，然后从服务实例反射获取
        if (smartCacheManager != null) {
            return smartCacheManager;
        }
        return getDependencyFromServiceInstance("smartCacheManager");
    }

    /**
     * 设置扩展属性
     */
    public ProcessorRegistrationContext setAttribute(String key, Object value) {
        this.attributes.put(key, value);
        return this; // 支持链式调用
    }
    
    /**
     * 获取扩展属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
    
    /**
     * 检查属性是否存在
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
}