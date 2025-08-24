package com.redis.smartcache.webapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty; /**
 * 批量提交规则请求模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchRuleRequest {
    
    @JsonProperty("rules")
    private java.util.List<RuleInfo> rules;

    // 构造函数
    public BatchRuleRequest() {}

    // Getters and Setters
    public java.util.List<RuleInfo> getRules() { return rules; }
    public void setRules(java.util.List<RuleInfo> rules) { this.rules = rules; }
}
