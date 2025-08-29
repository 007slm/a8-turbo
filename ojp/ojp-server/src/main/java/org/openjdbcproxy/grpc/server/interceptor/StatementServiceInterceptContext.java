package org.openjdbcproxy.grpc.server.interceptor;

import com.openjdbcproxy.grpc.ConnectionDetails;
import com.openjdbcproxy.grpc.OpResult;
import com.openjdbcproxy.grpc.SessionInfo;
import com.openjdbcproxy.grpc.StatementRequest;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * gRPC拦截器上下文，封装调用相关的所有信息
 */
@Getter
public class StatementServiceInterceptContext<ReqT, RespT> {
    // 方法名称
    private final String methodName;
    
    // gRPC原生对象
    private final ServerCall<ReqT, RespT> serverCall;
    private final Metadata headers;
    
    // 请求和响应数据
    @Setter
    private ReqT request;
    @Setter
    private RespT responseResult;
    @Setter
    private Throwable error;
    
    // 自定义属性存储（用于拦截点间传递数据）
    private final Map<String, Object> attributes = new HashMap<>();

    public StatementServiceInterceptContext(String methodName, ServerCall<ReqT, RespT> serverCall, Metadata headers) {
        this.methodName = methodName;
        this.serverCall = serverCall;
        this.headers = headers;

    }

    /**
     * 设置自定义属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取自定义属性
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 类型安全的请求参数获取（executeUpdate等方法专用）
     */
    public StatementRequest getStatementRequest() {
        if (!"executeUpdate".equals(methodName) && !"executeQuery".equals(methodName)) {
            throw new IllegalStateException("Method [" + methodName + "] does not support StatementRequest");
        }
        return (StatementRequest) request;
    }

    /**
     * 类型安全的连接请求参数获取（connect方法专用）
     */
    public ConnectionDetails getConnectionDetails() {
        if (!"connect".equals(methodName)) {
            throw new IllegalStateException("Method [" + methodName + "] does not support ConnectionDetails");
        }
        return (ConnectionDetails) request;
    }

    /**
     * 类型安全的会话信息获取（事务相关方法专用）
     */
    public SessionInfo getSessionInfo() {
        if (!isSessionInfoMethod()) {
            throw new IllegalStateException("Method [" + methodName + "] does not support SessionInfo");
        }
        return (SessionInfo) request;
    }

    /**
     * 类型安全的操作结果获取（executeUpdate等方法专用）
     */
    public OpResult getOpResultResult() {
        if (!"executeUpdate".equals(methodName) && !"fetchNextRows".equals(methodName)) {
            throw new IllegalStateException("Method [" + methodName + "] does not return OpResult");
        }
        return (OpResult) responseResult;
    }

    /**
     * 类型安全的会话结果获取（connect等方法专用）
     */
    public SessionInfo getSessionInfoResult() {
        if (!isSessionResultMethod()) {
            throw new IllegalStateException("Method [" + methodName + "] does not return SessionInfo");
        }
        return (SessionInfo) responseResult;
    }

    /**
     * 判断是否为需要SessionInfo参数的方法
     */
    private boolean isSessionInfoMethod() {
        return "startTransaction".equals(methodName) 
                || "commitTransaction".equals(methodName)
                || "rollbackTransaction".equals(methodName)
                || "terminateSession".equals(methodName);
    }

    /**
     * 判断是否为返回SessionInfo的方法
     */
    private boolean isSessionResultMethod() {
        return "connect".equals(methodName)
                || "startTransaction".equals(methodName)
                || "commitTransaction".equals(methodName)
                || "rollbackTransaction".equals(methodName);
    }
}
