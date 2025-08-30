package org.openjdbcproxy.grpc.server.service.interceptor.impl;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 全局 gRPC 拦截器
 * 用于记录请求日志、性能监控等
 */
@Slf4j
@GlobalServerInterceptor
@Service
@Order(100)
public class GlobalLoggingInterceptor implements ServerInterceptor {
    
    private static final AtomicLong requestCounter = new AtomicLong(0);
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        long requestId = requestCounter.incrementAndGet();
        String methodName = call.getMethodDescriptor().getFullMethodName();
        long startTime = System.currentTimeMillis();
        
        log.info("gRPC请求开始 - ID: {}, 方法: {}, 客户端: {}", 
                requestId, methodName, call.getAuthority());
        
        // 包装 ServerCall 以记录响应信息
        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("gRPC请求完成 - ID: {}, 方法: {}, 状态: {}, 耗时: {}ms", 
                        requestId, methodName, status.getCode(), duration);
                super.close(status, trailers);
            }
        };
        
        // 包装 ServerCall.Listener 以记录请求体信息
        ServerCall.Listener<ReqT> wrappedListener = new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(wrappedCall, headers)) {
            
            @Override
            public void onMessage(ReqT message) {
                log.info("gRPC请求消息 - ID: {}, 方法: {}, 消息类型: {}",
                        requestId, methodName, message.getClass().getSimpleName());
                super.onMessage(message);
            }
            
            @Override
            public void onComplete() {
                log.info("gRPC请求完成 - ID: {}, 方法: {}", requestId, methodName);
                super.onComplete();
            }
            
            @Override
            public void onCancel() {
                log.info("gRPC请求被取消 - ID: {}, 方法: {}", requestId, methodName);
                super.onCancel();
            }
        };
        
        return wrappedListener;
    }
}
