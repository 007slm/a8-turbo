package org.openjdbcproxy.grpc.server.service.interceptor.context;

import com.openjdbcproxy.grpc.SessionInfo;
import com.zaxxer.hikari.HikariDataSource;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import lombok.Getter;
import lombok.Setter;
import org.openjdbcproxy.grpc.server.SessionManager;
import org.openjdbcproxy.grpc.server.SlowQuerySegregationManager;
import org.openjdbcproxy.grpc.server.service.interceptor.impl.DataSourceInterceptor;

/**
 * gRPC拦截器上下文，封装调用相关的所有信息
 */

public class CurrentRequestContext<ReqT, RespT> extends AbstrctCurrentRequestContext<ReqT, RespT>{

    @Getter
    @Setter
    String connHash;


    @Getter
    @Setter
    SessionManager sessionManager;

    @Getter
    @Setter
    SlowQuerySegregationManager currentSlowQuerySegregationManager;

    @Getter
    @Setter
    HikariDataSource currentDataSource;
    @Getter
    @Setter
    DataSourceInterceptor dataSourceInterceptor;
    @Getter
    @Setter
    SessionInfo currentSessionInfo;

    public CurrentRequestContext(String methodName, ServerCall serverCall, Metadata headers) {
        super(methodName, serverCall, headers);
    }








}
