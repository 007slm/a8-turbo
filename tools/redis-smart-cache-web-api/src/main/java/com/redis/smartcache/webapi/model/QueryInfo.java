package com.redis.smartcache.webapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 查询信息模型
 * 对应Go CLI中的Query结构体
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryInfo {
    
    @JsonProperty("queryId")
    private String id;
    
    @JsonProperty("sql")
    private String sql;
    
    @JsonProperty("tables")
    private List<String> tables;
    
    @JsonProperty("key")
    private String key;
    
    @JsonProperty("count")
    private long count;
    
    @JsonProperty("meanQueryTime")
    private double meanTime;
    
    @JsonProperty("isCached")
    private boolean cached;
    
    @JsonProperty("currentTtl")
    private String currentTtl;
    
    @JsonProperty("pendingTtl")
    private String pendingTtl;
    
    @JsonProperty("currentRule")
    private RuleInfo currentRule;
    
    @JsonProperty("pendingRule")
    private RuleInfo pendingRule;

    // 构造函数
    public QueryInfo() {}

    public QueryInfo(String id, String sql, List<String> tables, String key, 
                    long count, double meanTime) {
        this.id = id;
        this.sql = sql;
        this.tables = tables;
        this.key = key;
        this.count = count;
        this.meanTime = meanTime;
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final QueryInfo query = new QueryInfo();

        public Builder id(String id) {
            query.id = id;
            return this;
        }

        public Builder sql(String sql) {
            query.sql = sql;
            return this;
        }

        public Builder tables(List<String> tables) {
            query.tables = tables;
            return this;
        }

        public Builder key(String key) {
            query.key = key;
            return this;
        }

        public Builder count(long count) {
            query.count = count;
            return this;
        }

        public Builder meanTime(double meanTime) {
            query.meanTime = meanTime;
            return this;
        }

        public Builder cached(boolean cached) {
            query.cached = cached;
            return this;
        }

        public Builder currentTtl(String currentTtl) {
            query.currentTtl = currentTtl;
            return this;
        }

        public Builder pendingTtl(String pendingTtl) {
            query.pendingTtl = pendingTtl;
            return this;
        }

        public Builder currentRule(RuleInfo currentRule) {
            query.currentRule = currentRule;
            return this;
        }

        public Builder pendingRule(RuleInfo pendingRule) {
            query.pendingRule = pendingRule;
            return this;
        }

        public QueryInfo build() {
            return query;
        }
    }

    /**
     * 从RuleConfig设置当前规则并更新缓存状态
     */
    public void setCurrentRuleFromConfig(com.redis.smartcache.webapi.model.RuleConfig ruleConfig) {
        if (ruleConfig != null) {
            this.currentRule = RuleInfo.fromRuleConfig(ruleConfig);
            this.currentTtl = ruleConfig.getTtl().toString();
            this.cached = true;
        } else {
            this.currentRule = null;
            this.currentTtl = null;
            this.cached = false;
        }
    }

    /**
     * 检查查询是否匹配给定规则
     */
    public boolean matchesRule(com.redis.smartcache.webapi.model.RuleConfig rule) {
        if (rule == null) return false;

        // 检查查询ID匹配
        if (rule.getQueryIds() != null && rule.getQueryIds().contains(this.id)) {
            return true;
        }

        // 检查表格匹配
        if (this.tables != null && !this.tables.isEmpty()) {
            // Tables Exact匹配
            if (rule.getTables() != null && 
                rule.getTables().size() == this.tables.size() &&
                rule.getTables().containsAll(this.tables)) {
                return true;
            }

            // Tables Any匹配
            if (rule.getTablesAny() != null && 
                rule.getTablesAny().stream().anyMatch(this.tables::contains)) {
                return true;
            }

            // Tables All匹配
            if (rule.getTablesAll() != null && 
                this.tables.containsAll(rule.getTablesAll())) {
                return true;
            }
        }

        // 检查正则表达式匹配
        if (rule.getRegex() != null && this.sql != null) {
            return this.sql.matches(rule.getRegex());
        }

        // 检查全局规则（所有条件为空）
        if ((rule.getQueryIds() == null || rule.getQueryIds().isEmpty()) &&
            (rule.getTables() == null || rule.getTables().isEmpty()) &&
            (rule.getTablesAny() == null || rule.getTablesAny().isEmpty()) &&
            (rule.getTablesAll() == null || rule.getTablesAll().isEmpty()) &&
            (rule.getRegex() == null || rule.getRegex().isEmpty())) {
            return true;
        }

        return false;
    }

    // Getter和Setter方法
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }

    public List<String> getTables() { return tables; }
    public void setTables(List<String> tables) { this.tables = tables; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public double getMeanTime() { return meanTime; }
    public void setMeanTime(double meanTime) { this.meanTime = meanTime; }

    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }

    public String getCurrentTtl() { return currentTtl; }
    public void setCurrentTtl(String currentTtl) { this.currentTtl = currentTtl; }

    public String getPendingTtl() { return pendingTtl; }
    public void setPendingTtl(String pendingTtl) { this.pendingTtl = pendingTtl; }

    public RuleInfo getCurrentRule() { return currentRule; }
    public void setCurrentRule(RuleInfo currentRule) { this.currentRule = currentRule; }

    public RuleInfo getPendingRule() { return pendingRule; }
    public void setPendingRule(RuleInfo pendingRule) { this.pendingRule = pendingRule; }

    @Override
    public String toString() {
        return "QueryInfo{" +
                "id='" + id + '\'' +
                ", sql='" + sql + '\'' +
                ", tables=" + tables +
                ", count=" + count +
                ", meanTime=" + meanTime +
                ", cached=" + cached +
                '}';
    }
}