package org.openjdbcproxy.grpc.server.chain;

import com.openjdbcproxy.grpc.OpResult;
import com.openjdbcproxy.grpc.StatementRequest;
import io.grpc.stub.StreamObserver;

import java.sql.SQLException;

/**
 * SQL语句处理责任链接口
 * 
 * 支持所有类型的SQL操作：SELECT、INSERT、UPDATE、DELETE、DDL等
 * 每个处理器可以对SQL进行检查、修改、拦截或执行
 */
public interface SqlProcessor {
    
    /**
     * 处理SQL语句请求
     * 
     * @param context SQL处理上下文
     * @return true 如果已处理完成，false 如果需要继续传递给下一个处理器
     * @throws SQLException 处理过程中的SQL异常
     */
    boolean process(SqlProcessContext context) throws SQLException;
    
    /**
     * 设置下一个处理器
     * 
     * @param next 下一个处理器
     */
    void setNext(SqlProcessor next);
    
    /**
     * 获取处理器名称（用于日志和调试）
     * 
     * @return 处理器名称
     */
    String getProcessorName();
    
    /**
     * 获取处理器优先级（数字越大优先级越高）
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }
    

    /**
     * 获取支持的SQL操作类型
     * 
     * @return 支持的操作类型集合
     */
    default java.util.Set<SqlProcessContext.SqlOperationType> getSupportedOperations() {
        return java.util.Set.of(SqlProcessContext.SqlOperationType.values());
    }
}