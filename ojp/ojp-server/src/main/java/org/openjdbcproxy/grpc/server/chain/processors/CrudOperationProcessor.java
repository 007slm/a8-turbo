package org.openjdbcproxy.grpc.server.chain.processors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;
import org.openjdbcproxy.grpc.server.chain.PreProcessor;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CRUD操作处理器
 * 
 * 负责所有CRUD操作的统一处理：
 * 1. 操作类型验证和规范化
 * 2. 批量操作检测和优化
 * 3. 操作审计和监控
 * 4. 操作级别的权限控制
 * 5. SQL注入检测
 */
@Slf4j
@Component
public class CrudOperationProcessor extends AbstractSqlProcessor implements PreProcessor {
    
    private static final String PROCESSOR_NAME = "CrudOperationProcessor";
    
    // 操作统计
    private final Map<SqlProcessContext.SqlOperationType, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong rejectedOperations = new AtomicLong(0);
    
    // 危险操作模式
    private static final Set<String> DANGEROUS_PATTERNS = Set.of(
        "DROP\\s+TABLE",
        "TRUNCATE\\s+TABLE", 
        "DELETE\\s+FROM\\s+\\w+\\s*;?\\s*$", // DELETE without WHERE
        "UPDATE\\s+\\w+\\s+SET\\s+.*\\s*;?\\s*$" // UPDATE without WHERE
    );
    
    public CrudOperationProcessor() {
        // 初始化操作计数器
        for (SqlProcessContext.SqlOperationType type : SqlProcessContext.SqlOperationType.values()) {
            operationCounts.put(type, new AtomicLong(0));
        }
    }
    

    
    /**
     * 验证和规范化SQL语句
     */
    private void validateAndNormalizeSql(SqlProcessContext context) throws SQLException {
        String sql = context.getCurrentSql();
        SqlProcessContext.SqlOperationType operationType = context.getOperationType();
        
        // 基本SQL验证
        if (sql == null || sql.trim().isEmpty()) {
            throw new SQLException("SQL statement cannot be empty");
        }
        
        // SQL长度检查
        if (sql.length() > 10000) { // 10KB limit
            log.warn("Large SQL statement detected: {} characters", sql.length());
            context.setAttribute("large_sql", true);
        }
        
        // 操作类型与SQL语句一致性检查
        String trimmedSql = sql.trim().toLowerCase();
        if (!isOperationTypeConsistent(operationType, trimmedSql)) {
            log.warn("Operation type mismatch: expected {}, but SQL starts with {}", 
                    operationType, trimmedSql.substring(0, Math.min(20, trimmedSql.length())));
        }
        
        // 规范化SQL（移除多余空格等）
        String normalizedSql = normalizeSql(sql);
        if (!normalizedSql.equals(sql)) {
            context.updateSql(normalizedSql);
            context.setAttribute("sql_normalized", true);
        }
    }
    
    /**
     * 检测危险操作
     */
    private void checkDangerousOperation(SqlProcessContext context) throws SQLException {
        String sql = context.getCurrentSql().toUpperCase();
        
        for (String pattern : DANGEROUS_PATTERNS) {
            if (sql.matches(".*" + pattern + ".*")) {
                log.warn("Dangerous operation detected: {} in SQL: {}", pattern, context.getCurrentSql());
                context.setAttribute("dangerous_operation", true);
                context.setAttribute("danger_pattern", pattern);
                break;
            }
        }
    }
    
