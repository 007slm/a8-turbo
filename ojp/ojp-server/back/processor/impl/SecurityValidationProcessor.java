package org.openjdbcproxy.grpc.server.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.processor.AbstractGrpcMethodProcessor;
import org.openjdbcproxy.grpc.server.processor.StatementServiceMethodName;
import org.openjdbcproxy.grpc.server.processor.ProcessorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 安全验证处理器
 * 为每个 gRPC 方法提供专门的安全验证逻辑
 * 
 * 配置说明：
 * - 默认禁用，需要明确启用
 * - 支持 SQL 注入检测、危险操作检测、频率限制等
 * - 通过 Spring @Value 注解进行配置
 */
@Slf4j
@Component
public class SecurityValidationProcessor extends AbstractGrpcMethodProcessor {
    
    // ========== Spring 配置 ==========
    

    
    /**
     * 是否启用 SQL 注入检测
     */
    @Value("${ojp.security.sql-injection-detection:true}")
    private boolean enableSqlInjectionDetection;
    
    /**
     * 是否阻止危险操作
     */
    @Value("${ojp.security.block-dangerous-operations:true}")
    private boolean blockDangerousOperations;
    
    /**
     * 是否启用频率限制
     */
    @Value("${ojp.security.rate-limit:false}")
    private boolean enableRateLimit;
    
    /**
     * 最大请求大小（字节）
     */
    @Value("${ojp.security.max-request-size:#{10 * 1024 * 1024}}")
    private long maxRequestSize; // 10MB
    
    /**
     * 最大 LOB 大小（字节）
     */
    @Value("${ojp.security.max-lob-size:#{100 * 1024 * 1024}}")
    private long maxLobSize; // 100MB
    
    /**
     * 最大并发事务数
     */
    @Value("${ojp.security.max-concurrent-transactions:100}")
    private int maxConcurrentTransactions;

    // SQL 注入检测模式
    private static final Set<Pattern> SQL_INJECTION_PATTERNS = Set.of(
            Pattern.compile("(?i).*\\bunion\\s+select\\b.*"),
            Pattern.compile("(?i).*\\bor\\s+1\\s*=\\s*1\\b.*"),
            Pattern.compile("(?i).*\\bdrop\\s+table\\b.*"),
            Pattern.compile("(?i).*\\bdelete\\s+from\\b.*"),
            Pattern.compile("(?i).*\\binsert\\s+into\\b.*"),
            Pattern.compile("(?i).*\\bupdate\\s+.*\\bset\\b.*"),
            Pattern.compile("(?i).*\\bexec\\s*\\(.*"),
            Pattern.compile("(?i).*\\bscript\\b.*"),
            Pattern.compile("(?i).*\\bjavascript\\b.*"),
            Pattern.compile("(?i).*['\"]\\s*;\\s*--.*")
    );

    // 危险操作模式
    private static final Set<Pattern> DANGEROUS_OPERATION_PATTERNS = Set.of(
            Pattern.compile("(?i).*\\btruncate\\s+table\\b.*"),
            Pattern.compile("(?i).*\\balter\\s+table\\b.*"),
            Pattern.compile("(?i).*\\bcreate\\s+table\\b.*"),
            Pattern.compile("(?i).*\\bdrop\\s+database\\b.*"),
            Pattern.compile("(?i).*\\bshutdown\\b.*"),
            Pattern.compile("(?i).*\\bxp_cmdshell\\b.*")
    );

    @Override
    public boolean supports(StatementServiceMethodName methodType) {
        return true; // 安全验证应用于所有方法
    }

    @Override
    public int getOrder() {
        return 5; // 安全验证应该最先执行
    }



    // ========== CONNECT 方法处理 ==========
    
    @Override
    public void preConnect(ProcessorContext<?, ?> context) {
        safeExecute("preConnect", () -> {
            // 验证连接参数
            validateConnectionRequest(context);
            
            // 检查连接频率限制
            if (enableRateLimit) {
                checkConnectionRateLimit(context);
            }
        });
    }

    // ========== EXECUTE_QUERY 方法处理 ==========
    
    @Override
    public void preExecuteQuery(ProcessorContext<?, ?> context) {
        safeExecute("preExecuteQuery", () -> {
            String sql = getSqlFromRequest(context);
            
            // SQL 注入检测
            if (enableSqlInjectionDetection) {
                detectSqlInjection(sql, context);
            }
            
            // 查询权限验证
            validateQueryPermissions(sql, context);
            
            // 请求大小验证
            validateRequestSize(context);
        });
    }

    // ========== EXECUTE_UPDATE 方法处理 ==========
    
    @Override
    public void preExecuteUpdate(ProcessorContext<?, ?> context) {
        safeExecute("preExecuteUpdate", () -> {
            String sql = getSqlFromRequest(context);
            
            // SQL 注入检测
            if (enableSqlInjectionDetection) {
                detectSqlInjection(sql, context);
            }
            
            // 危险操作检测
            if (blockDangerousOperations) {
                detectDangerousOperations(sql, context);
            }
            
            // 更新权限验证
            validateUpdatePermissions(sql, context);
            
            // 请求大小验证
            validateRequestSize(context);
        });
    }

    // ========== TRANSACTION 方法处理 ==========
    
    @Override
    public void preStartTransaction(ProcessorContext<?, ?> context) {
        safeExecute("preStartTransaction", () -> {
            // 事务权限验证
            validateTransactionPermissions(context);
            
            // 检查并发事务数量限制
            checkConcurrentTransactionLimit(context);
        });
    }

