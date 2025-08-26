package org.openjdbcproxy.grpc.server.chain.processors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 分库分表处理器
 * 
 * 根据分片规则对SQL进行路由和表名重写：
 * 1. 根据分片键确定目标数据库
 * 2. 根据分片规则重写表名
 * 3. 支持多种分片策略（取模、范围、哈希等）
 * 4. 处理跨库查询和事务
 */
@Slf4j
@Component
public class ShardingProcessor extends AbstractSqlProcessor {
    
    private static final String PROCESSOR_NAME = "ShardingProcessor";
    
    // 分片规则配置
    private final Map<String, ShardingRule> shardingRules = new HashMap<>();
    
    // 分片策略
    public enum ShardingStrategy {
        HASH,    // 哈希分片
        RANGE,   // 范围分片
        MOD,     // 取模分片
        TIME     // 时间分片
    }
    
    /**
     * 分片规则配置
     */
    public static class ShardingRule {
        private String tableName;
        private String shardingColumn;
        private ShardingStrategy strategy;
        private int shardCount;
        private String tablePrefix;
        private String databasePrefix;
        
        public ShardingRule(String tableName, String shardingColumn, 
                          ShardingStrategy strategy, int shardCount) {
            this.tableName = tableName;
            this.shardingColumn = shardingColumn;
            this.strategy = strategy;
            this.shardCount = shardCount;
            this.tablePrefix = tableName + "_";
            this.databasePrefix = "db_";
        }
        
        // getters and setters
        public String getTableName() { return tableName; }
        public String getShardingColumn() { return shardingColumn; }
        public ShardingStrategy getStrategy() { return strategy; }
        public int getShardCount() { return shardCount; }
        public String getTablePrefix() { return tablePrefix; }
        public String getDatabasePrefix() { return databasePrefix; }
    }
    
    public ShardingProcessor() {
        // 初始化分片规则（实际应该从配置文件加载）
        initializeShardingRules();
    }
    
    private void initializeShardingRules() {
        // 示例：用户表按user_id进行哈希分片，分为4个表
        shardingRules.put("users", new ShardingRule("users", "user_id", ShardingStrategy.HASH, 4));
        
        // 示例：订单表按order_id进行取模分片，分为8个表
        shardingRules.put("orders", new ShardingRule("orders", "order_id", ShardingStrategy.MOD, 8));
        
        // 示例：日志表按create_time进行时间分片（按月）
        shardingRules.put("logs", new ShardingRule("logs", "create_time", ShardingStrategy.TIME, 12));
    }
    

    
    /**
     * 前处理：在SQL执行前进行分片处理
     */
    @Override
    public void preProcess(SqlProcessContext context) throws SQLException {
        String originalSql = context.getCurrentSql();
        String modifiedSql = originalSql;
        
        // 检查SQL中涉及的表是否需要分片
        if (context.getParseInfo() != null && context.getParseInfo().getTableNames() != null) {
            for (String tableName : context.getParseInfo().getTableNames()) {
                ShardingRule rule = shardingRules.get(tableName);
                if (rule != null) {
                    modifiedSql = applyShardingRule(modifiedSql, rule, context);
                }
            }
        }
        
        // 如果SQL被修改，更新上下文
        if (!modifiedSql.equals(originalSql)) {
            context.updateSql(modifiedSql);
            log.info("SQL modified by sharding: {} -> {}", originalSql, modifiedSql);
            
            // 设置目标数据库信息
            setTargetDatabase(context);
        }
    }
    
    /**
     * 应用分片规则
     */
    private String applyShardingRule(String sql, ShardingRule rule, SqlProcessContext context) throws SQLException {
        String shardingValue = extractShardingValue(sql, rule, context);
        
        if (shardingValue != null) {
            int shardIndex = calculateShardIndex(shardingValue, rule);
            String targetTableName = rule.getTablePrefix() + shardIndex;
            
            // 替换表名
            String modifiedSql = replaceTableName(sql, rule.getTableName(), targetTableName);
            
            // 记录分片信息
            context.setAttribute("shard_table_" + rule.getTableName(), targetTableName);
            context.setAttribute("shard_index_" + rule.getTableName(), shardIndex);
            context.setAttribute("shard_value_" + rule.getTableName(), shardingValue);
            
            log.debug("Applied sharding rule for table {}: {} -> {}, shard value: {}", 
                     rule.getTableName(), rule.getTableName(), targetTableName, shardingValue);
            
            return modifiedSql;
        }
        
        return sql;
    }
    
    /**
     * 从SQL中提取分片键值
     */
    private String extractShardingValue(String sql, ShardingRule rule, SqlProcessContext context) {
        String shardingColumn = rule.getShardingColumn();
        
        // 对于不同的SQL操作类型，提取分片键值的方式不同
        switch (context.getOperationType()) {
            case SELECT:
            case UPDATE:
            case DELETE:
                return extractFromWhereClause(sql, shardingColumn);
                
            case INSERT:
                return extractFromInsertValues(sql, shardingColumn);
                
            default:
                return null;
        }
    }
    
