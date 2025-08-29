package org.openjdbcproxy.grpc.server.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.processor.AbstractGrpcMethodProcessor;
import org.openjdbcproxy.grpc.server.processor.StatementServiceMethodName;
import org.openjdbcproxy.grpc.server.processor.ProcessorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 审计日志处理器
 * 为每个 gRPC 方法提供专门的审计日志记录
 * 
 * 配置说明：
 * - 通过 Spring @Value 注解进行配置
 * - 支持按操作类型控制日志记录
 * - 支持 SQL 参数记录和最大长度限制
 */
@Slf4j
@Component
public class AuditLoggingProcessor extends AbstractGrpcMethodProcessor {
    
    // ========== Spring 配置 ==========
    
    /**
     * 是否记录连接操作
     */
    @Value("${ojp.audit.log-connections:true}")
    private boolean logConnections;
    
    /**
     * 是否记录查询操作
     */
    @Value("${ojp.audit.log-queries:true}")
    private boolean logQueries;
    
    /**
     * 是否记录事务操作
     */
    @Value("${ojp.audit.log-transactions:true}")
    private boolean logTransactions;
    
    /**
     * 是否记录 LOB 操作
     */
    @Value("${ojp.audit.log-lob-operations:false}")
    private boolean logLobOperations;
    
    /**
     * 是否记录资源调用
     */
    @Value("${ojp.audit.log-resource-calls:false}")
    private boolean logResourceCalls;
    
    /**
     * 是否记录 SQL 参数
     */
    @Value("${ojp.audit.log-sql-parameters:false}")
    private boolean logSqlParameters;
    
    /**
     * SQL 日志最大长度
     */
    @Value("${ojp.audit.max-sql-length:1000}")
    private int maxSqlLength;

    // 审计日志专用的 Logger
    private static final org.slf4j.Logger auditLog = org.slf4j.LoggerFactory.getLogger("AUDIT");

    @Override
    public boolean supports(StatementServiceMethodName methodType) {
        return shouldLogMethod(methodType);
    }

    @Override
    public int getOrder() {
        return 20; // 在性能监控之后执行
    }



    /**
     * 判断是否应该记录该方法的审计日志
     */
    private boolean shouldLogMethod(StatementServiceMethodName methodType) {
        return switch (methodType) {
            case CONNECT, TERMINATE_SESSION -> logConnections;
            case EXECUTE_QUERY, EXECUTE_UPDATE, FETCH_NEXT_ROWS -> logQueries;
            case START_TRANSACTION, COMMIT_TRANSACTION, ROLLBACK_TRANSACTION -> logTransactions;
            case CREATE_LOB, READ_LOB -> logLobOperations;
            case CALL_RESOURCE -> logResourceCalls;
        };
    }

    // ========== CONNECT 方法处理 ==========
    
    @Override
    public void preConnect(ProcessorContext<?, ?> context) {
        
        safeExecute("preConnect", () -> {
            auditLog.info("CONNECTION_ATTEMPT - Session: {}, User: {}, Database: {}", 
                    getSessionId(context),
                    getUserInfo(context),
                    getDatabaseInfo(context));
        });
    }

    @Override
    public void postConnect(ProcessorContext<?, ?> context) {
        
        safeExecute("postConnect", () -> {
            auditLog.info("CONNECTION_SUCCESS - Session: {}, User: {}, Database: {}, Duration: {}ms", 
                    getSessionId(context),
                    getUserInfo(context),
                    getDatabaseInfo(context),
                    getOperationDuration(context));
        });
    }

    @Override
    public void onConnectException(ProcessorContext<?, ?> context) {
        if (!logConnections) return;
        
        safeExecute("onConnectException", () -> {
            auditLog.error("CONNECTION_FAILED - Session: {}, User: {}, Database: {}, Duration: {}ms, Error: {}", 
                    getSessionId(context),
                    getUserInfo(context),
                    getDatabaseInfo(context),
                    getOperationDuration(context),
                    sanitizeErrorMessage(context.getException()));
        });
    }

    // ========== EXECUTE_QUERY 方法处理 ==========
    
    @Override
    public void preExecuteQuery(ProcessorContext<?, ?> context) {
        if (!logQueries) return;
        
        safeExecute("preExecuteQuery", () -> {
            String sql = getSqlFromContext(context);
            auditLog.info("QUERY_ATTEMPT - Session: {}, User: {}, SQL: {}", 
                    getSessionId(context),
                    getUserInfo(context),
                    sanitizeSql(sql));
        });
    }

