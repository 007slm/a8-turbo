package org.openjdbcproxy.grpc.server.smartcache.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 规则信息模型 - 与 smart cache web api 保持一致的接口
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

    // 从 CacheRuleInfo 创建 RuleInfo 的静态方法
    public static RuleInfo fromCacheRuleInfo(CacheRuleInfo cacheRuleInfo) {
        if (cacheRuleInfo == null) return null;

        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.id = cacheRuleInfo.getId();
        ruleInfo.ttl = cacheRuleInfo.getTtl();
        ruleInfo.tables = cacheRuleInfo.getTables();
        ruleInfo.tablesAny = cacheRuleInfo.getTablesAny();
        ruleInfo.tablesAll = cacheRuleInfo.getTablesAll();
        ruleInfo.regex = cacheRuleInfo.getRegex();
        ruleInfo.queryIds = cacheRuleInfo.getQueryIds();
        ruleInfo.priority = cacheRuleInfo.getPriority();
        ruleInfo.matches = cacheRuleInfo.getMatches();

        // 转换规则类型
        ruleInfo.ruleType = convertRuleType(cacheRuleInfo.getRuleType());

        return ruleInfo;
    }

    // 转换为 CacheRuleInfo
    public CacheRuleInfo toCacheRuleInfo() {
        CacheRuleInfo cacheRuleInfo = new CacheRuleInfo();
        cacheRuleInfo.setId(this.id);
        cacheRuleInfo.setTtl(this.ttl);
        cacheRuleInfo.setTables(this.tables);
        cacheRuleInfo.setTablesAny(this.tablesAny);
        cacheRuleInfo.setTablesAll(this.tablesAll);
        cacheRuleInfo.setRegex(this.regex);
        cacheRuleInfo.setQueryIds(this.queryIds);
        cacheRuleInfo.setPriority(this.priority);
        cacheRuleInfo.setMatches(this.matches);

        // 转换规则类型
        cacheRuleInfo.setRuleType(convertToCacheRuleType(this.ruleType));

        return cacheRuleInfo;
    }

    // 转换规则类型
    private static RuleType convertRuleType(CacheRuleInfo.RuleType cacheRuleType) {
        if (cacheRuleType == null) return RuleType.UNKNOWN;
        
        switch (cacheRuleType) {
            case ALL:
                return RuleType.ALL;
            case REGEX:
                return RuleType.REGEX;
            case TABLES:
                return RuleType.TABLES;
            case TABLES_ANY:
                return RuleType.TABLES_ANY;
            case TABLES_ALL:
                return RuleType.TABLES_ALL;
            case QUERY_IDS:
                return RuleType.QUERY_IDS;
            default:
                return RuleType.UNKNOWN;
        }
    }

    // 转换为 CacheRuleInfo 的规则类型
    private static CacheRuleInfo.RuleType convertToCacheRuleType(RuleType ruleType) {
        if (ruleType == null) return CacheRuleInfo.RuleType.UNKNOWN;
        
        switch (ruleType) {
            case ALL:
                return CacheRuleInfo.RuleType.ALL;
            case REGEX:
                return CacheRuleInfo.RuleType.REGEX;
            case TABLES:
                return CacheRuleInfo.RuleType.TABLES;
            case TABLES_ANY:
                return CacheRuleInfo.RuleType.TABLES_ANY;
            case TABLES_ALL:
                return CacheRuleInfo.RuleType.TABLES_ALL;
            case QUERY_IDS:
                return CacheRuleInfo.RuleType.QUERY_IDS;
            default:
                return CacheRuleInfo.RuleType.UNKNOWN;
        }
    }

    // 确定规则类型
    public static RuleType determineRuleType(RuleInfo rule) {
        if (rule.getTables() != null && !rule.getTables().isEmpty()) {
            return RuleType.TABLES;
        }
        if (rule.getTablesAny() != null && !rule.getTablesAny().isEmpty()) {
            return RuleType.TABLES_ANY;
        }
        if (rule.getTablesAll() != null && !rule.getTablesAll().isEmpty()) {
            return RuleType.TABLES_ALL;
        }
        if (StringUtils.isNotBlank(rule.getRegex())) {
            return RuleType.REGEX;
        }
        if (rule.getQueryIds() != null && !rule.getQueryIds().isEmpty()) {
            return RuleType.QUERY_IDS;
        }
        return RuleType.ALL;
    }

    // 确定匹配条件
    public static String determineMatches(RuleInfo rule) {
        if (rule.getTables() != null && !rule.getTables().isEmpty()) {
            return "Tables: " + String.join(", ", rule.getTables());
        }
        if (rule.getTablesAny() != null && !rule.getTablesAny().isEmpty()) {
            return "Tables (Any): " + String.join(", ", rule.getTablesAny());
        }
        if (rule.getTablesAll() != null && !rule.getTablesAll().isEmpty()) {
            return "Tables (All): " + String.join(", ", rule.getTablesAll());
        }
        if (StringUtils.isNotBlank(rule.getRegex())) {
            return "Regex: " + rule.getRegex();
        }
        if (rule.getQueryIds() != null && !rule.getQueryIds().isEmpty()) {
            return "Query IDs: " + String.join(", ", rule.getQueryIds());
        }
        return "All queries";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTtl() {
        return ttl;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public List<String> getTablesAny() {
        return tablesAny;
    }

    public void setTablesAny(List<String> tablesAny) {
        this.tablesAny = tablesAny;
    }

    public List<String> getTablesAll() {
        return tablesAll;
    }

    public void setTablesAll(List<String> tablesAll) {
        this.tablesAll = tablesAll;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public List<String> getQueryIds() {
        return queryIds;
    }

    public void setQueryIds(List<String> queryIds) {
        this.queryIds = queryIds;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getMatches() {
        return matches;
    }

    public void setMatches(String matches) {
        this.matches = matches;
    }
}
