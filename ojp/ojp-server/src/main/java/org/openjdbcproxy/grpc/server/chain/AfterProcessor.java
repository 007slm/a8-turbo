package org.openjdbcproxy.grpc.server.chain;

import java.sql.SQLException;

/**
 * 后处理器接口
 * 
 * 定义处理器在SQL执行完成后的后处理能力
 * 只有需要后处理的处理器才需要实现此接口
 */
public interface AfterProcessor {
    
    /**
     * 执行后处理操作（在SQL执行完成后调用）
     * 
     * @param context SQL处理上下文
     * @throws SQLException 后处理过程中的SQL异常
     */
    void afterProcess(SqlProcessContext context) throws SQLException;
}
