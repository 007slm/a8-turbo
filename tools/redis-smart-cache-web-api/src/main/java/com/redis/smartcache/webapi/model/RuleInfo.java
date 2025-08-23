package com.redis.smartcache.webapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import io.airlift.units.Duration;
import java.util.List;

/**
 * 规则信息模型
 * 对应Go CLI中的Rule结构体和Java的RuleConfig
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleInfo {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("ttl")
    private String ttl;
    
    @JsonProperty("ruleType")
    private RuleType ruleType;
    
    @JsonProperty("tables")
    private List<String> tables;
    
    @JsonProperty("tablesAny")
    private List<String> tablesAny;
    
    @JsonProperty("tablesAll")
    private List<String> tablesAll;
    
    @JsonProperty("regex")
    private String regex;
    
    @JsonProperty("queryIds")
    private List<String> queryIds;
    
    @JsonProperty("priority")
    private int priority;
    
    @JsonProperty("matches")
    private String matches;

    // 规则类型枚举
    public enum RuleType {
        ALL("All"),
        REGEX("Regex"),
        TABLES("Tables Exact"),
        TABLES_ANY("Tables Any"),
        TABLES_ALL("Tables All"),
        QUERY_IDS("Query IDs"),
        UNKNOWN("Unknown");

        private final String displayName;

        RuleType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 构造函数
    public RuleInfo() {}

    public RuleInfo(String ttl, RuleType ruleType) {
        this.ttl = ttl;
        this.ruleType = ruleType;
    }

    // 从RuleConfig创建RuleInfo的静态方法
    public static RuleInfo fromRuleConfig(com.redis.smartcache.webapi.model.RuleConfig ruleConfig) {
        if (ruleConfig == null) return null;

        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.ttl = ruleConfig.getTtl().toString();
        ruleInfo.tables = ruleConfig.getTables();
        ruleInfo.tablesAny = ruleConfig.getTablesAny();
        ruleInfo.tablesAll = ruleConfig.getTablesAll();
        ruleInfo.regex = ruleConfig.getRegex();
        ruleInfo.queryIds = ruleConfig.getQueryIds();

        // 确定规则类型和匹配条件
        ruleInfo.ruleType = determineRuleType(ruleConfig);
        ruleInfo.matches = determineMatches(ruleConfig);

        return ruleInfo;
    }

    // 转换为RuleConfig
    public com.redis.smartcache.webapi.model.RuleConfig toRuleConfig() {
        com.redis.smartcache.webapi.model.RuleConfig ruleConfig = new com.redis.smartcache.webapi.model.RuleConfig();
        
        // 转换TTL字符串为Duration对象
        if (this.ttl != null && !this.ttl.trim().isEmpty()) {
            try {
                ruleConfig.setTtl(Duration.valueOf(this.ttl));
            } catch (IllegalArgumentException e) {
                // 如果解析失败，使用默认值
                ruleConfig.setTtl(com.redis.smartcache.webapi.model.RuleConfig.DEFAULT_TTL);
            }
        } else {
            ruleConfig.setTtl(com.redis.smartcache.webapi.model.RuleConfig.DEFAULT_TTL);
        }
        
        ruleConfig.setTables(this.tables);
        ruleConfig.setTablesAny(this.tablesAny);
        ruleConfig.setTablesAll(this.tablesAll);
        ruleConfig.setRegex(this.regex);
        ruleConfig.setQueryIds(this.queryIds);
        return ruleConfig;
    }

    // 确定规则类型
    private static RuleType determineRuleType(com.redis.smartcache.webapi.model.RuleConfig rule) {
        if (rule.getTables() != null && !rule.getTables().isEmpty()) {
            return RuleType.TABLES;
        }
        if (rule.getTablesAny() != null && !rule.getTablesAny().isEmpty()) {
            return RuleType.TABLES_ANY;
        }
        if (rule.getTablesAll() != null && !rule.getTablesAll().isEmpty()) {
            return RuleType.TABLES_ALL;
        }
        if (rule.getQueryIds() != null && !rule.getQueryIds().isEmpty()) {
            return RuleType.QUERY_IDS;
        }
        if (StringUtils.isNotBlank(rule.getRegex())) {
            return RuleType.REGEX;
        }
        return RuleType.ALL;
    }

    // 确定匹配条件
    private static String determineMatches(com.redis.smartcache.webapi.model.RuleConfig rule) {
        if (rule.getTables() != null && !rule.getTables().isEmpty()) {
            return String.join(",", rule.getTables());
        }
        if (rule.getTablesAny() != null && !rule.getTablesAny().isEmpty()) {
            return String.join(",", rule.getTablesAny());
        }
        if (rule.getTablesAll() != null && !rule.getTablesAll().isEmpty()) {
            return String.join(",", rule.getTablesAll());
        }
        if (rule.getQueryIds() != null && !rule.getQueryIds().isEmpty()) {
            return String.join(",", rule.getQueryIds());
        }
        if (StringUtils.isNotBlank(rule.getRegex())) {
            return rule.getRegex();
        }
        return "any";
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RuleInfo rule = new RuleInfo();

        public Builder id(String id) {
            rule.id = id;
            return this;
        }

        public Builder ttl(String ttl) {
            rule.ttl = ttl;
            return this;
        }

        public Builder ruleType(RuleType ruleType) {
            rule.ruleType = ruleType;
            return this;
        }

        public Builder tables(List<String> tables) {
            rule.tables = tables;
            return this;
        }

        public Builder tablesAny(List<String> tablesAny) {
            rule.tablesAny = tablesAny;
            return this;
        }

        public Builder tablesAll(List<String> tablesAll) {
            rule.tablesAll = tablesAll;
            return this;
        }

        public Builder regex(String regex) {
            rule.regex = regex;
            return this;
        }

        public Builder queryIds(List<String> queryIds) {
            rule.queryIds = queryIds;
            return this;
        }

        public Builder priority(int priority) {
            rule.priority = priority;
            return this;
        }

        public Builder matches(String matches) {
            rule.matches = matches;
            return this;
        }

        public RuleInfo build() {
            return rule;
        }
    }

    // Getter和Setter方法
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTtl() { return ttl; }
    public void setTtl(String ttl) { this.ttl = ttl; }

    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }

    public List<String> getTables() { return tables; }
    public void setTables(List<String> tables) { this.tables = tables; }

    public List<String> getTablesAny() { return tablesAny; }
    public void setTablesAny(List<String> tablesAny) { this.tablesAny = tablesAny; }

    public List<String> getTablesAll() { return tablesAll; }
    public void setTablesAll(List<String> tablesAll) { this.tablesAll = tablesAll; }

    public String getRegex() { return regex; }
    public void setRegex(String regex) { this.regex = regex; }

    public List<String> getQueryIds() { return queryIds; }
    public void setQueryIds(List<String> queryIds) { this.queryIds = queryIds; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getMatches() { return matches; }
    public void setMatches(String matches) { this.matches = matches; }

    @Override
    public String toString() {
        return "RuleInfo{" +
                "id='" + id + '\'' +
                ", ttl='" + ttl + '\'' +
                ", ruleType=" + ruleType +
                ", matches='" + matches + '\'' +
                '}';
    }
}