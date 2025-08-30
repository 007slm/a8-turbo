package org.openjdbcproxy.grpc.server.chain.processors;

import com.google.protobuf.ByteString;
import com.openjdbcproxy.grpc.OpResult;
import com.openjdbcproxy.grpc.ResultType;
import org.openjdbcproxy.grpc.server.SessionContext;
import org.openjdbcproxy.grpc.server.chain.AbstractSqlProcessor;
import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.openjdbcproxy.grpc.SerializationHandler.serialize;

/**
 * SQL执行处理器
 * 
 * 责任链中的最后一个处理器，负责实际执行SQL语句
 * 支持所有类型的SQL操作：SELECT、INSERT、UPDATE、DELETE、DDL等
 * 
 * 处理流程：
 * 1. 获取数据库连接
 * 2. 根据SQL类型选择执行方法
 * 3. 执行SQL语句
 * 4. 构建返回结果
 */
public class SqlExecutionProcessor extends AbstractSqlProcessor {
    
    private static final String PROCESSOR_NAME = "SqlExecutionProcessor";
    private static final Logger log = LoggerFactory.getLogger(SqlExecutionProcessor.class);
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        String sql = context.getCurrentSql();
        SessionContext connectionSession = context.getConnectionSession();
        
        if (connectionSession == null) {
            log.error("No connection session available for SQL execution");
            throw new SQLException("No connection session available");
        }
        
        try {
            // 获取数据库连接
            Connection connection = getConnection(context);
            if (connection == null) {
                log.error("Failed to get database connection");
                throw new SQLException("Failed to get database connection");
            }
            
            // 执行SQL并获取结果
            OpResult result = executeSQL(connection, sql, context);
            
            // 标记处理完成并设置结果
            context.markCompleted(result);
            
            log.debug("SQL execution completed successfully: {}", sql);
            return true;
            
        } catch (SQLException e) {
            log.error("SQL execution failed: {}", e.getMessage(), e);
            context.setError(e);
            throw e;
        }
    }
    
    /**
     * 获取数据库连接
     */
    private Connection getConnection(SqlProcessContext context) throws SQLException {
        SessionContext connectionSession = context.getConnectionSession();
        
        // 检查是否指定了目标数据库（分片场景）
        String targetDatabase = context.getAttribute("target_database");
        if (targetDatabase != null) {
            log.debug("Using target database: {}", targetDatabase);
            // 这里应该根据目标数据库获取相应的连接
            // 简化实现，仍使用原连接
        }
        
        // 这里应该从连接池获取连接
        // 简化实现，直接返回会话中的连接
        return connectionSession.getConnection();
    }
    
    /**
     * 执行SQL语句
     */
    private OpResult executeSQL(Connection connection, String sql, SqlProcessContext context) throws SQLException {
        OpResult.Builder resultBuilder = OpResult.newBuilder();
        
        switch (context.getOperationType()) {
            case SELECT:
                return executeQuery(connection, sql);
                
            case INSERT:
            case UPDATE:
            case DELETE:
                return executeUpdate(connection, sql);
                
            case CREATE:
            case DROP:
            case ALTER:
            case TRUNCATE:
                return executeDDL(connection, sql);
                
            case CALL:
                return executeCall(connection, sql);
                
            default:
                return executeGeneric(connection, sql);
        }
    }
    
    /**
     * 执行查询语句
     */
    private OpResult executeQuery(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            // 这里应该将ResultSet转换为OpResult
            // 简化实现，返回基本结果
            return OpResult.newBuilder()
                    .setType(ResultType.RESULT_SET_DATA)
                    .setFlag("Query executed successfully")
                    .build();
        }
    }
    
    /**
     * 执行更新语句（INSERT、UPDATE、DELETE）
     */
    private OpResult executeUpdate(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            int affectedRows = statement.executeUpdate(sql);
            
            return OpResult.newBuilder()
                    .setType(ResultType.INTEGER)
                    .setFlag("Update executed successfully, affected rows: " + affectedRows)
                    .setValue(ByteString.copyFrom(serialize(affectedRows)))
                    .build();
        }
    }
    
    /**
     * 执行DDL语句
     */
    private OpResult executeDDL(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            boolean result = statement.execute(sql);
            
            return OpResult.newBuilder()
                    .setType(ResultType.INTEGER)
                    .setFlag("DDL executed successfully")
                    .build();
        }
    }
    
    /**
     * 执行存储过程调用
     */
    private OpResult executeCall(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute(sql);
            
            if (hasResultSet) {
                // 处理返回的结果集
                try (ResultSet resultSet = statement.getResultSet()) {
                    return OpResult.newBuilder()
                            .setType(ResultType.RESULT_SET_DATA)
                            .setFlag("Procedure executed successfully with result set")
                            .build();
                }
            } else {
                // 没有结果集，可能有更新计数
                int updateCount = statement.getUpdateCount();
                return OpResult.newBuilder()
                        .setType(ResultType.INTEGER)
                        .setFlag("Procedure executed successfully, update count: " + updateCount)
                        .setValue(ByteString.copyFrom(serialize(updateCount)))
                        .build();
            }
        }
    }
    
    /**
     * 执行通用SQL语句
     */
    private OpResult executeGeneric(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute(sql);
            
            return OpResult.newBuilder()
                    .setType(ResultType.INTEGER)
                    .setFlag("SQL executed successfully")
                    .build();
        }
    }
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public int getPriority() {
        return -100; // 最低优先级，作为责任链的最后一环
    }
}