    @Override
    public void postExecuteQuery(ProcessorContext<?, ?> context) {
        if (!logQueries) return;
        
        safeExecute("postExecuteQuery", () -> {
            String sql = getSqlFromContext(context);
            Long rowCount = getRowCountFromResponse(context);
            
            auditLog.info("QUERY_SUCCESS - Session: {}, User: {}, SQL: {}, Rows: {}, Duration: {}ms", 
                    getSessionId(context),
                    getUserInfo(context),
                    sanitizeSql(sql),
                    rowCount != null ? rowCount : "unknown",
                    getOperationDuration(context));
        });
    }

    @Override
    public void onExecuteQueryException(ProcessorContext<?, ?> context) {
        if (!logQueries) return;
        
        safeExecute("onExecuteQueryException", () -> {
            String sql = getSqlFromContext(context);
            auditLog.error("QUERY_FAILED - Session: {}, User: {}, SQL: {}, Duration: {}ms, Error: {}", 
                    getSessionId(context),
                    getUserInfo(context),
                    sanitizeSql(sql),
                    getOperationDuration(context),
                    sanitizeErrorMessage(context.getException()));
        });
    }

    // ========== EXECUTE_UPDATE 方法处理 ==========
    
    @Override
    public void preExecuteUpdate(ProcessorContext<?, ?> context) {
        if (!logQueries) return;
        
        safeExecute("preExecuteUpdate", () -> {
            String sql = getSqlFromContext(context);
            auditLog.info("UPDATE_ATTEMPT - Session: {}, User: {}, SQL: {}", 
                    getSessionId(context),
                    getUserInfo(context),
                    sanitizeSql(sql));
        });
    }

    @Override
    public void postExecuteUpdate(ProcessorContext<?, ?> context) {
        if (!logQueries) return;
        
        safeExecute("postExecuteUpdate", () -> {
            String sql = getSqlFromContext(context);
            Integer affectedRows = getAffectedRowsFromResponse(context);
            
            auditLog.info("UPDATE_SUCCESS - Session: {}, User: {}, SQL: {}, AffectedRows: {}, Duration: {}ms", 
                    getSessionId(context),
                    getUserInfo(context),
                    sanitizeSql(sql),
                    affectedRows != null ? affectedRows : "unknown",
                    getOperationDuration(context));
        });
    }

    @Override
    public void onExecuteUpdateException(ProcessorContext<?, ?> context) {
        if (!logQueries) return;
        
        safeExecute("onExecuteUpdateException", () -> {
            String sql = getSqlFromContext(context);
            auditLog.error("UPDATE_FAILED - Session: {}, User: {}, SQL: {}, Duration: {}ms, Error: {}", 
                    getSessionId(context),
                    getUserInfo(context),
                    sanitizeSql(sql),
                    getOperationDuration(context),
                    sanitizeErrorMessage(context.getException()));
        });
    }

    // ========== TRANSACTION 方法处理 ==========
    
    @Override
    public void preStartTransaction(ProcessorContext<?, ?> context) {
        if (!logTransactions) return;
        
        safeExecute("preStartTransaction", () -> {
            auditLog.info("TRANSACTION_START_ATTEMPT - Session: {}, User: {}", 
                    getSessionId(context), getUserInfo(context));
        });
    }

    @Override
    public void postStartTransaction(ProcessorContext<?, ?> context) {
        if (!logTransactions) return;
        
        safeExecute("postStartTransaction", () -> {
            auditLog.info("TRANSACTION_START_SUCCESS - Session: {}, User: {}, Duration: {}ms", 
                    getSessionId(context), getUserInfo(context), getOperationDuration(context));
        });
    }

    @Override
    public void preCommitTransaction(ProcessorContext<?, ?> context) {
        if (!logTransactions) return;
        
        safeExecute("preCommitTransaction", () -> {
            auditLog.info("TRANSACTION_COMMIT_ATTEMPT - Session: {}, User: {}", 
                    getSessionId(context), getUserInfo(context));
        });
    }

    @Override
    public void postCommitTransaction(ProcessorContext<?, ?> context) {
        if (!logTransactions) return;
        
        safeExecute("postCommitTransaction", () -> {
            auditLog.info("TRANSACTION_COMMIT_SUCCESS - Session: {}, User: {}, Duration: {}ms", 
                    getSessionId(context), getUserInfo(context), getOperationDuration(context));
        });
    }

    @Override
    public void preRollbackTransaction(ProcessorContext<?, ?> context) {
        if (!logTransactions) return;
        
        safeExecute("preRollbackTransaction", () -> {
            auditLog.info("TRANSACTION_ROLLBACK_ATTEMPT - Session: {}, User: {}", 
                    getSessionId(context), getUserInfo(context));
        });
    }

