package org.openjdbcproxy.grpc.server.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.processor.AbstractGrpcMethodProcessor;
import org.openjdbcproxy.grpc.server.processor.StatementServiceMethodName;
import org.openjdbcproxy.grpc.server.processor.ProcessorContext;
import org.openjdbcproxy.grpc.server.utils.SqlUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SQL 分析处理器
 * 专门负责分析 SQL 语句的类型、复杂度等特征
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ojp.processor.sql-analysis.enabled", havingValue = "true", matchIfMissing = true)
public class SqlAnalysisProcessor extends AbstractGrpcMethodProcessor {

    @Override
    public String getName() {
        return "SqlAnalysis";
    }

    @Override
    public int getOrder() {
        // 在性能监控之前执行，为其他处理器提供 SQL 分析信息
        return 100;
    }

    @Override
    public boolean supports(StatementServiceMethodName methodType) {
        // 支持所有涉及 SQL 的方法
        return methodType == StatementServiceMethodName.EXECUTE_QUERY ||
               methodType == StatementServiceMethodName.EXECUTE_UPDATE;
    }

    // ========== EXECUTE_QUERY 方法处理 ==========
    
    @Override
    public void preExecuteQuery(ProcessorContext<?, ?> context) {
        safeExecute("preExecuteQuery", () -> {
            analyzeSql(context);
        });
    }

    // ========== EXECUTE_UPDATE 方法处理 ==========
    
    @Override
    public void preExecuteUpdate(ProcessorContext<?, ?> context) {
        safeExecute("preExecuteUpdate", () -> {
            analyzeSql(context);
        });
    }



    /**
     * 分析 SQL 语句
     */
    private void analyzeSql(ProcessorContext<?, ?> context) {
        try {
            // 从请求中提取 SQL 语句
            String sql = extractSqlFromRequest(context);
            if (sql == null || sql.trim().isEmpty()) {
                recordMetric(context, "sqlType", SqlUtils.SqlType.UNKNOWN);
                recordMetric(context, "sqlTypeDescription", "Unknown SQL");
                return;
            }

            // 使用 SqlUtils 分析 SQL 类型
            SqlUtils.SqlType sqlType = SqlUtils.getSqlType(sql);
            
            // 记录分析结果到上下文，供其他处理器使用
            recordMetric(context, "sqlType", sqlType);
            recordMetric(context, "sqlTypeDescription", sqlType.getDescription());
            recordMetric(context, "originalSql", sql);
            recordMetric(context, "sqlLength", sql.length());
            
            // 分析 SQL 复杂度
            analyzeSqlComplexity(context, sql, sqlType);
            
            if (log.isDebugEnabled()) {
                log.debug("[Session: {}] SQL analysis completed - Type: {}, Length: {}", 
                    extractSessionId(context), sqlType.getDescription(), sql.length());
            }
            
        } catch (Exception e) {
            String sessionId = extractSessionId(context);
            log.warn("Failed to analyze SQL in session {}: {}", 
                sessionId, e.getMessage());
            recordMetric(context, "sqlType", SqlUtils.SqlType.UNKNOWN);
            recordMetric(context, "sqlAnalysisError", e.getMessage());
        }
    }

    /**
     * 分析 SQL 复杂度
     */
    private void analyzeSqlComplexity(ProcessorContext<?, ?> context, String sql, SqlUtils.SqlType sqlType) {
        try {
            String normalizedSql = sql.toLowerCase().trim();
            
            // 基础复杂度评分
            int complexityScore = calculateComplexityScore(normalizedSql, sqlType);
            recordMetric(context, "sqlComplexityScore", complexityScore);
            
            // 判断复杂度等级
            String complexityLevel = determineComplexityLevel(complexityScore);
            recordMetric(context, "sqlComplexityLevel", complexityLevel);
            
            // 特殊 SQL 特征检测
            detectSpecialFeatures(context, normalizedSql);
            
        } catch (Exception e) {
            log.debug("Failed to analyze SQL complexity: {}", e.getMessage());
            recordMetric(context, "sqlComplexityScore", 0);
            recordMetric(context, "sqlComplexityLevel", "UNKNOWN");
        }
    }

