package org.openjdbcproxy.grpc.server.interceptor;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.openjdbcproxy.database.DatabaseUtils;
import org.openjdbcproxy.grpc.server.SessionManager;

import com.openjdbcproxy.grpc.*;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
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



    // 当前请求关联的信息
    @Getter
    @Setter
    DbName currentDbName;

    @Setter
    @Getter
    private SessionInfo currentSessionInfo;

    @Setter
    private Connection currentConnection;

    @Setter
    private Connection currentInterceptedConnection;

    public Connection getCurrentConnection(){
        // 优先从上下文获取 支持拦截器修改connection
        return currentInterceptedConnection != null ? currentInterceptedConnection : currentConnection;
    }

    // 自定义属性存储（用于拦截点间传递数据）
    private final Map<String, Object> attributes = new HashMap<>();

    public StatementServiceInterceptContext(String methodName, ServerCall<ReqT, RespT> serverCall, Metadata headers) {
        this.methodName = methodName;
        this.serverCall = serverCall;
        this.headers = headers;

    }

    /**
     * 依据传入的 SessionInfo 在上下文中获取/创建数据库连接与会话，并回填上下文必要属性。
     *
     * 该方法整合了原先 StatementServiceImpl#acquireSessionContext 的逻辑。
     *
     * @param sessionManager       会话管理器
     * @param datasourceMap        数据源映射（connHash -> HikariDataSource）
     * @param sessionInfo          客户端传入的会话信息
     * @param startSessionIfNone   若无会话则创建新会话
     * @return 当前上下文（便于链式调用）
     * @throws java.sql.SQLException 获取连接失败或连接关闭时抛出
     */
    public StatementServiceInterceptContext<ReqT, RespT> acquireSessionContext(
            SessionManager sessionManager,
            java.util.Map<String, HikariDataSource> datasourceMap,
            SessionInfo sessionInfo,
            boolean startSessionIfNone) throws java.sql.SQLException {

        Connection conn;
        if (StringUtils.isNotEmpty(sessionInfo.getSessionUUID())) {
            conn = sessionManager.getConnection(sessionInfo);
        } else {
            HikariDataSource dataSource = datasourceMap.get(sessionInfo.getConnHash());
            conn = dataSource.getConnection();
            if (startSessionIfNone) {
                sessionInfo = sessionManager.createSession(sessionInfo.getClientUUID(), conn,sessionInfo.getReadOnly());
            }
        }

        this.setCurrentConnection(conn);
        this.setCurrentSessionInfo(sessionInfo);
        this.setCurrentDbName(DatabaseUtils.resolveDbName(conn.getMetaData().getURL()));

        return this;
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
    public StatementRequest requestToStatementRequest() {
        return (StatementRequest) request;
    }

    /**
     * 类型安全的连接请求参数获取（connect方法专用）
     */
    public ConnectionDetails requestToConnectionDetails() {
        return (ConnectionDetails) request;
    }

    /**
     * 类型安全的会话信息获取（事务相关方法专用）
     */
    public SessionInfo requestToSessionInfo() {
        return (SessionInfo) request;
    }

    /**
     * 类型安全的操作结果获取（executeUpdate等方法专用）
     */
    public OpResult responseToOpResultResult() {
        return (OpResult) responseResult;
    }

    /**
     * 类型安全的会话结果获取（connect等方法专用）
     */
    public SessionInfo responseToSessionInfo() {
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
