package org.openjdbcproxy.grpc.server.utils;

/**
 * SQL 语句工具类
 * 提供各种 SQL 语句类型判断的静态方法
 */
public final class SqlUtils {
    
    // 私有构造函数，防止实例化
    private SqlUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 检查 SQL 是否为查询语句
     * 
     * @param sql SQL 语句
     * @return 如果是查询语句返回 true，否则返回 false
     */
    public static boolean isSelectSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String trimmedSql = sql.trim().toLowerCase();
        return trimmedSql.startsWith("select") || 
               trimmedSql.startsWith("with") || 
               trimmedSql.contains(" select ");
    }
    
    /**
     * 检查 SQL 是否为 DML 语句（INSERT, UPDATE, DELETE）
     * 
     * @param sql SQL 语句
     * @return 如果是 DML 语句返回 true，否则返回 false
     */
    public static boolean isDmlSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String trimmedSql = sql.trim().toLowerCase();
        return trimmedSql.startsWith("insert") || 
               trimmedSql.startsWith("update") || 
               trimmedSql.startsWith("delete");
    }
    
    /**
     * 检查 SQL 是否为 DDL 语句（CREATE, ALTER, DROP, TRUNCATE 等）
     * 
     * @param sql SQL 语句
     * @return 如果是 DDL 语句返回 true，否则返回 false
     */
    public static boolean isDdlSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String trimmedSql = sql.trim().toLowerCase();
        return trimmedSql.startsWith("create") || 
               trimmedSql.startsWith("alter") || 
               trimmedSql.startsWith("drop") ||
               trimmedSql.startsWith("truncate");
    }
    
    /**
     * 检查 SQL 是否为事务控制语句（COMMIT, ROLLBACK, SAVEPOINT 等）
     * 
     * @param sql SQL 语句
     * @return 如果是事务控制语句返回 true，否则返回 false
     */
    public static boolean isTransactionSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String trimmedSql = sql.trim().toLowerCase();
        return trimmedSql.startsWith("commit") || 
               trimmedSql.startsWith("rollback") || 
               trimmedSql.startsWith("savepoint") ||
               trimmedSql.startsWith("begin");
    }
    
    /**
     * 获取 SQL 语句的类型
     * 
     * @param sql SQL 语句
     * @return SQL 语句类型枚举
     */
    public static SqlType getSqlType(String sql) {
        if (isSelectSql(sql)) {
            return SqlType.SELECT;
        } else if (isDmlSql(sql)) {
            return SqlType.DML;
        } else if (isDdlSql(sql)) {
            return SqlType.DDL;
        } else if (isTransactionSql(sql)) {
            return SqlType.TRANSACTION;
        } else {
            return SqlType.UNKNOWN;
        }
    }
    
    /**
     * SQL 语句类型枚举
     */
    public enum SqlType {
        SELECT("查询语句"),
        DML("数据操作语句"),
        DDL("数据定义语句"),
        TRANSACTION("事务控制语句"),
        UNKNOWN("未知类型");
        
        private final String description;
        
        SqlType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
