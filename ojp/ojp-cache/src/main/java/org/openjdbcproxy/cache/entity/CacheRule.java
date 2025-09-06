package org.openjdbcproxy.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存规则
 * 定义缓存策略和配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheRule {

    /**
     * 规则ID
     */
    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * TTL（生存时间，秒）
     */
    private int ttl;

    /**
     * 涉及的表名列表（AND关系：查询必须包含所有这些表）
     */
    private List<String> tables;

    /**
     * 涉及的表名列表（OR关系：查询包含任意一个表即可）
     */
    private List<String> tablesAny;

    /**
     * 规则优先级（数字越小优先级越高）
     */
    private int priority;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 规则类型
     */
    private RuleType ruleType;

    /**
     * 规则条件（可选，用于更复杂的匹配逻辑）
     */
    private String condition;

    /**
     * 规则类型枚举
     */
    public enum RuleType {
        /**
         * 表匹配规则（基于tables字段）
         */
        TABLE_MATCH,
        
        /**
         * 表任意匹配规则（基于tablesAny字段）
         */
        TABLE_ANY_MATCH,
        
        /**
         * 条件匹配规则（基于condition字段）
         */
        CONDITION_MATCH,
        
        /**
         * 默认规则（匹配所有查询）
         */
        DEFAULT
    }

    /**
     * 检查查询是否匹配此规则
     */
    public boolean matches(Query query) {
        if (!enabled) {
            return false;
        }

        // 检查数据源
        if (datasourceName != null && !datasourceName.equals(query.getDatasourceName())) {
            return false;
        }

        // 只处理SELECT查询
        if (!query.isSelectQuery()) {
            return false;
        }

        switch (ruleType) {
            case TABLE_MATCH:
                return matchesTables(query);
            case TABLE_ANY_MATCH:
                return matchesTablesAny(query);
            case CONDITION_MATCH:
                return matchesCondition(query);
            case DEFAULT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 检查是否匹配tables（AND关系）
     */
    private boolean matchesTables(Query query) {
        if (tables == null || tables.isEmpty()) {
            return false;
        }
        
        List<String> queryTables = query.getTables();
        if (queryTables == null || queryTables.isEmpty()) {
            return false;
        }
        
        // 查询必须包含所有指定的表
        return queryTables.containsAll(tables);
    }

    /**
     * 检查是否匹配tablesAny（OR关系）
     */
    private boolean matchesTablesAny(Query query) {
        if (tablesAny == null || tablesAny.isEmpty()) {
            return false;
        }
        
        List<String> queryTables = query.getTables();
        if (queryTables == null || queryTables.isEmpty()) {
            return false;
        }
        
        // 查询包含任意一个指定的表即可
        return queryTables.stream().anyMatch(tablesAny::contains);
    }

    /**
     * 检查是否匹配条件（简单实现，可扩展）
     */
    private boolean matchesCondition(Query query) {
        if (condition == null || condition.isEmpty()) {
            return false;
        }
        
        // 简单的条件匹配实现
        // 可以扩展为更复杂的表达式解析
        String sql = query.getSql().toLowerCase();
        String cond = condition.toLowerCase();
        
        return sql.contains(cond);
    }

    /**
     * 获取规则摘要信息
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Rule[%s]: ttl=%ds", id, ttl));
        
        if (tables != null && !tables.isEmpty()) {
            sb.append(", tables=").append(tables);
        }
        
        if (tablesAny != null && !tablesAny.isEmpty()) {
            sb.append(", tablesAny=").append(tablesAny);
        }
        
        sb.append(", enabled=").append(enabled);
        
        return sb.toString();
    }
}