    @Override
    public void postRollbackTransaction(ProcessorContext<?, ?> context) {
        if (!logTransactions) return;
        
        safeExecute("postRollbackTransaction", () -> {
            auditLog.info("TRANSACTION_ROLLBACK_SUCCESS - Session: {}, User: {}, Duration: {}ms", 
                    getSessionId(context), getUserInfo(context), getOperationDuration(context));
        });
    }

    // ========== SESSION 方法处理 ==========
    
    @Override
    public void preTerminateSession(ProcessorContext<?, ?> context) {
        if (!logConnections) return;
        
        safeExecute("preTerminateSession", () -> {
            auditLog.info("SESSION_TERMINATE_ATTEMPT - Session: {}, User: {}", 
                    getSessionId(context), getUserInfo(context));
        });
    }

    @Override
    public void postTerminateSession(ProcessorContext<?, ?> context) {
        if (!logConnections) return;
        
        safeExecute("postTerminateSession", () -> {
            auditLog.info("SESSION_TERMINATE_SUCCESS - Session: {}, User: {}, Duration: {}ms", 
                    getSessionId(context), getUserInfo(context), getOperationDuration(context));
        });
    }

    // ========== LOB 方法处理 ==========
    
    @Override
    public void preCreateLob(ProcessorContext<?, ?> context) {
        if (!logLobOperations) return;
        
        safeExecute("preCreateLob", () -> {
            auditLog.info("LOB_CREATE_ATTEMPT - Session: {}, User: {}", 
                    getSessionId(context), getUserInfo(context));
        });
    }

    @Override
    public void postCreateLob(ProcessorContext<?, ?> context) {
        if (!logLobOperations) return;
        
        safeExecute("postCreateLob", () -> {
            auditLog.info("LOB_CREATE_SUCCESS - Session: {}, User: {}, Duration: {}ms", 
                    getSessionId(context), getUserInfo(context), getOperationDuration(context));
        });
    }

    @Override
    public void preReadLob(ProcessorContext<?, ?> context) {
        if (!logLobOperations) return;
        
        safeExecute("preReadLob", () -> {
            auditLog.info("LOB_READ_ATTEMPT - Session: {}, User: {}", 
                    getSessionId(context), getUserInfo(context));
        });
    }

    @Override
    public void postReadLob(ProcessorContext<?, ?> context) {
        if (!logLobOperations) return;
        
        safeExecute("postReadLob", () -> {
            auditLog.info("LOB_READ_SUCCESS - Session: {}, User: {}, Duration: {}ms", 
                    getSessionId(context), getUserInfo(context), getOperationDuration(context));
        });
    }

    // ========== 工具方法 ==========
    
    private String getSessionId(ProcessorContext<?, ?> context) {
        return context.getSessionId();
    }
    
    private String getUserInfo(ProcessorContext<?, ?> context) {
        // TODO: 从 context 中提取用户信息
        return "system"; // 暂时返回默认值
    }
    
    private String getDatabaseInfo(ProcessorContext<?, ?> context) {
        // TODO: 从 context 中提取数据库信息
        return "default"; // 暂时返回默认值
    }
    
    private String getSqlFromContext(ProcessorContext<?, ?> context) {
        // TODO: 从 request 中提取 SQL 语句
        return "SELECT ..."; // 暂时返回占位符
    }
    
    private Long getRowCountFromResponse(ProcessorContext<?, ?> context) {
        // TODO: 从 response 中提取行数
        return null;
    }
    
    private Integer getAffectedRowsFromResponse(ProcessorContext<?, ?> context) {
        // TODO: 从 response 中提取影响行数
        return null;
    }
    
    private Long getOperationDuration(ProcessorContext<?, ?> context) {
        if (context.getEndTime() > 0 && context.getStartTime() > 0) {
            return context.getEndTime() - context.getStartTime();
        }
        return 0L;
    }
    
    /**
     * 对 SQL 进行安全处理，移除敏感信息
     */
    private String sanitizeSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "empty";
        }
        
        if (!logSqlParameters) {
            // 移除参数值，只保留 SQL 结构
            return sql.replaceAll("'[^']*'", "'?'")
                      .replaceAll("\\b\\d+\\b", "?");
        }
        
        // 限制 SQL 长度
        if (sql.length() > maxSqlLength) {
            return sql.substring(0, maxSqlLength) + "...";
        }
        
        return sql;
    }
    
    /**
     * 对错误消息进行安全处理
     */
    private String sanitizeErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        
        String message = throwable.getMessage();
        if (message == null) {
            message = throwable.getClass().getSimpleName();
        }
        
        // 限制错误消息长度
        if (message.length() > 500) {
            return message.substring(0, 500) + "...";
        }
        
        return message;
    }
    
}