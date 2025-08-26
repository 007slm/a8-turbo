package org.openjdbcproxy.grpc.server.chain;

import java.sql.SQLException;

/**
 * 前处理器接口
 * 
 * 定义处理器在SQL执行前的预处理能力
 * 只有需要前处理的处理器才需要实现此接口
 * 
 * 前处理通常用于：
 * 1. 参数验证和转换
 * 2. SQL语句预处理和优化
 * 3. 权限预检查
 * 4. 连接池预分配
 * 5. 监控和日志记录
 */
public interface PreProcessor {
    
    /**
     * 执行前处理操作
     * 
     * @param context SQL处理上下文
     * @throws SQLException 前处理过程中的SQL异常
     */
    void preProcess(SqlProcessContext context) throws SQLException;
}
