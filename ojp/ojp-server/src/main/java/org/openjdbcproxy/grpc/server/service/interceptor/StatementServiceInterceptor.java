package org.openjdbcproxy.grpc.server.service.interceptor;

import com.openjdbcproxy.grpc.*;
import io.grpc.stub.StreamObserver;
import org.openjdbcproxy.grpc.server.service.interceptor.context.CurrentRequestContext;

/**
 * StatementService专用拦截器接口
 * 包含通用拦截点和所有方法的专用拦截点
 */
public interface StatementServiceInterceptor {

    // -------------------------- 通用拦截点 --------------------------

    /**
     * 所有方法执行前的通用前置处理
     */
    default void preProcess(CurrentRequestContext<?, ?> context) {}

    /**
     * 所有方法执行成功后的通用后置处理
     */
    default void postProcess(CurrentRequestContext<?, ?> context) {}

    /**
     * 所有方法发生异常时的处理
     */
    default void onError(CurrentRequestContext<?, ?> context, Throwable error) {}

    /**
     * 收到客户端流式数据时的处理（适用于所有流式方法）
     */
    default void onClientData(CurrentRequestContext<?, ?> context, Object data) {}

    /**
     * 客户端完成发送请求时触发
     */
    default void onClientComplete(CurrentRequestContext<?, ?> context) {}

    /**
     * 客户端取消请求时触发
     */
    default void onClientCancel(CurrentRequestContext<?, ?> context) {}

    /**
     * 监听器就绪时触发
     */
    default void onReady(CurrentRequestContext<?, ?> context) {}

    // -------------------------- 专用方法拦截点 --------------------------

    /**
     * connect方法前置处理
     */
    default void preProcessConnect(CurrentRequestContext<ConnectionDetails, StreamObserver<SessionInfo>> context) {}

    /**
     * connect方法后置处理
     */
    default void postProcessConnect(CurrentRequestContext<ConnectionDetails, SessionInfo> context) {}

    /**
     * executeUpdate方法前置处理
     */
    default void preProcessExecuteUpdate(CurrentRequestContext<StatementRequest, OpResult> context) {}

    /**
     * executeUpdate方法后置处理
     */
    default void postProcessExecuteUpdate(CurrentRequestContext<StatementRequest, OpResult> context) {}

    /**
     * executeQuery方法前置处理
     */
    default void preProcessExecuteQuery(CurrentRequestContext<StatementRequest, StreamObserver<OpResult>> context) {}

    /**
     * executeQuery方法后置处理
     */
    default void postProcessExecuteQuery(CurrentRequestContext<StatementRequest, StreamObserver<OpResult>> context) {}

    /**
     * fetchNextRows方法前置处理
     */
    default void preProcessFetchNextRows(CurrentRequestContext<ResultSetFetchRequest, OpResult> context) {}

    /**
     * fetchNextRows方法后置处理
     */
    default void postProcessFetchNextRows(CurrentRequestContext<ResultSetFetchRequest, OpResult> context) {}

    /**
     * createLob方法前置处理
     */
    default void preProcessCreateLob(CurrentRequestContext<StreamObserver<LobDataBlock>, StreamObserver<LobReference>> context) {}

    /**
     * createLob方法后置处理
     */
    default void postProcessCreateLob(CurrentRequestContext<StreamObserver<LobDataBlock>, StreamObserver<LobReference>> context) {}

    /**
     * readLob方法前置处理
     */
    default void preProcessReadLob(CurrentRequestContext<ReadLobRequest, StreamObserver<LobDataBlock>> context) {}

    /**
     * readLob方法后置处理
     */
    default void postProcessReadLob(CurrentRequestContext<ReadLobRequest, StreamObserver<LobDataBlock>> context) {}

    /**
     * startTransaction方法前置处理
     */
    default void preProcessStartTransaction(CurrentRequestContext<SessionInfo, SessionInfo> context) {}

    /**
     * startTransaction方法后置处理
     */
    default void postProcessStartTransaction(CurrentRequestContext<SessionInfo, SessionInfo> context) {}

    /**
     * commitTransaction方法前置处理
     */
    default void preProcessCommitTransaction(CurrentRequestContext<SessionInfo, SessionInfo> context) {}

    /**
     * commitTransaction方法后置处理
     */
    default void postProcessCommitTransaction(CurrentRequestContext<SessionInfo, SessionInfo> context) {}

    /**
     * rollbackTransaction方法前置处理
     */
    default void preProcessRollbackTransaction(CurrentRequestContext<SessionInfo, SessionInfo> context) {}

    /**
     * rollbackTransaction方法后置处理
     */
    default void postProcessRollbackTransaction(CurrentRequestContext<SessionInfo, SessionInfo> context) {}

    /**
     * terminateSession方法前置处理
     */
    default void preProcessTerminateSession(CurrentRequestContext<SessionInfo, SessionTerminationStatus> context) {}

    /**
     * terminateSession方法后置处理
     */
    default void postProcessTerminateSession(CurrentRequestContext<SessionInfo, SessionTerminationStatus> context) {}

    /**
     * callResource方法前置处理
     */
    default void preProcessCallResource(CurrentRequestContext<CallResourceRequest, CallResourceResponse> context) {}

    /**
     * callResource方法后置处理
     */
    default void postProcessCallResource(CurrentRequestContext<CallResourceRequest, CallResourceResponse> context) {}
}