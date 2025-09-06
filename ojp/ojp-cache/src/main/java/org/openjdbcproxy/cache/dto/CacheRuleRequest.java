package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 缓存规则请求
 * 用于创建和更新缓存规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheRuleRequest {

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * TTL（生存时间，秒）
     */
    @NotNull(message = "TTL不能为空")
    @Min(value = 1, message = "TTL必须大于0")
    private Integer ttl;

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
    @Builder.Default
    private Integer priority = 100;

    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 规则类型
     */
    @NotNull(message = "规则类型不能为空")
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
     * 验证请求参数
     */
    public void validate() {
        switch (ruleType) {
            case TABLE_MATCH:
                if (tables == null || tables.isEmpty()) {
                    throw new IllegalArgumentException("TABLE_MATCH类型规则必须指定tables");
                }
                break;
            case TABLE_ANY_MATCH:
                if (tablesAny == null || tablesAny.isEmpty()) {
                    throw new IllegalArgumentException("TABLE_ANY_MATCH类型规则必须指定tablesAny");
                }
                break;
            case CONDITION_MATCH:
                if (condition == null || condition.trim().isEmpty()) {
                    throw new IllegalArgumentException("CONDITION_MATCH类型规则必须指定condition");
                }
                break;
            case DEFAULT:
                // 默认规则不需要额外验证
                break;
            default:
                throw new IllegalArgumentException("不支持的规则类型: " + ruleType);
        }
    }

    /**
     * 获取规则摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Rule[%s]: ttl=%ds", name, ttl));
        
        if (tables != null && !tables.isEmpty()) {
            sb.append(", tables=").append(tables);
        }
        
        if (tablesAny != null && !tablesAny.isEmpty()) {
            sb.append(", tablesAny=").append(tablesAny);
        }
        
        if (condition != null && !condition.trim().isEmpty()) {
            sb.append(", condition=").append(condition);
        }
        
        sb.append(", enabled=").append(enabled);
        
        return sb.toString();
    }
}