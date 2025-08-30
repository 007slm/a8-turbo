package org.openjdbcproxy.grpc.server.chain;

import com.openjdbcproxy.grpc.OpResult;
import com.openjdbcproxy.grpc.StatementRequest;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.SessionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openjdbcproxy.grpc.SerializationHandler.deserialize;
import org.openjdbcproxy.grpc.dto.Parameter;
import java.util.ArrayList;

/**
 * SQL处理上下文
 * 
 * 包含SQL处理过程中需要的所有信息，在责任链中传递
 */
@Slf4j
@Data
@Builder
public class SqlProcessContext {
    
    /**
     * 原始SQL语句请求
     */
    private StatementRequest request;
    
    /**
     * 连接会话信息
     */
    private SessionContext connectionSession;
    
    /**
     * SQL操作类型
     */
    private SqlOperationType operationType;
    
    /**
     * 处理结果（如果已经被某个处理器处理完成）
     */
    private OpResult result;
    
    /**
     * 是否已完成处理
     */
    @Builder.Default
    private boolean completed = false;
    
    /**
     * 处理器上下文数据（用于处理器之间传递信息）
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    
    /**
     * 原始SQL（保持不变）
     */
    private String originalSql;
    
    /**
     * 当前SQL（可能被处理器修改）
     */
    private String currentSql;
    
    /**
     * SQL解析信息
     */
    private SqlParseInfo parseInfo;
    
    /**
     * 错误信息（如果处理过程中出现错误）
     */
    private Exception error;
    
    /**
     * 处理开始时间（用于性能监控）
     */
    @Builder.Default
    private long startTime = System.currentTimeMillis();
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 连接哈希
     */
    private String connectionHash;
    
    /**
     * SQL参数列表
     */
    private List<Parameter> parameters;
    
    /**
     * 用户上下文信息
     */
    private UserContext userContext;
    
    /**
     * 设置连接会话信息
     */
    public void setConnectionSession(SessionContext connectionSession) {
        this.connectionSession = connectionSession;
    }
    
    /**
     * 检查是否已完成处理
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * 获取处理结果
     */
    public OpResult getResult() {
        return result;
    }
    
    /**
     * 标记经过的时间（用于性能监控）
     */
    public void markElapsed() {
        // 可以在这里记录时间戳或其他性能指标
    }
    
    /**
     * 创建SQL处理上下文的静态工厂方法
     */
    public static SqlProcessContext create(StatementRequest request) {
        String sql = request.getSql();
        
        // 解析参数
        List<Parameter> parameters = new ArrayList<>();
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            try {
                // 反序列化参数
                parameters = deserializeParameters(request.getParameters().toByteArray());
            } catch (Exception e) {
                log.warn("Failed to deserialize parameters for SQL: {}", sql, e);
            }
        }
        
