package org.openjdbcproxy.grpc.server.chain;

import java.sql.SQLException;

/**
 * 后处理器接口
 * 
 * 定义处理器在SQL执行完成后的后处理能力
 * 只有需要后处理的处理器才需要实现此接口
 */
public interface PostProcessor {
    
    /**
     * 执行后处理操作
     * 
     * @param context SQL处理上下文
     * @throws SQLException 后处理过程中的SQL异常
     */
    void postProcess(SqlProcessContext context) throws SQLException;
}