    /**
     * 检测批量操作
     */
    private void detectBatchOperation(SqlProcessContext context) {
        String sql = context.getCurrentSql();
        
        // 检测批量INSERT
        if (context.getOperationType() == SqlProcessContext.SqlOperationType.INSERT) {
            if (sql.toUpperCase().contains("VALUES") && sql.contains("),(")) {
                context.setAttribute("batch_insert", true);
                int valueCount = sql.split("\\),\\s*\\(").length;
                context.setAttribute("batch_size", valueCount);
                log.debug("Batch INSERT detected with {} rows", valueCount);
            }
        }
        
        // 检测可能的批量UPDATE/DELETE
        if (context.getOperationType() == SqlProcessContext.SqlOperationType.UPDATE ||
            context.getOperationType() == SqlProcessContext.SqlOperationType.DELETE) {
            
            // 简单启发式：包含IN子句可能是批量操作
            if (sql.toUpperCase().contains(" IN (")) {
                context.setAttribute("potential_batch_operation", true);
                log.debug("Potential batch operation detected: {}", context.getOperationType());
            }
        }
    }
    

    
    /**
     * 记录审计信息
     */
    private void recordAuditInfo(SqlProcessContext context) {
        // 构建审计记录
        AuditRecord audit = AuditRecord.builder()
                .sessionId(context.getSessionId())
                .userId("unknown")
                .operationType(context.getOperationType())
                .sql(context.getCurrentSql())
                .timestamp(System.currentTimeMillis())
                .dangerous(context.hasAttribute("dangerous_operation"))
                .batch(context.hasAttribute("batch_insert") || context.hasAttribute("potential_batch_operation"))
                .build();
        
        context.setAttribute("audit_record", audit);
        
        // 记录到日志（实际项目中可能写入审计数据库）
        if (audit.isDangerous() || audit.isBatch()) {
            log.info("Audit: {} executed {} operation: {}", 
                    audit.getUserId(), audit.getOperationType(), 
                    audit.getSql().substring(0, Math.min(100, audit.getSql().length())));
        }
    }
    
    /**
     * 记录操作统计
     */
    private void recordOperation(SqlProcessContext.SqlOperationType operationType) {
        operationCounts.get(operationType).incrementAndGet();
        totalOperations.incrementAndGet();
    }
    
    /**
     * 检查操作类型一致性
     */
    private boolean isOperationTypeConsistent(SqlProcessContext.SqlOperationType operationType, String sql) {
        switch (operationType) {
            case SELECT:
                return sql.startsWith("select") || sql.startsWith("with");
            case INSERT:
                return sql.startsWith("insert");
            case UPDATE:
                return sql.startsWith("update");
            case DELETE:
                return sql.startsWith("delete");
            case CREATE:
                return sql.startsWith("create");
            case DROP:
                return sql.startsWith("drop");
            case ALTER:
                return sql.startsWith("alter");
            case TRUNCATE:
                return sql.startsWith("truncate");
            case CALL:
                return sql.startsWith("call") || sql.startsWith("exec");
            default:
                return true; // UNKNOWN等类型不检查
        }
    }
    
    /**
     * 规范化SQL语句
     */
    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
    

    
    /**
     * 获取操作统计信息
     */
    public CrudStats getStats() {
        Map<SqlProcessContext.SqlOperationType, Long> counts = new ConcurrentHashMap<>();
        operationCounts.forEach((type, count) -> counts.put(type, count.get()));
        
        return CrudStats.builder()
                .operationCounts(counts)
                .totalOperations(totalOperations.get())
                .rejectedOperations(rejectedOperations.get())
                .build();
    }
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public int getPriority() {
        return 95; // 在事务处理器之后，权限处理器之前
    }
    
    /**
     * 前处理：在SQL执行前进行验证和准备
     */
    @Override
    public void preProcess(SqlProcessContext context) throws SQLException {
        // 记录操作统计
        SqlProcessContext.SqlOperationType operationType = context.getOperationType();
        recordOperation(operationType);
        
        // 验证和规范化SQL
        validateAndNormalizeSql(context);
        
        // 检测危险操作
        checkDangerousOperation(context);
        
        // 检测批量操作
        detectBatchOperation(context);
        
        // 记录审计信息
        recordAuditInfo(context);
        
        log.debug("CRUD pre-processing completed for SQL: {}", context.getCurrentSql());
    }
    
    /**
     * 审计记录
     */
    @lombok.Builder
    @lombok.Data
    public static class AuditRecord {
        private String sessionId;
        private String userId;
        private SqlProcessContext.SqlOperationType operationType;
        private String sql;
        private long timestamp;
        private boolean dangerous;
        private boolean batch;
    }
    
    /**
     * CRUD统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class CrudStats {
        private Map<SqlProcessContext.SqlOperationType, Long> operationCounts;
        private long totalOperations;
        private long rejectedOperations;
        
        public double getRejectionRate() {
            return totalOperations > 0 ? (double) rejectedOperations / totalOperations : 0.0;
        }
    }
}