        return SqlProcessContext.builder()
                .request(request)
                .originalSql(sql)
                .currentSql(sql)
                .operationType(determineSqlOperationType(sql))
                .sessionId(request.getSession() != null ? request.getSession().getSessionUUID() : null)
                .connectionHash(request.getSession() != null ? request.getSession().getConnHash() : null)
                .parameters(parameters)
                .build();
    }
    
    /**
     * Deserialize parameters with proper type information
     */
    @SuppressWarnings("unchecked")
    private static List<Parameter> deserializeParameters(byte[] data) {
        try {
            // Deserialize as raw list first
            List<?> rawList = deserialize(data, List.class);
            
            // For type safety, we trust that the serialized data contains Parameter objects
            // In a production system, additional validation might be needed
            return (List<Parameter>) (List<?>) rawList;
        } catch (Exception e) {
            log.warn("Failed to deserialize parameters", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据SQL语句确定操作类型
     */
    private static SqlOperationType determineSqlOperationType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return SqlOperationType.UNKNOWN;
        }
        
        String trimmedSql = sql.trim().toUpperCase();
        
        if (trimmedSql.startsWith("SELECT")) {
            return SqlOperationType.SELECT;
        } else if (trimmedSql.startsWith("INSERT")) {
            return SqlOperationType.INSERT;
        } else if (trimmedSql.startsWith("UPDATE")) {
            return SqlOperationType.UPDATE;
        } else if (trimmedSql.startsWith("DELETE")) {
            return SqlOperationType.DELETE;
        } else if (trimmedSql.startsWith("CREATE")) {
            return SqlOperationType.CREATE;
        } else if (trimmedSql.startsWith("DROP")) {
            return SqlOperationType.DROP;
        } else if (trimmedSql.startsWith("ALTER")) {
            return SqlOperationType.ALTER;
        } else if (trimmedSql.startsWith("TRUNCATE")) {
            return SqlOperationType.TRUNCATE;
        } else if (trimmedSql.startsWith("CALL") || trimmedSql.startsWith("EXEC")) {
            return SqlOperationType.CALL;
        } else {
            return SqlOperationType.UNKNOWN;
        }
    }
    
    /**
     * 获取属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
    
    /**
     * 获取属性（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        return (T) attributes.getOrDefault(key, defaultValue);
    }
    
    /**
     * 检查是否包含属性
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    /**
     * 设置属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 标记处理完成
     */
    public void markCompleted() {
        this.completed = true;
    }
    
    /**
     * 标记处理完成并设置结果
     */
    public void markCompleted(OpResult result) {
        this.result = result;
        this.completed = true;
    }
    
    /**
     * 获取处理耗时
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 设置错误信息
     */
    public void setError(Exception error) {
        this.error = error;
    }
    
    /**
     * 更新SQL语句
     */
    public void updateSql(String newSql) {
        this.currentSql = newSql;
        // 重新解析SQL
        this.parseInfo = SqlParseInfo.parse(newSql);
        // 更新操作类型
        this.operationType = parseInfo.getOperationType();
    }
    
    /**
     * 检查是否为读操作
     */
    public boolean isReadOperation() {
        return operationType == SqlOperationType.SELECT;
    }
    
    /**
     * 检查是否为写操作
     */
    public boolean isWriteOperation() {
        return operationType == SqlOperationType.INSERT ||
               operationType == SqlOperationType.UPDATE ||
               operationType == SqlOperationType.DELETE ||
               operationType == SqlOperationType.TRUNCATE;
    }
    
    /**
     * 检查是否为DDL操作
     */
    public boolean isDdlOperation() {
        return operationType == SqlOperationType.CREATE ||
               operationType == SqlOperationType.DROP ||
               operationType == SqlOperationType.ALTER;
    }


    /**
     * SQL操作类型枚举
     */
    public enum SqlOperationType {
        SELECT,     // 查询操作
        INSERT,     // 插入操作
        UPDATE,     // 更新操作
        DELETE,     // 删除操作
        CREATE,     // 创建操作（表、索引等）
        DROP,       // 删除操作（表、索引等）
        ALTER,      // 修改操作（表结构等）
        TRUNCATE,   // 清空表操作
        CALL,       // 存储过程调用
        UNKNOWN     // 未知操作
    }



    /**
     * SQL解析信息
     */
    @Data
    @Builder
    public static class SqlParseInfo {

        /**
         * SQL操作类型
         */
        private SqlOperationType operationType;

        /**
         * 涉及的表名列表
         */
        private List<String> tableNames;

        /**
         * 涉及的列名列表
         */
        private List<String> columnNames;

        /**
         * WHERE条件信息
         */
        private WhereClauseInfo whereClause;

        /**
         * 是否包含聚合函数
         */
        private boolean hasAggregation;

        /**
         * 是否包含JOIN
         */
        private boolean hasJoin;

        /**
         * 是否包含子查询
         */
        private boolean hasSubquery;

        /**
         * 解析SQL语句
         */
        public static SqlParseInfo parse(String sql) {
            if (sql == null || sql.trim().isEmpty()) {
                return SqlParseInfo.builder()
                        .operationType(SqlOperationType.UNKNOWN)
                        .tableNames(List.of())
                        .columnNames(List.of())
                        .build();
            }

            String trimmedSql = sql.trim().toLowerCase();
            SqlOperationType operationType = determineOperationType(trimmedSql);

            // 这里应该使用真正的SQL解析器，如Antlr、JSqlParser等
            // 为了简化，这里提供基础实现
            return SqlParseInfo.builder()
                    .operationType(operationType)
                    .tableNames(extractTableNames(sql, operationType))
                    .columnNames(List.of()) // 需要完整的SQL解析器实现
                    .whereClause(extractWhereClause(sql))
                    .hasAggregation(hasAggregationFunctions(sql))
                    .hasJoin(sql.toLowerCase().contains(" join "))
                    .hasSubquery(sql.contains("(") && sql.toLowerCase().contains("select"))
                    .build();
        }

        private static SqlOperationType determineOperationType(String sql) {
            if (sql.startsWith("select")) return SqlOperationType.SELECT;
            if (sql.startsWith("insert")) return SqlOperationType.INSERT;
            if (sql.startsWith("update")) return SqlOperationType.UPDATE;
            if (sql.startsWith("delete")) return SqlOperationType.DELETE;
            if (sql.startsWith("create")) return SqlOperationType.CREATE;
            if (sql.startsWith("drop")) return SqlOperationType.DROP;
            if (sql.startsWith("alter")) return SqlOperationType.ALTER;
            if (sql.startsWith("truncate")) return SqlOperationType.TRUNCATE;
            if (sql.startsWith("call")) return SqlOperationType.CALL;
            return SqlOperationType.UNKNOWN;
        }

        private static List<String> extractTableNames(String sql, SqlOperationType operationType) {
            // 简化实现，实际需要使用SQL解析器
            return List.of(); // 占位实现
        }

        private static WhereClauseInfo extractWhereClause(String sql) {
            // 简化实现，实际需要使用SQL解析器
            boolean hasWhere = sql.toLowerCase().contains(" where ");
            return WhereClauseInfo.builder()
                    .hasWhere(hasWhere)
                    .whereConditions(List.of())
                    .build();
        }

        private static boolean hasAggregationFunctions(String sql) {
            String lowerSql = sql.toLowerCase();
            return lowerSql.contains("count(") || lowerSql.contains("sum(") ||
                    lowerSql.contains("avg(") || lowerSql.contains("max(") ||
                    lowerSql.contains("min(") || lowerSql.contains("group by");
        }
    }

    /**
     * WHERE子句信息
     */
    @Data
    @Builder
    public static class WhereClauseInfo {

        /**
         * 是否包含WHERE子句
         */
        private boolean hasWhere;

        /**
         * WHERE条件列表
         */
        private List<WhereCondition> whereConditions;

        /**
         * 原始WHERE子句
         */
        private String originalWhereClause;
    }

    /**
     * WHERE条件信息
     */
    @Data
    @Builder
    public static class WhereCondition {

        /**
         * 列名
         */
        private String columnName;

        /**
         * 操作符（=, >, <, IN, LIKE等）
         */
        private String operator;

        /**
         * 值
         */
        private Object value;

        /**
         * 逻辑连接符（AND, OR）
         */
        private String logicalOperator;
    }

    /**
     * 用户上下文信息
     */
    @Data
    @Builder
    public static class UserContext {

        /**
         * 用户ID
         */
        private String userId;

        /**
         * 租户ID
         */
        private String tenantId;

        /**
         * 用户角色
         */
        private java.util.Set<String> roles;

        /**
         * 数据权限范围
         */
        private DataScope dataScope;

        /**
         * 用户部门
         */
        private java.util.Set<String> departments;

        /**
         * 用户属性
         */
        @Builder.Default
        private Map<String, Object> attributes = new HashMap<>();

        /**
         * 数据权限范围枚举
         */
        public enum DataScope {
            ALL,           // 全部数据
            DEPARTMENT,    // 部门数据
            PERSONAL,      // 个人数据
            CUSTOM         // 自定义范围
        }
    }
}