    @Override
    public void preCommitTransaction(ProcessorContext<?, ?> context) {
        safeExecute("preCommitTransaction", () -> {
            // 验证事务状态
            validateTransactionState(context);
        });
    }

    @Override
    public void preRollbackTransaction(ProcessorContext<?, ?> context) {
        safeExecute("preRollbackTransaction", () -> {
            // 验证事务状态
            validateTransactionState(context);
        });
    }

    // ========== LOB 方法处理 ==========
    
    @Override
    public void preCreateLob(ProcessorContext<?, ?> context) {
        safeExecute("preCreateLob", () -> {
            // LOB 大小限制检查
            validateLobSize(context);
            
            // LOB 权限验证
            validateLobPermissions(context);
        });
    }

    @Override
    public void preReadLob(ProcessorContext<?, ?> context) {
        safeExecute("preReadLob", () -> {
            // LOB 读取权限验证
            validateLobReadPermissions(context);
        });
    }

    // ========== 验证方法实现 ==========
    
    /**
     * 验证连接请求
     */
    private void validateConnectionRequest(ProcessorContext<?, ?> context) {
        // TODO: 实现连接参数验证逻辑
        log.debug("Validating connection request for session: {}", context.getSessionId());
    }
    
    /**
     * 检查连接频率限制
     */
    private void checkConnectionRateLimit(ProcessorContext<?, ?> context) {
        // TODO: 实现连接频率限制检查
        log.debug("Checking connection rate limit");
    }
    
    /**
     * SQL 注入检测
     */
    private void detectSqlInjection(String sql, ProcessorContext<?, ?> context) {
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }
        
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(sql).matches()) {
                String errorMsg = "Potential SQL injection detected: " + sql.substring(0, Math.min(sql.length(), 100));
                log.error("Security violation - SQL injection attempt detected from session: {}, SQL: {}", 
                         getSessionId(context), errorMsg);
                throw new SecurityException("SQL injection attempt detected");
            }
        }
    }
    
    /**
     * 危险操作检测
     */
    private void detectDangerousOperations(String sql, ProcessorContext<?, ?> context) {
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }
        
        for (Pattern pattern : DANGEROUS_OPERATION_PATTERNS) {
            if (pattern.matcher(sql).matches()) {
                String errorMsg = "Dangerous operation detected: " + sql.substring(0, Math.min(sql.length(), 100));
                log.error("Security violation - Dangerous operation attempt from session: {}, SQL: {}", 
                         getSessionId(context), errorMsg);
                throw new SecurityException("Dangerous operation not allowed");
            }
        }
    }
    
    /**
     * 验证查询权限
     */
    private void validateQueryPermissions(String sql, ProcessorContext<?, ?> context) {
        // TODO: 实现查询权限验证逻辑
        log.debug("Validating query permissions for session: {}", getSessionId(context));
    }
    
    /**
     * 验证更新权限
     */
    private void validateUpdatePermissions(String sql, ProcessorContext<?, ?> context) {
        // TODO: 实现更新权限验证逻辑
        log.debug("Validating update permissions for session: {}", getSessionId(context));
    }
    
    /**
     * 验证事务权限
     */
    private void validateTransactionPermissions(ProcessorContext<?, ?> context) {
        // TODO: 实现事务权限验证逻辑
        log.debug("Validating transaction permissions for session: {}", getSessionId(context));
    }
    
    /**
     * 检查并发事务数量限制
     */
    private void checkConcurrentTransactionLimit(ProcessorContext<?, ?> context) {
        // TODO: 实现并发事务数量限制检查
        log.debug("Checking concurrent transaction limit");
    }
    
    /**
     * 验证事务状态
     */
    private void validateTransactionState(ProcessorContext<?, ?> context) {
        // TODO: 实现事务状态验证逻辑
        log.debug("Validating transaction state for session: {}", getSessionId(context));
    }
    
    /**
     * 验证 LOB 大小
     */
    private void validateLobSize(ProcessorContext<?, ?> context) {
        // TODO: 实现 LOB 大小验证逻辑
        log.debug("Validating LOB size, max allowed: {} bytes", maxLobSize);
    }
    
    /**
     * 验证 LOB 权限
     */
    private void validateLobPermissions(ProcessorContext<?, ?> context) {
        // TODO: 实现 LOB 权限验证逻辑
        log.debug("Validating LOB permissions for session: {}", getSessionId(context));
    }
    
    /**
     * 验证 LOB 读取权限
     */
    private void validateLobReadPermissions(ProcessorContext<?, ?> context) {
        // TODO: 实现 LOB 读取权限验证逻辑
        log.debug("Validating LOB read permissions for session: {}", getSessionId(context));
    }
    
    /**
     * 验证请求大小
     */
    private void validateRequestSize(ProcessorContext<?, ?> context) {
        // TODO: 实现请求大小验证逻辑
        // 使用配置的最大请求大小
        log.debug("Validating request size, max allowed: {} bytes", maxRequestSize);
    }
    
    /**
     * 从请求中提取 SQL
     */
    private String getSqlFromRequest(ProcessorContext<?, ?> context) {
        // TODO: 根据实际的请求类型提取 SQL
        return "SELECT ..."; // 暂时返回占位符
    }
    
    /**
     * 获取会话ID
     */
    private String getSessionId(ProcessorContext<?, ?> context) {
        return context.getSessionId();
    }
    

}