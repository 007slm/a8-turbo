package org.openjdbcproxy.grpc.server.interceptor;

/**
 * 业务拦截器接口
 * 定义了各种gRPC方法的前置、后置和异常处理方法
 */
public interface StatementServiceInterceptor {

    // -------------------------- 通用拦截点 --------------------------

    /**
     * 所有方法执行前的通用前置处理
     */
    default void preProcess(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * 所有方法执行成功后的通用后置处理
     */
    default void postProcess(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * 所有方法发生异常时的处理
     */
    default void onError(StatementServiceInterceptContext<?, ?> context, Throwable error) {}

    /**
     * 收到客户端流式数据时的处理（适用于所有流式方法）
     */
    default void onClientData(StatementServiceInterceptContext<?, ?> context, Object data) {}

    /**
     * 客户端完成发送请求时触发
     */
    default void onClientComplete(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * 客户端取消请求时触发
     */
    default void onClientCancel(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * 监听器就绪时触发
     */
    default void onReady(StatementServiceInterceptContext<?, ?> context) {}

    // -------------------------- 专用方法拦截点 --------------------------

    /**
     * connect方法前置处理
     */
    default void preProcessConnect(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * connect方法后置处理
     */
    default void postProcessConnect(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * executeUpdate方法前置处理
     */
    default void preProcessExecuteUpdate(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * executeUpdate方法后置处理
     */
    default void postProcessExecuteUpdate(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * executeQuery方法前置处理
     */
    default void preProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * executeQuery方法后置处理
     */
    default void postProcessExecuteQuery(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * fetchNextRows方法前置处理
     */
    default void preProcessFetchNextRows(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * fetchNextRows方法后置处理
     */
    default void postProcessFetchNextRows(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * createLob方法前置处理
     */
    default void preProcessCreateLob(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * createLob方法后置处理
     */
    default void postProcessCreateLob(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * readLob方法前置处理
     */
    default void preProcessReadLob(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * readLob方法后置处理
     */
    default void postProcessReadLob(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * startTransaction方法前置处理
     */
    default void preProcessStartTransaction(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * startTransaction方法后置处理
     */
    default void postProcessStartTransaction(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * commitTransaction方法前置处理
     */
    default void preProcessCommitTransaction(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * commitTransaction方法后置处理
     */
    default void postProcessCommitTransaction(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * rollbackTransaction方法前置处理
     */
    default void preProcessRollbackTransaction(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * rollbackTransaction方法后置处理
     */
    default void postProcessRollbackTransaction(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * terminateSession方法前置处理
     */
    default void preProcessTerminateSession(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * terminateSession方法后置处理
     */
    default void postProcessTerminateSession(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * callResource方法前置处理
     */
    default void preProcessCallResource(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * callResource方法后置处理
     */
    default void postProcessCallResource(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * createStatement方法前置处理
     */
    default void preProcessCreateStatement(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * createStatement方法后置处理
     */
    default void postProcessCreateStatement(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * createPreparedStatement方法前置处理
     */
    default void preProcessCreatePreparedStatement(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * createPreparedStatement方法后置处理
     */
    default void postProcessCreatePreparedStatement(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * setReadOnly方法前置处理
     */
    default void preProcessSetReadOnly(StatementServiceInterceptContext<?, ?> context) {}

    /**
     * setReadOnly方法后置处理
     */
    default void postProcessSetReadOnly(StatementServiceInterceptContext<?, ?> context) {}
}