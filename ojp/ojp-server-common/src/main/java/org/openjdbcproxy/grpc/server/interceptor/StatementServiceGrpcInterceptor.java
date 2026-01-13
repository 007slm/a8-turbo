package org.openjdbcproxy.grpc.server.interceptor;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Tracer;
import org.apache.skywalking.apm.toolkit.trace.SpanRef;
import org.openjdbcproxy.grpc.server.SessionManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring gRPC拦截器适配器
 * 基于框架原生拦截器机制，提供业务友好的拦截能力
 *
 * SkyWalking Trace 管理策略：
 * - 在 onMessage 中创建 EntrySpan
 * - 在 ServerCall.close() 中结束 span
 * - 这样确保整个 gRPC 请求处理过程（包括 Service 方法执行）都有 trace context
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatementServiceGrpcInterceptor implements ServerInterceptor {

    private static final Context.Key<StatementServiceInterceptContext> grpcContextWithInterceptorContextKey =
            Context.key("statement.service.intercept.context");

    private final SessionManager sessionManager;

    private final List<StatementServiceInterceptor> businessInterceptors;
    @SuppressWarnings("unchecked")
    public static StatementServiceInterceptContext getCurrentContext() {
        Context context = Context.current();
        return (StatementServiceInterceptContext) grpcContextWithInterceptorContextKey.get(context);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // 1. 解析方法名
        String methodName = call.getMethodDescriptor().getBareMethodName();

        // 2. 创建业务上下文
        StatementServiceInterceptContext<ReqT, RespT> context = new StatementServiceInterceptContext<>(
                methodName,
                call,
                headers,
                sessionManager
        );

        // 3. 将 context 存入 gRPC Context（span 在 onMessage 中创建，确保包裹 Service 方法执行）
        Context grpcContextWithInterceptorContext = Context.current()
                .withValue(grpcContextWithInterceptorContextKey, context);

        // 4. 包装响应处理器（处理后置逻辑）
        ServerCall<ReqT, RespT> wrappedCall = wrapServerCall(call, context);

        ServerCall.Listener<ReqT> listener = Contexts.interceptCall(grpcContextWithInterceptorContext, wrappedCall, headers, next);
        return wrapListener(listener, context, call);
    }

    /**
     * 执行所有拦截器的前置处理
     */
    private void executePreProcessors(StatementServiceInterceptContext<?, ?> context) {
        for (StatementServiceInterceptor interceptor : businessInterceptors) {
            // 先执行通用前置处理
            interceptor.preProcess(context);
            // 再执行特定方法前置处理
            dispatchPreProcess(interceptor, context);
        }
    }


    /**
     * 包装ServerCall，处理响应发送和后置逻辑
     */
    private <ReqT, RespT> ServerCall<ReqT, RespT> wrapServerCall(
            ServerCall<ReqT, RespT> original,
            StatementServiceInterceptContext<ReqT, RespT> context) {
        return new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(original) {
            @Override
            public void sendMessage(RespT message) {
                // 保存响应结果到上下文
                context.setResponseResult(message);
                super.sendMessage(message);
            }

            @Override
            public void close(Status status, Metadata trailers) {
                try {
                    if (status.isOk()) {
                        // 正常结束时执行后置处理
                        executePostProcessors(context);
                    } else {
                        // 异常时执行错误处理
                        context.setError(status.getCause());
                        executeErrorProcessors(context);
                    }
                } finally {
                    super.close(status, trailers);
                }
            }
        };
    }

    /**
     * 包装监听器，处理请求数据和流式数据
     * 
     * 关键：在 onMessage 中创建和结束 span，确保 Service 方法执行时有 trace context
     * 如果请求包含 sessionUUID，则创建该 session 的子 span；否则创建独立 span
     */
    private <ReqT, RespT> ServerCall.Listener<ReqT> wrapListener(
            ServerCall.Listener<ReqT> original,
            StatementServiceInterceptContext<ReqT, RespT> context,
            ServerCall call) {

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(original) {
            @Override
            public void onMessage(ReqT message) {
                // 提取 session 信息（如果存在）
                String sessionUUID = extractSessionUUID(message, context.getMethodName());
                SpanRef entrySpan = null;
                
                try {
                    // 根据是否有 session 来决定 span 创建策略
                    if (sessionUUID != null) {
                        // 有 session：尝试获取 session 的 trace context
                        Object sessionTraceObj = sessionManager.getSessionTraceContext(sessionUUID);
                        
                        if (sessionTraceObj instanceof SpanRef) {
                            // 创建 session 的子 span
                            entrySpan = Tracer.createLocalSpan("gRPC/" + call.getMethodDescriptor().getFullMethodName());
                            ActiveSpan.tag("grpc.method", context.getMethodName());
                            ActiveSpan.tag("session.uuid", sessionUUID);
                            ActiveSpan.tag("component", "ojp-grpc-request");
                        } else {
                            // session span 不存在，创建独立 span
                            entrySpan = Tracer.createEntrySpan("gRPC/" + call.getMethodDescriptor().getFullMethodName(), null);
                            ActiveSpan.tag("grpc.method", context.getMethodName());
                            ActiveSpan.tag("session.uuid", sessionUUID);
                            ActiveSpan.tag("component", "ojp-grpc-server");
                        }
                    } else {
                        // 没有 session（如 connect 请求）：创建独立的 entry span
                        entrySpan = Tracer.createEntrySpan("gRPC/" + call.getMethodDescriptor().getFullMethodName(), null);
                        ActiveSpan.tag("grpc.method", context.getMethodName());
                        ActiveSpan.tag("component", "ojp-grpc-server");
                    }
                    
                    // 保存请求数据到上下文
                    context.setRequest(message);
                    // 执行前置拦截
                    executePreProcessors(context);

                    // 触发客户端流式数据拦截
                    for (StatementServiceInterceptor interceptor : businessInterceptors) {
                        interceptor.onClientData(context, message);
                    }
                    
                    // 调用原始的 onMessage（Service 方法）- 同步执行
                    super.onMessage(message);
                    
                } catch (Exception e) {
                    if (entrySpan != null) {
                        ActiveSpan.error(e);
                    }
                    throw e;
                } finally {
                    // 在 Service 方法执行完成后结束 span
                    if (entrySpan != null) {
                        Tracer.stopSpan();
                    }
                }
            }

            @Override
            public void onComplete() {
                for (StatementServiceInterceptor interceptor : businessInterceptors) {
                    interceptor.onClientComplete(context);
                }
                try {
                    super.onComplete();
                } catch (Exception e) {
                    context.setError(e);
                    executeErrorProcessors(context);
                    throw e;
                }
            }

            @Override
            public void onCancel() {
                for (StatementServiceInterceptor interceptor : businessInterceptors) {
                    interceptor.onClientCancel(context);
                }
                super.onCancel();
            }

            @Override
            public void onReady() {
                for (StatementServiceInterceptor interceptor : businessInterceptors) {
                    interceptor.onReady(context);
                }
                super.onReady();
            }
        };
    }



    /**
     * 执行后置处理（逆序执行，保证责任链完整性）
     */
    private void executePostProcessors(StatementServiceInterceptContext<?, ?> context) {
        for (int i = businessInterceptors.size() - 1; i >= 0; i--) {
            StatementServiceInterceptor interceptor = businessInterceptors.get(i);
            dispatchPostProcess(interceptor, context);
            interceptor.postProcess(context);
        }
    }

    /**
     * 执行异常处理
     */
    private void executeErrorProcessors(StatementServiceInterceptContext<?, ?> context) {
        for (StatementServiceInterceptor interceptor : businessInterceptors) {
            interceptor.onError(context, context.getError());
        }
    }

    /**
     * 路由到特定方法的前置处理
     */
    private void dispatchPreProcess(StatementServiceInterceptor interceptor, StatementServiceInterceptContext<?, ?> context) {
        switch (context.getMethodName()) {
            case "connect":
                interceptor.preProcessConnect(context);
                break;
            case "executeUpdate":
                interceptor.preProcessExecuteUpdate(context);
                break;
            case "executeQuery":
                interceptor.preProcessExecuteQuery(context);
                break;
            case "fetchNextRows":
                interceptor.preProcessFetchNextRows(context);
                break;
            case "createLob":
                interceptor.preProcessCreateLob(context);
                break;
            case "readLob":
                interceptor.preProcessReadLob(context);
                break;
            case "startTransaction":
                interceptor.preProcessStartTransaction(context);
                break;
            case "commitTransaction":
                interceptor.preProcessCommitTransaction(context);
                break;
            case "rollbackTransaction":
                interceptor.preProcessRollbackTransaction(context);
                break;
            case "terminateSession":
                interceptor.preProcessTerminateSession(context);
                break;
            case "callResource":
                interceptor.preProcessCallResource(context);
                break;
            case "createStatement":
                interceptor.preProcessCreateStatement(context);
                break;
            case "createPreparedStatement":
                interceptor.preProcessCreatePreparedStatement(context);
                break;
        }
    }

    /**
     * 路由到特定方法的后置处理
     */
    private void dispatchPostProcess(StatementServiceInterceptor interceptor, StatementServiceInterceptContext<?, ?> context) {
        switch (context.getMethodName()) {
            case "connect":
                interceptor.postProcessConnect(context);
                break;
            case "executeUpdate":
                interceptor.postProcessExecuteUpdate(context);
                break;
            case "executeQuery":
                interceptor.postProcessExecuteQuery(context);
                break;
            case "fetchNextRows":
                interceptor.postProcessFetchNextRows(context);
                break;
            case "createLob":
                interceptor.postProcessCreateLob(context);
                break;
            case "readLob":
                interceptor.postProcessReadLob(context);
                break;
            case "startTransaction":
                interceptor.postProcessStartTransaction(context);
                break;
            case "commitTransaction":
                interceptor.postProcessCommitTransaction(context);
                break;
            case "rollbackTransaction":
                interceptor.postProcessRollbackTransaction(context);
                break;
            case "terminateSession":
                interceptor.postProcessTerminateSession(context);
                break;
            case "callResource":
                interceptor.postProcessCallResource(context);
                break;
            case "createStatement":
                interceptor.postProcessCreateStatement(context);
                break;
            case "createPreparedStatement":
                interceptor.postProcessCreatePreparedStatement(context);
                break;
        }
    }
    
    /**
     * 从请求消息中提取 session UUID
     * 使用反射来处理不同类型的请求消息
     */
    private String extractSessionUUID(Object message, String methodName) {
        if (message == null) {
            return null;
        }
        
        try {
            // connect 请求没有 session
            if ("connect".equals(methodName)) {
                return null;
            }
            
            // 大部分请求都有 session 字段
            java.lang.reflect.Method getSessionMethod = message.getClass().getMethod("getSession");
            Object sessionInfo = getSessionMethod.invoke(message);
            
            if (sessionInfo != null) {
                java.lang.reflect.Method getSessionUUIDMethod = sessionInfo.getClass().getMethod("getSessionUUID");
                String sessionUUID = (String) getSessionUUIDMethod.invoke(sessionInfo);
                return sessionUUID != null && !sessionUUID.isEmpty() ? sessionUUID : null;
            }
        } catch (Exception e) {
            // 如果反射失败，记录日志但不影响正常流程
            log.debug("Failed to extract session UUID from message: {}", e.getMessage());
        }
        
        return null;
    }
}