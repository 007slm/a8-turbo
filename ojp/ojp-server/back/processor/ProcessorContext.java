package org.openjdbcproxy.grpc.server.processor;

import lombok.Builder;
import lombok.Data;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

/**
 * 处理器上下文
 * 包含请求、响应、异常信息以及自定义属性
 * 
 * @param <REQ> 请求类型
 * @param <RESP> 响应类型
 */
@Data
@Builder
public class ProcessorContext<REQ, RESP> {
    
    /**
     * gRPC 方法类型
     */
    private StatementServiceMethodName methodType;
    
    /**
     * 请求对象
     */
    private REQ request;
    
    /**
     * 响应对象
     */
    private RESP response;
    
    /**
     * gRPC 响应流观察者
     */
    private StreamObserver<?> responseObserver;
    
    /**
     * 异常信息
     */
    private Exception exception;
    
    /**
     * 请求开始时间（毫秒时间戳）
     */
    @Builder.Default
    private long startTime = System.currentTimeMillis();
    
    /**
     * 请求结束时间（毫秒时间戳）
     */
    private long endTime;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executionTimeMs;
    
    /**
     * 会话信息
     */
    private Object sessionInfo;
    
    /**
     * 连接哈希
     */
    private String connectionHash;
    
    /**
     * 客户端UUID
     */
    private String clientUUID;
    
    /**
     * 自定义属性映射
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    
    /**
     * 是否成功
     */
    @Builder.Default
    private boolean success = true;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 设置自定义属性
     * 
     * @param key 属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取自定义属性
     * 
     * @param key 属性键
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 获取自定义属性，如果不存在则返回默认值
     * 
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        T value = (T) attributes.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 移除自定义属性
     * 
     * @param key 属性键
     * @return 被移除的属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(String key) {
        return (T) attributes.remove(key);
    }

    /**
     * 判断是否包含指定属性
     * 
     * @param key 属性键
     * @return 是否包含
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * 设置异常信息并更新成功状态
     * 
     * @param exception 异常
     */
    public void setException(Exception exception) {
        this.exception = exception;
        this.success = false;
        this.errorMessage = exception.getMessage();
    }

    /**
     * 计算并设置执行耗时
     */
    public void calculateExecutionTime() {
        if (startTime > 0) {
            endTime = System.currentTimeMillis();
            executionTimeMs = endTime - startTime;
        }
    }
    
    /**
     * 获取会话ID（从 sessionInfo 中提取）
     */
    public String getSessionId() {
        if (sessionInfo != null) {
            // 检查是否有 getSessionId 方法
            try {
                var sessionIdMethod = sessionInfo.getClass().getMethod("getSessionId");
                Object result = sessionIdMethod.invoke(sessionInfo);
                return result != null ? result.toString() : "unknown";
            } catch (Exception e) {
                // 如果没有 getSessionId 方法，尝试 toString
                return sessionInfo.toString();
            }
        }
        return "unknown";
    }

    /**
     * 获取方法名称
     */
    public String getMethodName() {
        return methodType != null ? methodType.name() : "UNKNOWN";
    }

    /**
     * 创建子上下文（用于嵌套调用）
     */
    public ProcessorContext<REQ, RESP> createSubContext() {
        return ProcessorContext.<REQ, RESP>builder()
                .methodType(this.methodType)
                .request(this.request)
                .sessionInfo(this.sessionInfo)
                .connectionHash(this.connectionHash)
                .clientUUID(this.clientUUID)
                .attributes(new HashMap<>(this.attributes))
                .build();
    }
}
