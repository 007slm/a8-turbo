package com.redis.smartcache.webapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty; /**
 * 规则创建请求模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleRequest {
    
    @JsonProperty("queryId")
    private String queryId;
    
    @JsonProperty("tableName")
    private String tableName;
    
    @JsonProperty("ttl")
    private String ttl;
    
    @JsonProperty("rule")
    private RuleInfo rule;

    // 构造函数
    public RuleRequest() {}

    // Getters and Setters
    public String getQueryId() { return queryId; }
    public void setQueryId(String queryId) { this.queryId = queryId; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getTtl() { return ttl; }
    public void setTtl(String ttl) { this.ttl = ttl; }

    public RuleInfo getRule() { return rule; }
    public void setRule(RuleInfo rule) { this.rule = rule; }
}
