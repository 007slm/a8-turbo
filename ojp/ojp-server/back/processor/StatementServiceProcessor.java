package org.openjdbcproxy.grpc.server.processor;

/**
 * gRPC 方法处理器接口
 * 
 * 支持两个级别的处理：
 * 1. 全局级别：适用于所有方法的统一处理（preProcess/postProcess/onException）
 * 2. 方法级别：针对特定 gRPC 方法的专门处理（preXxx/postXxx/onXxxException）
 * 
 * 异常处理原则：
 * - 处理器中的异常应该正常抛出，不要捕获
 * - 中间件层不处理异常，让异常向上传播
 * - 只在最外层做一次统一的异常转换和包装
 */
public interface StatementServiceProcessor {
    
    // ========== 全局级别处理方法 ==========
    
    /**
     * 全局前置处理 - 在执行任何 gRPC 方法之前调用
     */
    default void preProcess(ProcessorContext<?, ?> context) {}
    
    /**
     * 全局后置处理 - 在执行任何 gRPC 方法成功后调用
     */
    default void postProcess(ProcessorContext<?, ?> context) {}
    
    /**
     * 全局异常处理 - 在执行任何 gRPC 方法发生异常时调用
     */
    default void onException(ProcessorContext<?, ?> context) {}
    
    // ========== CONNECT 方法处理 ==========
    
    default void preConnect(ProcessorContext<?, ?> context) {}
    default void postConnect(ProcessorContext<?, ?> context) {}
    default void onConnectException(ProcessorContext<?, ?> context) {}
    
    // ========== EXECUTE_UPDATE 方法处理 ==========
    
    default void preExecuteUpdate(ProcessorContext<?, ?> context) {}
    default void postExecuteUpdate(ProcessorContext<?, ?> context) {}
    default void onExecuteUpdateException(ProcessorContext<?, ?> context) {}
    
    // ========== EXECUTE_QUERY 方法处理 ==========
    
    default void preExecuteQuery(ProcessorContext<?, ?> context) {}
    default void postExecuteQuery(ProcessorContext<?, ?> context) {}
    default void onExecuteQueryException(ProcessorContext<?, ?> context) {}
    
    // ========== FETCH_NEXT_ROWS 方法处理 ==========
    
    default void preFetchNextRows(ProcessorContext<?, ?> context) {}
    default void postFetchNextRows(ProcessorContext<?, ?> context) {}
    default void onFetchNextRowsException(ProcessorContext<?, ?> context) {}
    
    // ========== CREATE_LOB 方法处理 ==========
    
    default void preCreateLob(ProcessorContext<?, ?> context) {}
    default void postCreateLob(ProcessorContext<?, ?> context) {}
    default void onCreateLobException(ProcessorContext<?, ?> context) {}
    
    // ========== READ_LOB 方法处理 ==========
    
    default void preReadLob(ProcessorContext<?, ?> context) {}
    default void postReadLob(ProcessorContext<?, ?> context) {}
    default void onReadLobException(ProcessorContext<?, ?> context) {}
    
    // ========== TERMINATE_SESSION 方法处理 ==========
    
    default void preTerminateSession(ProcessorContext<?, ?> context) {}
    default void postTerminateSession(ProcessorContext<?, ?> context) {}
    default void onTerminateSessionException(ProcessorContext<?, ?> context) {}
    
    // ========== START_TRANSACTION 方法处理 ==========
    
    default void preStartTransaction(ProcessorContext<?, ?> context) {}
    default void postStartTransaction(ProcessorContext<?, ?> context) {}
    default void onStartTransactionException(ProcessorContext<?, ?> context) {}
    
    // ========== COMMIT_TRANSACTION 方法处理 ==========
    
    default void preCommitTransaction(ProcessorContext<?, ?> context) {}
    default void postCommitTransaction(ProcessorContext<?, ?> context) {}
    default void onCommitTransactionException(ProcessorContext<?, ?> context) {}
    
    // ========== ROLLBACK_TRANSACTION 方法处理 ==========
    
    default void preRollbackTransaction(ProcessorContext<?, ?> context) {}
    default void postRollbackTransaction(ProcessorContext<?, ?> context) {}
    default void onRollbackTransactionException(ProcessorContext<?, ?> context) {}
    
    // ========== CALL_RESOURCE 方法处理 ==========
    
    default void preCallResource(ProcessorContext<?, ?> context) {}
    default void postCallResource(ProcessorContext<?, ?> context) {}
    default void onCallResourceException(ProcessorContext<?, ?> context) {}
    
    // ========== 处理器元信息 ==========
    
    /**
     * 判断是否支持处理指定的方法类型
     */
    default boolean supports(StatementServiceMethodName methodType) {
        return true;
    }
    
    /**
     * 获取处理器的执行顺序，数值越小优先级越高
     */
    default int getOrder() {
        return 100;
    }
    
    /**
     * 获取处理器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}