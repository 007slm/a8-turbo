package org.openjdbcproxy.grpc.server.processor;

/**
 * 处理器异常包装类
 * 用于在最外层统一转换和包装处理器异常
 * 
 * 设计原则：
 * - 保持原始异常的完整信息
 * - 提供统一的异常类型
 * - 支持异常链和根本原因分析
 */
public class ProcessorException extends RuntimeException {
    
    private final String processorName;
    private final String operation;
    private final StatementServiceMethodName methodType;
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原始异常
     * @param processorName 处理器名称
     * @param operation 操作名称
     * @param methodType 方法类型
     */
    public ProcessorException(String message, Throwable cause, String processorName, 
                           String operation, StatementServiceMethodName methodType) {
        super(message, cause);
        this.processorName = processorName;
        this.operation = operation;
        this.methodType = methodType;
    }
    
    /**
     * 构造函数（简化版本）
     * 
     * @param message 异常消息
     * @param cause 原始异常
     * @param processorName 处理器名称
     */
    public ProcessorException(String message, Throwable cause, String processorName) {
        this(message, cause, processorName, "unknown", null);
    }
    
    /**
     * 构造函数（最简化版本）
     * 
     * @param message 异常消息
     * @param cause 原始异常
     */
    public ProcessorException(String message, Throwable cause) {
        this(message, cause, "unknown");
    }
    
    /**
     * 获取处理器名称
     */
    public String getProcessorName() {
        return processorName;
    }
    
    /**
     * 获取操作名称
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * 获取方法类型
     */
    public StatementServiceMethodName getMethodType() {
        return methodType;
    }
    
    /**
     * 创建处理器异常的便捷方法
     * 
     * @param processor 处理器实例
     * @param operation 操作名称
     * @param cause 原始异常
     * @return 包装后的异常
     */
    public static ProcessorException wrap(StatementServiceProcessor processor, String operation, Throwable cause) {
        return new ProcessorException(
            String.format("Processor '%s' failed during '%s': %s", 
                         processor.getName(), operation, cause.getMessage()),
            cause,
            processor.getName(),
            operation,
            null
        );
    }
    
    /**
     * 创建处理器异常的便捷方法（带方法类型）
     * 
     * @param processor 处理器实例
     * @param operation 操作名称
     * @param methodType 方法类型
     * @param cause 原始异常
     * @return 包装后的异常
     */
    public static ProcessorException wrap(StatementServiceProcessor processor, String operation,
                                          StatementServiceMethodName methodType, Throwable cause) {
        return new ProcessorException(
            String.format("Processor '%s' failed during '%s' for method '%s': %s", 
                         processor.getName(), operation, methodType, cause.getMessage()),
            cause,
            processor.getName(),
            operation,
            methodType
        );
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProcessorException{");
        sb.append("processor=").append(processorName);
        if (operation != null && !"unknown".equals(operation)) {
            sb.append(", operation=").append(operation);
        }
        if (methodType != null) {
            sb.append(", methodType=").append(methodType);
        }
        sb.append(", message=").append(getMessage());
        if (getCause() != null) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName())
              .append(": ").append(getCause().getMessage());
        }
        sb.append("}");
        return sb.toString();
    }
}
