package com.redis.smartcache.webapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 表格信息模型
 * 对应Go CLI中的Table结构体
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableInfo {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("accessFrequency")
    private long accessFrequency;
    
    @JsonProperty("avgQueryTime")
    private double queryTime;
    
    @JsonProperty("ttl")
    private String ttl;
    
    @JsonProperty("rule")
    private RuleInfo rule;

    // 构造函数
    public TableInfo() {}

    public TableInfo(String name, long accessFrequency, double queryTime) {
        this.name = name;
        this.accessFrequency = accessFrequency;
        this.queryTime = queryTime;
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TableInfo table = new TableInfo();

        public Builder name(String name) {
            table.name = name;
            return this;
        }

        public Builder accessFrequency(long accessFrequency) {
            table.accessFrequency = accessFrequency;
            return this;
        }

        public Builder queryTime(double queryTime) {
            table.queryTime = queryTime;
            return this;
        }

        public Builder ttl(String ttl) {
            table.ttl = ttl;
            return this;
        }

        public Builder rule(RuleInfo rule) {
            table.rule = rule;
            return this;
        }

        public TableInfo build() {
            return table;
        }
    }

    /**
     * 设置规则并更新TTL
     */
    public void setRule(com.redis.smartcache.webapi.model.RuleConfig ruleConfig) {
        if (ruleConfig != null) {
            this.rule = RuleInfo.fromRuleConfig(ruleConfig);
            this.ttl = ruleConfig.getTtl().toString();
        } else {
            this.rule = null;
            this.ttl = null;
        }
    }

    /**
     * 检查表格是否匹配给定规则
     */
    public boolean matchesRule(RuleConfig rule) {
        if (rule == null || this.name == null) return false;

        // 检查TablesAny匹配
        if (rule.getTablesAny() != null && 
            rule.getTablesAny().contains(this.name)) {
            return true;
        }

        // 检查Tables匹配
        if (rule.getTables() != null && 
            rule.getTables().contains(this.name)) {
            return true;
        }

        // 检查TablesAll匹配
        if (rule.getTablesAll() != null && 
            rule.getTablesAll().contains(this.name)) {
            return true;
        }

        // 检查全局规则
        if ((rule.getQueryIds() == null || rule.getQueryIds().isEmpty()) &&
            (rule.getTables() == null || rule.getTables().isEmpty()) &&
            (rule.getTablesAny() == null || rule.getTablesAny().isEmpty()) &&
            (rule.getTablesAll() == null || rule.getTablesAll().isEmpty()) &&
            (rule.getRegex() == null || rule.getRegex().isEmpty())) {
            return true;
        }

        return false;
    }

    /**
     * 获取TTL，如果没有规则则返回空字符串
     */
    public String getTtl() {
        if (rule != null && rule.getTtl() != null) {
            return rule.getTtl();
        }
        return ttl != null ? ttl : "";
    }

    // Getter和Setter方法
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getAccessFrequency() { return accessFrequency; }
    public void setAccessFrequency(long accessFrequency) { this.accessFrequency = accessFrequency; }

    public double getQueryTime() { return queryTime; }
    public void setQueryTime(double queryTime) { this.queryTime = queryTime; }

    public void setTtl(String ttl) { this.ttl = ttl; }

    public RuleInfo getRule() { return rule; }
    public void setRule(RuleInfo rule) { this.rule = rule; }

    @Override
    public String toString() {
        return "TableInfo{" +
                "name='" + name + '\'' +
                ", accessFrequency=" + accessFrequency +
                ", queryTime=" + queryTime +
                ", ttl='" + ttl + '\'' +
                '}';
    }
}