package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 缓存规则响应DTO
 * 严格按照设计文档定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheRuleResponse {
    private String ruleId;
    private String ruleName;
    private String datasourceName;
    private String dbType;
    private List<String> tables;
    private String ruleType; // tables, tablesAny, tablesAll, queryIds, regex
    private int ttl; // 秒数
    private boolean enabled;
    private String description;
    private int priority;
    private String createdAt; // ISO格式: "2024-01-01T12:00:00Z"
    private String updatedAt; // ISO格式: "2024-01-01T12:00:00Z"
    private List<String> matchedQueries;
}