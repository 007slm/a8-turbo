package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建缓存规则请求DTO
 * 严格按照设计文档定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCacheRuleRequest {
    private String ruleName;
    private String datasourceName;
    private String dbType;
    private List<String> tables;
    private String ruleType; // tables, tablesAny, tablesAll, queryIds, regex
    private int ttl; // 秒数
    private boolean enabled;
    private String description;
    private int priority;
}