    /**
     * 从WHERE子句提取分片键值
     */
    private String extractFromWhereClause(String sql, String shardingColumn) {
        // 简化实现：查找 column = value 模式
        Pattern pattern = Pattern.compile(shardingColumn + "\\s*=\\s*'?([^'\\s,)]+)'?", 
                                        Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果WHERE子句中没有明确的分片键值，可能需要特殊处理
        // 例如跨分片查询、基于其他条件推断等
        return null;
    }
    
    /**
     * 从INSERT语句的VALUES子句提取分片键值
     */
    private String extractFromInsertValues(String sql, String shardingColumn) {
        // 简化实现：需要解析INSERT语句的列和值
        // 实际应该使用SQL解析器
        
        // 查找INSERT INTO table (columns) VALUES (values) 模式
        Pattern insertPattern = Pattern.compile(
            "insert\\s+into\\s+\\w+\\s*\\(([^)]+)\\)\\s*values\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = insertPattern.matcher(sql);
        if (matcher.find()) {
            String columns = matcher.group(1);
            String values = matcher.group(2);
            
            String[] columnArray = columns.split(",");
            String[] valueArray = values.split(",");
            
            for (int i = 0; i < columnArray.length && i < valueArray.length; i++) {
                if (columnArray[i].trim().equalsIgnoreCase(shardingColumn)) {
                    return valueArray[i].trim().replaceAll("'", "");
                }
            }
        }
        
        return null;
    }
    
    /**
     * 计算分片索引
     */
    private int calculateShardIndex(String shardingValue, ShardingRule rule) {
        switch (rule.getStrategy()) {
            case HASH:
                return Math.abs(shardingValue.hashCode()) % rule.getShardCount();
                
            case MOD:
                try {
                    long numericValue = Long.parseLong(shardingValue);
                    return (int) (numericValue % rule.getShardCount());
                } catch (NumberFormatException e) {
                    // 如果不是数字，使用哈希策略
                    return Math.abs(shardingValue.hashCode()) % rule.getShardCount();
                }
                
            case RANGE:
                // 范围分片：需要预定义范围规则
                return calculateRangeShardIndex(shardingValue, rule);
                
            case TIME:
                // 时间分片：按时间周期分片
                return calculateTimeShardIndex(shardingValue, rule);
                
            default:
                return 0;
        }
    }
    
    /**
     * 计算范围分片索引
     */
    private int calculateRangeShardIndex(String shardingValue, ShardingRule rule) {
        // 简化实现：按数值范围分片
        try {
            long numericValue = Long.parseLong(shardingValue);
            long rangeSize = 1000000; // 每个分片的范围大小
            return (int) Math.min(numericValue / rangeSize, rule.getShardCount() - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 计算时间分片索引
     */
    private int calculateTimeShardIndex(String shardingValue, ShardingRule rule) {
        // 简化实现：按月分片
        try {
            // 假设时间格式为 YYYY-MM-DD
            String[] parts = shardingValue.split("-");
            if (parts.length >= 2) {
                int month = Integer.parseInt(parts[1]);
                return (month - 1) % rule.getShardCount();
            }
        } catch (Exception e) {
            log.warn("Failed to parse time value for sharding: {}", shardingValue);
        }
        
        return 0;
    }
    
    /**
     * 设置目标数据库信息
     */
    private void setTargetDatabase(SqlProcessContext context) {
        // 根据分片规则确定目标数据库
        // 这里可以设置连接池路由信息
        
        String targetDatabase = determineTargetDatabase(context);
        if (targetDatabase != null) {
            context.setAttribute("target_database", targetDatabase);
            log.debug("Target database determined: {}", targetDatabase);
        }
    }
    
    /**
     * 确定目标数据库
     */
    private String determineTargetDatabase(SqlProcessContext context) {
        // 简化实现：基于分片索引确定数据库
        // 实际可能需要更复杂的路由逻辑
        
        for (String key : context.getAttributes().keySet()) {
            if (key.startsWith("shard_index_")) {
                Integer shardIndex = context.getAttribute(key);
                if (shardIndex != null) {
                    return "db_" + shardIndex;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 处理跨分片查询
     */
    private boolean handleCrossShardQuery(SqlProcessContext context) {
        // 如果查询涉及多个分片，需要特殊处理
        // 可能需要拆分查询、合并结果等
        
        log.warn("Cross-shard query detected, this requires special handling");
        return false;
    }
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public int getPriority() {
        return 80; // 在数据权限之后，缓存之前执行
    }
    
    @Override
    public Set<SqlProcessContext.SqlOperationType> getSupportedOperations() {
        return Set.of(
            SqlProcessContext.SqlOperationType.SELECT,
            SqlProcessContext.SqlOperationType.INSERT,
            SqlProcessContext.SqlOperationType.UPDATE,
            SqlProcessContext.SqlOperationType.DELETE
        );
    }
}