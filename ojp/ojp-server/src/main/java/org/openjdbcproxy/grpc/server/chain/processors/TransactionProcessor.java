package org.openjdbcproxy.grpc.server.chain.processors;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.chain.AbstractSqlProcessor;

import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;
import org.openjdbcproxy.grpc.server.smartcache.transaction.TransactionStateTracker;

import java.sql.SQLException;
import java.util.Set;

/**
 * 事务处理器
 * 
 * 负责事务相关的处理：
 * 1. 跟踪事务状态，确保缓存安全性
 * 2. 记录写操作，影响缓存决策
 * 3. 事务权限控制
 * 4. 事务隔离级别检查
 */
@Slf4j
public class TransactionProcessor extends AbstractSqlProcessor {
    
    private static final String PROCESSOR_NAME = "TransactionProcessor";
    
    private final TransactionStateTracker transactionTracker;
    
    public TransactionProcessor(TransactionStateTracker transactionTracker) {
        this.transactionTracker = transactionTracker != null ? transactionTracker : new TransactionStateTracker();
    }
    
    public TransactionProcessor() {
        this(new TransactionStateTracker());
    }
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        String sessionId = context.getSessionId();
        
        // 处理不同类型的SQL操作
        switch (context.getOperationType()) {
            case INSERT:
            case UPDATE: 
            case DELETE:
            case TRUNCATE:
                handleWriteOperation(context, sessionId);
                break;
                
            case SELECT:
                handleReadOperation(context, sessionId);
                break;
                
            default:
                // DDL等其他操作暂不特殊处理
                break;
        }
        
        // 记录操作信息到上下文
        recordTransactionState(context, sessionId);
        
        return false; // 继续传递给下一个处理器
    }
    
    /**
     * 处理写操作
     */
    private void handleWriteOperation(SqlProcessContext context, String sessionId) throws SQLException {
        // 检查事务权限
        SqlProcessContext.UserContext userContext = getUserContext(context);
        if (!hasWritePermission(userContext, context)) {
            throw new SQLException("User does not have permission to perform write operations");
        }
        
        // 标记当前事务有写操作
        if (transactionTracker.isInTransaction(sessionId)) {
            transactionTracker.markWrite(sessionId);
            log.debug("Marked write operation for session: {}", sessionId);
        }
        
        // 记录到上下文，影响后续处理器决策（如缓存跳过）
        context.setAttribute("has_write_operation", true);
        context.setAttribute("write_operation_type", context.getOperationType());
    }
    
    /**
     * 处理读操作
     */
    private void handleReadOperation(SqlProcessContext context, String sessionId) {
        // 检查当前事务是否有写操作
        boolean hasWrites = transactionTracker.hasWrites(sessionId);
        if (hasWrites) {
            // 事务中有写操作，标记为不能使用缓存
            context.setAttribute("transaction_has_writes", true);
            log.debug("Transaction has writes, caching will be skipped for session: {}", sessionId);
        }
        
        // 记录事务状态
        boolean inTransaction = transactionTracker.isInTransaction(sessionId);
        context.setAttribute("in_transaction", inTransaction);
    }
    
    /**
     * 检查用户是否有写权限
     */
    private boolean hasWritePermission(SqlProcessContext.UserContext userContext, SqlProcessContext context) {
        if (userContext == null) {
            return true; // 默认允许，由其他处理器控制权限
        }
        
        // 检查角色权限
        if (userContext.getRoles() != null) {
            // 只读用户不能执行写操作
            if (userContext.getRoles().contains("READ_ONLY")) {
                return false;
            }
            
            // 特定表的写权限控制
            if (context.getParseInfo() != null && context.getParseInfo().getTableNames() != null) {
                for (String tableName : context.getParseInfo().getTableNames()) {
                    if (tableName.startsWith("system_") || tableName.startsWith("audit_")) {
                        // 系统表需要管理员权限
                        return userContext.getRoles().contains("ADMIN");
                    }
                }
            }
        }
        
        return true; // 默认允许
    }
    
    /**
     * 记录事务状态到上下文
     */
    private void recordTransactionState(SqlProcessContext context, String sessionId) {
        TransactionStateTracker.TransactionState state = transactionTracker.getTransactionState(sessionId);
        if (state != null) {
            context.setAttribute("transaction_state", state);
            context.setAttribute("transaction_duration", state.getTransactionDuration());
            
            // 长时间运行的事务警告
            if (state.getTransactionDuration() > 30000) { // 30秒
                log.warn("Long running transaction detected for session: {}, duration: {}ms", 
                        sessionId, state.getTransactionDuration());
                context.setAttribute("long_running_transaction", true);
            }
        }
    }
    
    /**
     * 处理事务开始
     */
    public void handleTransactionStart(String sessionId) {
        transactionTracker.startTransaction(sessionId);
        log.info("Transaction started for session: {}", sessionId);
    }
    
    /**
     * 处理事务提交
     */
    public void handleTransactionCommit(String sessionId) {
        transactionTracker.endTransaction(sessionId);
        log.info("Transaction committed for session: {}", sessionId);
    }
    
    /**
     * 处理事务回滚
     */
    public void handleTransactionRollback(String sessionId) {
        transactionTracker.endTransaction(sessionId);
        log.info("Transaction rolled back for session: {}", sessionId);
    }
    
    /**
     * 获取事务统计信息
     */
    public TransactionStats getTransactionStats() {
        return TransactionStats.builder()
                .activeTransactionCount(transactionTracker.getActiveTransactionCount())
                .transactionsWithWritesCount(transactionTracker.getTransactionsWithWritesCount())
                .build();
    }
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public int getPriority() {
        return 110; // 最高优先级，在所有处理器之前执行
    }
    
    @Override
    public Set<SqlProcessContext.SqlOperationType> getSupportedOperations() {
        return Set.of(SqlProcessContext.SqlOperationType.values()); // 支持所有操作类型
    }
    
    /**
     * 事务统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class TransactionStats {
        private long activeTransactionCount;
        private long transactionsWithWritesCount;
        
        @Override
        public String toString() {
            return String.format("TransactionStats{active=%d, withWrites=%d}", 
                                activeTransactionCount, transactionsWithWritesCount);
        }
    }
}