package org.openjdbcproxy.grpc.server.chain.processors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.openjdbcproxy.grpc.server.chain.SqlProcessor;
import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;
import org.openjdbcproxy.grpc.server.chain.PostProcessor;
import org.openjdbcproxy.grpc.server.smartcache.statistics.SqlStatisticsData;
import org.openjdbcproxy.grpc.server.smartcache.statistics.TableStatisticsData;
import org.openjdbcproxy.grpc.server.smartcache.statistics.service.RedisStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * SQL统计处理器
 * 负责记录SQL执行情况、查询的表信息等到Redis中
 * 注意：当前版本简化实现，后续需要集成Redis
 */
@Slf4j
@Component
public class SqlStatisticsProcessor extends AbstractSqlProcessor implements PostProcessor {
    
    @Autowired
    private RedisStatisticsService redisStatisticsService;
    
    @Override
    public String getProcessorName() {
        return "SqlStatisticsProcessor";
    }
    
    @Override
    public int getPriority() {
        return 100; // 高优先级，在SmartCache之前执行
    }
    

    
    /**
     * 后处理：在SQL执行完成后记录统计信息
     */
    @Override
    public void postProcess(SqlProcessContext context) throws SQLException {
        // 异步记录统计信息，不阻塞主流程
        CompletableFuture.runAsync(() -> {
            try {
                recordSqlStatistics(context);
            } catch (Exception e) {
                log.warn("记录SQL统计信息失败", e);
            }
        });
    }
    
    /**
     * 记录SQL统计信息
     */
    private void recordSqlStatistics(SqlProcessContext context) {
        try {
            String sql = context.getCurrentSql();
            if (sql == null || sql.trim().isEmpty()) {
                return;
            }
            
            // 记录基本统计信息
            recordBasicStatistics(context);
            
            // 记录表使用情况
            recordTableUsage(context);
            
            // 记录查询模式
            recordQueryPattern(context);
            
            // 记录性能指标
            recordPerformanceMetrics(context);
            
        } catch (Exception e) {
            log.error("记录SQL统计信息时发生错误", e);
        }
    }
    
    /**
     * 记录基本统计信息
     */
    private void recordBasicStatistics(SqlProcessContext context) {
        try {
            String sql = context.getCurrentSql();
            String queryId = String.valueOf(sql.hashCode());
            String queryType = extractQueryType(sql);
            Set<String> tables = extractTableNames(sql);
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - context.getStartTime();
            
            // 创建或获取SQL统计数据
            SqlStatisticsData sqlStats = redisStatisticsService.getSqlStatistics(queryId);
            if (sqlStats == null) {
                sqlStats = SqlStatisticsData.builder()
                        .queryId(queryId)
                        .sql(sql)
                        .tables(tables)
                        .executionCount(0)
                        .averageExecutionTime(0)
                        .maxExecutionTime(0)
                        .minExecutionTime(0)
                        .totalExecutionTime(0)
                        .queryType(queryType)
                        .isCached(false)
                        .currentTtl(0)
                        .cacheHitCount(0)
                        .cacheMissCount(0)
                        .build();
            }
            
            // 更新执行统计
            sqlStats.updateExecutionStats(executionTime);
            
            // 保存到Redis
            redisStatisticsService.saveSqlStatistics(sqlStats);
            
            log.debug("SQL统计数据已记录: {}, 执行时间: {}ms", queryId, executionTime);
            
        } catch (Exception e) {
            log.warn("记录基本统计信息失败", e);
        }
    }
    
    /**
     * 记录表使用情况
     */
    private void recordTableUsage(SqlProcessContext context) {
        try {
            String sql = context.getCurrentSql();
            Set<String> tables = extractTableNames(sql);
            long executionTime = System.currentTimeMillis() - context.getStartTime();
            
            for (String tableName : tables) {
                // 更新表统计数据
                redisStatisticsService.updateTableStatistics(tableName, executionTime, false);
                
                log.debug("表使用统计已记录: {}, 执行时间: {}ms", tableName, executionTime);
            }
            
        } catch (Exception e) {
            log.warn("记录表使用情况失败", e);
        }
    }
    
    /**
     * 记录查询模式
     */
    private void recordQueryPattern(SqlProcessContext context) {
        try {
            String sql = context.getCurrentSql().toLowerCase().trim();
            String pattern = extractQueryPattern(sql);
            
            if (pattern != null) {
                log.info("查询模式统计 - 模式: {}, 时间: {}", 
                        pattern, 
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                // TODO: 集成Redis后，将查询模式统计保存到Redis
            }
            
        } catch (Exception e) {
            log.warn("记录查询模式失败", e);
        }
    }
    
    /**
     * 记录性能指标
     */
    private void recordPerformanceMetrics(SqlProcessContext context) {
        try {
            // 性能指标已经在recordBasicStatistics中记录
            // 这里可以添加额外的性能指标记录逻辑
            long executionTime = System.currentTimeMillis() - context.getStartTime();
            
            if (executionTime > 1000) { // 记录慢查询
                String queryId = String.valueOf(context.getCurrentSql().hashCode());
                log.warn("检测到慢查询 - Query ID: {}, 执行时间: {}ms", queryId, executionTime);
            }
            
        } catch (Exception e) {
            log.warn("记录性能指标失败", e);
        }
    }
    
    /**
     * 提取查询模式
     */
    private String extractQueryPattern(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        
        // 简单的模式提取逻辑
        if (sql.startsWith("select")) {
            return "SELECT";
        } else if (sql.startsWith("insert")) {
            return "INSERT";
        } else if (sql.startsWith("update")) {
            return "UPDATE";
        } else if (sql.startsWith("delete")) {
            return "DELETE";
        } else if (sql.startsWith("create")) {
            return "CREATE";
        } else if (sql.startsWith("drop")) {
            return "DROP";
        } else if (sql.startsWith("alter")) {
            return "ALTER";
        }
        
        return "OTHER";
    }
    
    /**
     * 提取查询类型
     */
    private String extractQueryType(String sql) {
        return extractQueryPattern(sql);
    }
    
    /**
     * 提取表名
     */
    private Set<String> extractTableNames(String sql) {
        Set<String> tables = new HashSet<>();
        if (sql == null || sql.trim().isEmpty()) {
            return tables;
        }
        
        String lowerSql = sql.toLowerCase().trim();
        
        // 简单的表名提取逻辑
        if (lowerSql.contains("from")) {
            String[] parts = lowerSql.split("from");
            if (parts.length > 1) {
                String tablePart = parts[1].trim();
                String[] tableNames = tablePart.split("[,\\s]+");
                
                for (String tableName : tableNames) {
                    tableName = tableName.trim();
                    if (!tableName.isEmpty() && !tableName.startsWith("(") && !tableName.startsWith("select")) {
                        // 移除可能的别名
                        if (tableName.contains(" ")) {
                            tableName = tableName.split(" ")[0];
                        }
                        tables.add(tableName);
                    }
                }
            }
        }
        
        // 处理JOIN语句
        if (lowerSql.contains("join")) {
            String[] parts = lowerSql.split("join");
            for (String part : parts) {
                if (part.trim().length() > 0) {
                    String tableName = part.trim().split("[\\s]+")[0];
                    if (!tableName.isEmpty() && !tableName.startsWith("(")) {
                        tables.add(tableName);
                    }
                }
            }
        }
        
        return tables;
    }
}
