package org.openjdbcproxy.grpc.server.chain.processors;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;
import org.openjdbcproxy.grpc.server.chain.SqlProcessor;
import org.openjdbcproxy.grpc.server.chain.PreProcessor;
import org.openjdbcproxy.grpc.server.chain.PostProcessor;

import java.sql.SQLException;
import java.util.Set;

/**
 * SQL处理器抽象基类
 * 
 * 提供责任链模式的基础实现，包括：
 * 1. 责任链传递逻辑
 * 2. 操作类型过滤
 * 3. 性能监控
 * 4. 异常处理
 */
@Slf4j
public abstract class AbstractSqlProcessor implements SqlProcessor {
    
    protected SqlProcessor next;
    
    @Override
    public final boolean process(SqlProcessContext context) throws SQLException {
        // 检查是否支持当前SQL操作类型
        if (!getSupportedOperations().contains(context.getOperationType())) {
            log.debug("Processor {} does not support operation type {}, skipping", 
                     getProcessorName(), context.getOperationType());
            return processNext(context);
        }
        
        // 记录处理开始时间
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Processing SQL with {}: {}", getProcessorName(), context.getCurrentSql());
            
            // 执行前处理（如果实现了PreProcessor接口）
            if (this instanceof PreProcessor) {
                try {
                    ((PreProcessor) this).preProcess(context);
                    log.debug("Pre-processing completed for processor: {}", getProcessorName());
                } catch (Exception e) {
                    log.warn("Pre-processing failed for processor {}: {}", getProcessorName(), e.getMessage());
                }
            }
            
            // 执行具体的处理逻辑
            boolean handled = doProcess(context);
            
            // 记录处理时间
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Processor {} completed in {}ms, handled: {}", 
                     getProcessorName(), processingTime, handled);
            
            // 如果已处理完成，直接返回
            if (handled || context.isCompleted()) {
                return true;
            }
            
            // 继续传递给下一个处理器
            return processNext(context);
            
        } catch (Exception e) {
            log.error("Error in processor {}: {}", getProcessorName(), e.getMessage(), e);
            context.setError(e);
            throw e;
        }
    }
    
    /**
     * 具体的处理逻辑，由子类实现
     * 默认返回false，表示继续传递给下一个处理器
     * 
     * @param context SQL处理上下文
     * @return true 如果已完成处理，false 如果需要继续传递
     * @throws SQLException 处理过程中的SQL异常
     */
    public boolean doProcess(SqlProcessContext context) throws SQLException {
        // 默认实现：继续传递给下一个处理器
        return false;
    }
    
    /**
     * 传递给下一个处理器
     */
    public boolean processNext(SqlProcessContext context) throws SQLException {
        if (next != null) {
            return next.process(context);
        }
        return false;
    }
    
    /**
     * 执行后处理（如果实现了PostProcessor接口）
     * 这个方法在责任链处理完成后被调用
     */
    public void executePostProcess(SqlProcessContext context) {
        if (this instanceof PostProcessor) {
            try {
                ((PostProcessor) this).postProcess(context);
                log.debug("Post-processing completed for processor: {}", getProcessorName());
            } catch (Exception e) {
                log.warn("Post-processing failed for processor {}: {}", getProcessorName(), e.getMessage());
            }
        }
    }
    
    @Override
    public void setNext(SqlProcessor next) {
        this.next = next;
    }
    
    /**
     * 默认支持所有操作类型，子类可以重写以限制支持的操作类型
     */
    @Override
    public Set<SqlProcessContext.SqlOperationType> getSupportedOperations() {
        return Set.of(SqlProcessContext.SqlOperationType.values());
    }
    
    /**
     * 检查SQL是否匹配指定的表名模式
     */
    protected boolean matchesTablePattern(SqlProcessContext context, String tablePattern) {
        if (context.getParseInfo() == null || context.getParseInfo().getTableNames() == null) {
            return false;
        }
        
        return context.getParseInfo().getTableNames().stream()
                .anyMatch(tableName -> tableName.matches(tablePattern));
    }
    
    /**
     * 向SQL的WHERE子句添加条件
     */
    protected String addWhereCondition(String sql, String condition) {
        String lowerSql = sql.toLowerCase();
        
        if (lowerSql.contains(" where ")) {
            // 已有WHERE子句，添加AND条件
            int whereIndex = lowerSql.indexOf(" where ");
            int nextClauseIndex = findNextClauseIndex(lowerSql, whereIndex);
            
            if (nextClauseIndex > 0) {
                // 在WHERE和下一个子句之间插入条件
                return sql.substring(0, nextClauseIndex) + " AND (" + condition + ")" + 
                       sql.substring(nextClauseIndex);
            } else {
                // WHERE是最后一个子句
                return sql + " AND (" + condition + ")";
            }
        } else {
            // 没有WHERE子句，添加WHERE
            int insertIndex = findWhereInsertPoint(lowerSql);
            if (insertIndex > 0) {
                return sql.substring(0, insertIndex) + " WHERE " + condition + " " + 
                       sql.substring(insertIndex);
            } else {
                return sql + " WHERE " + condition;
            }
        }
    }
    
    /**
     * 查找下一个SQL子句的位置（ORDER BY, GROUP BY, HAVING, LIMIT等）
     */
    private int findNextClauseIndex(String lowerSql, int fromIndex) {
        String[] clauses = {" order by ", " group by ", " having ", " limit ", " offset "};
        int minIndex = Integer.MAX_VALUE;
        
        for (String clause : clauses) {
            int index = lowerSql.indexOf(clause, fromIndex);
            if (index > 0 && index < minIndex) {
                minIndex = index;
            }
        }
        
        return minIndex == Integer.MAX_VALUE ? -1 : minIndex;
    }
    
    /**
     * 查找WHERE子句的插入点
     */
    private int findWhereInsertPoint(String lowerSql) {
        // 对于UPDATE语句，WHERE应该在SET之后
        if (lowerSql.startsWith("update")) {
            int setIndex = lowerSql.indexOf(" set ");
            if (setIndex > 0) {
                return findNextClauseIndex(lowerSql, setIndex);
            }
        }
        
        // 对于DELETE语句，WHERE应该在FROM之后
        if (lowerSql.startsWith("delete")) {
            int fromIndex = lowerSql.indexOf(" from ");
            if (fromIndex > 0) {
                return findNextClauseIndex(lowerSql, fromIndex);
            }
        }
        
        // 对于SELECT语句，WHERE应该在FROM之后
        if (lowerSql.startsWith("select")) {
            int fromIndex = lowerSql.indexOf(" from ");
            if (fromIndex > 0) {
                return findNextClauseIndex(lowerSql, fromIndex);
            }
        }
        
        return -1;
    }
    
    /**
     * 替换SQL中的表名（用于分库分表）
     */
    protected String replaceTableName(String sql, String originalTable, String newTable) {
        // 简单的表名替换，实际应该使用SQL解析器
        return sql.replaceAll("\\b" + originalTable + "\\b", newTable);
    }
    

}