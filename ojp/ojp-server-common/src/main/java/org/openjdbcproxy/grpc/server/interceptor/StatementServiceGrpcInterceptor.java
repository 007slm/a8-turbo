package org.openjdbcproxy.grpc.server.interceptor;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring gRPC拦截器适配器
 * 基于框架原生拦截器机制，提供业务友好的拦截能力
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatementServiceGrpcInterceptor implements ServerInterceptor {

    private static final Context.Key<StatementServiceInterceptContext> grpcContextWithInterceptorContextKey =
            Context.key("statement.service.intercept.context");


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
                headers
        );

        // 4. 包装响应处理器（处理后置逻辑）
        ServerCall<ReqT, RespT> wrappedCall = wrapServerCall(call, context);
        
        // 5. 包装处理请求和流式数据
        Context grpcContextWithInterceptorContext = Context.current()
                .withValue(grpcContextWithInterceptorContextKey,
                        context);


        ServerCall.Listener<ReqT> listener = Contexts.interceptCall(grpcContextWithInterceptorContext, wrappedCall, headers, next);
        return wrapListener(listener, context,call);
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
                if (status.isOk()) {
                    // 正常结束时执行后置处理
                    executePostProcessors(context);
                } else {
                    // 异常时执行错误处理
                    context.setError(status.getCause());
                    executeErrorProcessors(context);
                }
                super.close(status, trailers);
            }
        };
    }

    /**
     * 包装监听器，处理请求数据和流式数据
     */
    private <ReqT, RespT> ServerCall.Listener<ReqT> wrapListener(
            ServerCall.Listener<ReqT> original,
            StatementServiceInterceptContext<ReqT, RespT> context,
            ServerCall  call) {
        
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(original) {
            @Override
            public void onMessage(ReqT message) {
                // 保存请求数据到上下文
                context.setRequest(message);
                // 执行前置拦截
                executePreProcessors(context);

                // 触发客户端流式数据拦截
                for (StatementServiceInterceptor interceptor : businessInterceptors) {
                    interceptor.onClientData(context, message);
                }
                try {
                    super.onMessage(message);
                } catch (Exception e) {
                    context.setError(e);
                    executeErrorProcessors(context);
                    throw e;
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
}