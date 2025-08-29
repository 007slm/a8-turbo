package org.openjdbcproxy.grpc.server.processor;

/**
 * gRPC 方法类型工具类
 * 提供方法类型判断的静态工具方法
 */
public final class StatementServiceUtils {
    
    private StatementServiceUtils() {
        // 工具类，禁止实例化
    }
    
    /**
     * 检查是否为查询类型的方法
     */
    public static boolean isQueryMethod(StatementServiceMethodName methodType) {
        return methodType == StatementServiceMethodName.EXECUTE_QUERY ||
               methodType == StatementServiceMethodName.FETCH_NEXT_ROWS;
    }
    
    /**
     * 检查是否为更新类型的方法
     */
    public static boolean isUpdateMethod(StatementServiceMethodName methodType) {
        return methodType == StatementServiceMethodName.EXECUTE_UPDATE;
    }
    
    /**
     * 检查是否为事务类型的方法
     */
    public static boolean isTransactionMethod(StatementServiceMethodName methodType) {
        return methodType == StatementServiceMethodName.START_TRANSACTION ||
               methodType == StatementServiceMethodName.COMMIT_TRANSACTION ||
               methodType == StatementServiceMethodName.ROLLBACK_TRANSACTION;
    }
    
    /**
     * 检查是否为连接类型的方法
     */
    public static boolean isConnectionMethod(StatementServiceMethodName methodType) {
        return methodType == StatementServiceMethodName.CONNECT ||
               methodType == StatementServiceMethodName.TERMINATE_SESSION;
    }
    
    /**
     * 检查是否为 LOB 类型的方法
     */
    public static boolean isLobMethod(StatementServiceMethodName methodType) {
        return methodType == StatementServiceMethodName.CREATE_LOB ||
               methodType == StatementServiceMethodName.READ_LOB;
    }
}
