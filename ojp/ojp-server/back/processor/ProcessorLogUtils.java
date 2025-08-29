package org.openjdbcproxy.grpc.server.processor;

import lombok.extern.slf4j.Slf4j;

/**
 * 处理器日志工具类
 * 
 * 提供统一的日志记录功能，包括：
 * - 带会话信息的日志记录
 * - 不同级别的日志方法
 * - 统一的日志格式
 */
@Slf4j
public final class ProcessorLogUtils {
    
    private ProcessorLogUtils() {
        // 工具类，禁止实例化
    }
    
    /**
     * 记录信息日志，包含会话信息
     */
    public static void logWithSession(ProcessorContext<?, ?> context, String message, Object... args) {
        String sessionInfo = buildSessionInfo(context);
        log.info(sessionInfo + message, args);
    }
    
    /**
     * 记录调试日志，包含会话信息
     */
    public static void debugWithSession(ProcessorContext<?, ?> context, String message, Object... args) {
        String sessionInfo = buildSessionInfo(context);
        log.debug(sessionInfo + message, args);
    }
    
    /**
     * 记录警告日志，包含会话信息
     */
    public static void warnWithSession(ProcessorContext<?, ?> context, String message, Object... args) {
        String sessionInfo = buildSessionInfo(context);
        log.warn(sessionInfo + message, args);
    }
    
    /**
     * 记录错误日志，包含会话信息
     */
    public static void errorWithSession(ProcessorContext<?, ?> context, String message, Object... args) {
        String sessionInfo = buildSessionInfo(context);
        log.error(sessionInfo + message, args);
    }
    
    /**
     * 记录错误日志，包含异常信息
     */
    public static void errorWithSession(ProcessorContext<?, ?> context, String message, Throwable throwable) {
        String sessionInfo = buildSessionInfo(context);
        log.error(sessionInfo + message, throwable);
    }
    
    /**
     * 记录错误日志，包含异常信息和参数
     */
    public static void errorWithSession(ProcessorContext<?, ?> context, String message, Throwable throwable, Object... args) {
        String sessionInfo = buildSessionInfo(context);
        log.error(sessionInfo + message, args, throwable);
    }
    
    /**
     * 构建会话信息前缀
     */
    private static String buildSessionInfo(ProcessorContext<?, ?> context) {
        if (context == null) {
            return "";
        }
        
        StringBuilder sessionInfo = new StringBuilder();
        
        // 添加会话ID
        if (context.getSessionId() != null) {
            sessionInfo.append("[Session: ").append(context.getSessionId()).append("] ");
        }
        
        // 添加方法类型
        if (context.getMethodType() != null) {
            sessionInfo.append("[Method: ").append(context.getMethodType()).append("] ");
        }
        
        // 添加处理器名称
        if (context.getCurrentProcessor() != null) {
            sessionInfo.append("[Processor: ").append(context.getCurrentProcessor().getName()).append("] ");
        }
        
        return sessionInfo.toString();
    }
    
    /**
     * 记录方法开始日志
     */
    public static void logMethodStart(ProcessorContext<?, ?> context) {
        debugWithSession(context, "Method execution started");
    }
    
    /**
     * 记录方法完成日志
     */
    public static void logMethodComplete(ProcessorContext<?, ?> context, long durationMs) {
        if (context.isSuccess()) {
            debugWithSession(context, "Method execution completed successfully in {}ms", durationMs);
        } else {
            warnWithSession(context, "Method execution completed with warnings in {}ms", durationMs);
        }
    }
    
    /**
     * 记录方法异常日志
     */
    public static void logMethodException(ProcessorContext<?, ?> context, Exception exception) {
        errorWithSession(context, "Method execution failed: {}", exception.getMessage(), exception);
    }
    
    /**
     * 记录查询开始日志
     */
    public static void logQueryStart(ProcessorContext<?, ?> context, String sql) {
        debugWithSession(context, "Query execution started: {}", sql);
    }
    
    /**
     * 记录查询完成日志
     */
    public static void logQueryComplete(ProcessorContext<?, ?> context, long durationMs) {
        if (context.isSuccess()) {
            debugWithSession(context, "Query execution completed in {}ms", durationMs);
        } else {
            warnWithSession(context, "Query execution failed in {}ms", durationMs);
        }
    }
    
    /**
     * 记录事务开始日志
     */
    public static void logTransactionStart(ProcessorContext<?, ?> context) {
        debugWithSession(context, "Transaction started");
    }
    
    /**
     * 记录事务提交日志
     */
    public static void logTransactionCommit(ProcessorContext<?, ?> context, long durationMs) {
        if (context.isSuccess()) {
            debugWithSession(context, "Transaction committed in {}ms", durationMs);
        } else {
            warnWithSession(context, "Transaction commit failed in {}ms", durationMs);
        }
    }
    
    /**
     * 记录事务回滚日志
     */
    public static void logTransactionRollback(ProcessorContext<?, ?> context, long durationMs) {
        if (context.isSuccess()) {
            debugWithSession(context, "Transaction rolled back in {}ms", durationMs);
        } else {
            warnWithSession(context, "Transaction rollback failed in {}ms", durationMs);
        }
    }
}
