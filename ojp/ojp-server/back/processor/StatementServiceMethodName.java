package org.openjdbcproxy.grpc.server.processor;

/**
 * gRPC 方法类型枚举
 * 定义了 StatementService 中的所有核心方法
 */
public enum StatementServiceMethodName {
    
    /**
     * 连接方法
     */
    CONNECT("connect"),
    
    /**
     * 执行更新方法
     */
    EXECUTE_UPDATE("executeUpdate"),
    
    /**
     * 执行查询方法
     */
    EXECUTE_QUERY("executeQuery"),
    
    /**
     * 获取下一批行
     */
    FETCH_NEXT_ROWS("fetchNextRows"),
    
    /**
     * 创建 LOB
     */
    CREATE_LOB("createLob"),
    
    /**
     * 读取 LOB
     */
    READ_LOB("readLob"),
    
    /**
     * 终止会话
     */
    TERMINATE_SESSION("terminateSession"),
    
    /**
     * 开始事务
     */
    START_TRANSACTION("startTransaction"),
    
    /**
     * 提交事务
     */
    COMMIT_TRANSACTION("commitTransaction"),
    
    /**
     * 回滚事务
     */
    ROLLBACK_TRANSACTION("rollbackTransaction"),
    
    /**
     * 调用资源
     */
    CALL_RESOURCE("callResource");

    private final String methodName;

    StatementServiceMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    /**
     * 根据方法名称获取对应的枚举值
     * 
     * @param methodName 方法名称
     * @return 对应的枚举值，如果找不到则返回 null
     */
    public static StatementServiceMethodName fromMethodName(String methodName) {
        for (StatementServiceMethodName type : values()) {
            if (type.getMethodName().equals(methodName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为查询类型的方法
     */
    public boolean isQueryMethod() {
        return this == EXECUTE_QUERY || this == FETCH_NEXT_ROWS;
    }

    /**
     * 判断是否为更新类型的方法
     */
    public boolean isUpdateMethod() {
        return this == EXECUTE_UPDATE;
    }

    /**
     * 判断是否为事务相关的方法
     */
    public boolean isTransactionMethod() {
        return this == START_TRANSACTION || 
               this == COMMIT_TRANSACTION || 
               this == ROLLBACK_TRANSACTION;
    }

    /**
     * 判断是否为连接相关的方法
     */
    public boolean isConnectionMethod() {
        return this == CONNECT || this == TERMINATE_SESSION;
    }

    /**
     * 判断是否为 LOB 相关的方法
     */
    public boolean isLobMethod() {
        return this == CREATE_LOB || this == READ_LOB;
    }
}