    /**
     * 计算 SQL 复杂度评分
     */
    private int calculateComplexityScore(String normalizedSql, SqlUtils.SqlType sqlType) {
        int score = 0;
        
        // 基础分数（根据 SQL 类型）
        switch (sqlType) {
            case SELECT:
                score += 2;
                break;
            case DML:
                score += 3;
                break;
            case DDL:
                score += 5;
                break;
            case TRANSACTION:
                score += 1;
                break;
            default:
                score += 1;
        }
        
        // JOIN 复杂度
        score += countOccurrences(normalizedSql, "join") * 2;
        score += countOccurrences(normalizedSql, "left join") * 1;
        score += countOccurrences(normalizedSql, "right join") * 1;
        score += countOccurrences(normalizedSql, "inner join") * 1;
        score += countOccurrences(normalizedSql, "outer join") * 2;
        
        // 子查询复杂度
        score += countOccurrences(normalizedSql, "select") * 1; // 每个子查询
        
        // 聚合函数
        score += countOccurrences(normalizedSql, "group by") * 2;
        score += countOccurrences(normalizedSql, "having") * 2;
        score += countOccurrences(normalizedSql, "order by") * 1;
        
        // 条件复杂度
        score += countOccurrences(normalizedSql, "where") * 1;
        score += countOccurrences(normalizedSql, "and") * 1;
        score += countOccurrences(normalizedSql, "or") * 2;
        
        // 函数调用
        score += countOccurrences(normalizedSql, "count(") * 1;
        score += countOccurrences(normalizedSql, "sum(") * 1;
        score += countOccurrences(normalizedSql, "avg(") * 1;
        score += countOccurrences(normalizedSql, "max(") * 1;
        score += countOccurrences(normalizedSql, "min(") * 1;
        
        return score;
    }

    /**
     * 判断复杂度等级
     */
    private String determineComplexityLevel(int score) {
        if (score <= 5) {
            return "SIMPLE";
        } else if (score <= 15) {
            return "MEDIUM";
        } else if (score <= 30) {
            return "COMPLEX";
        } else {
            return "VERY_COMPLEX";
        }
    }

    /**
     * 检测特殊 SQL 特征
     */
    private void detectSpecialFeatures(ProcessorContext<?, ?> context, String normalizedSql) {
        // 检测是否包含事务相关语句
        boolean hasTransaction = normalizedSql.contains("begin") || 
                               normalizedSql.contains("commit") || 
                               normalizedSql.contains("rollback");
        recordMetric(context, "hasTransaction", hasTransaction);
        
        // 检测是否为分析型查询
        boolean isAnalytical = normalizedSql.contains("group by") || 
                             normalizedSql.contains("having") || 
                             normalizedSql.contains("window") ||
                             normalizedSql.contains("partition by");
        recordMetric(context, "isAnalytical", isAnalytical);
        
        // 检测是否包含递归查询
        boolean isRecursive = normalizedSql.contains("with recursive") || 
                            normalizedSql.contains("connect by");
        recordMetric(context, "isRecursive", isRecursive);
        
        // 检测是否为批量操作
        boolean isBulkOperation = normalizedSql.contains("bulk") || 
                                normalizedSql.contains("batch") ||
                                (normalizedSql.contains("insert") && normalizedSql.contains("values") && 
                                 countOccurrences(normalizedSql, "values") > 1);
        recordMetric(context, "isBulkOperation", isBulkOperation);
    }

    /**
     * 计算字符串出现次数
     */
    private int countOccurrences(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * 从请求中提取 SQL 语句
     * 这里需要根据实际的 gRPC 请求结构来实现
     */
    private String extractSqlFromRequest(ProcessorContext<?, ?> context) {
        try {
            Object request = context.getRequest();
            if (request == null) {
                return null;
            }
            
            // 这里需要根据实际的 gRPC 请求结构来提取 SQL
            // 例如：如果是 StatementRequest，可能有 getSql() 方法
            String requestStr = request.toString();
            
            // 简单的提取逻辑，实际应用中需要根据具体的 protobuf 结构调整
            if (requestStr.contains("sql=")) {
                int start = requestStr.indexOf("sql=") + 4;
                int end = requestStr.indexOf(",", start);
                if (end == -1) {
                    end = requestStr.indexOf("}", start);
                }
                if (end > start) {
                    return requestStr.substring(start, end).trim().replaceAll("^\"|\"$", "");
                }
            }
            
            return null;
        } catch (Exception e) {
            log.debug("Failed to extract SQL from request: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 提取会话 ID
     */
    private String extractSessionId(ProcessorContext<?, ?> context) {
        try {
            Object sessionInfo = context.getAttribute("sessionInfo");
            if (sessionInfo != null) {
                return sessionInfo.toString();
            }
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
