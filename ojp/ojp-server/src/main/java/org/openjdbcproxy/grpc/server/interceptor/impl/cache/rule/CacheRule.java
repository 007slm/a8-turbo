package org.openjdbcproxy.grpc.server.interceptor.impl.cache.rule;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 缓存规则
 * 定义缓存匹配规则和TTL配置
 */
@Slf4j
public class CacheRule {
    private String name;
    private String description;
    private RuleType type;
    private String pattern;
    private Duration ttl;
    private int priority;
    private boolean enabled;
    private List<String> tables;
    private List<String> tablesAny;
    private List<String> tablesAll;
    private String regex;
    
    public enum RuleType {
        TABLE_NAME, REGEX, QUERY_TYPE, GLOBAL
    }
    
    public boolean matches(String sql, List<String> tables, Object parameters) {
        if (!enabled) {
            return false;
        }
        
        switch (type) {
            case TABLE_NAME:
                return matchesTableName(tables);
            case REGEX:
                return matchesRegex(sql);
            case QUERY_TYPE:
                return matchesQueryType(sql);
            case GLOBAL:
                return true;
            default:
                return false;
        }
    }
    
    private boolean matchesTableName(List<String> queryTables) {
        if (tables != null && !tables.isEmpty()) {
            return queryTables.containsAll(tables) && tables.containsAll(queryTables);
        }
        if (tablesAny != null && !tablesAny.isEmpty()) {
            return queryTables.stream().anyMatch(tablesAny::contains);
        }
        if (tablesAll != null && !tablesAll.isEmpty()) {
            return queryTables.containsAll(tablesAll);
        }
        return false;
    }
    
    private boolean matchesRegex(String sql) {
        if (regex != null && !regex.isEmpty()) {
            try {
                return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sql).find();
            } catch (Exception e) {
                log.warn("Invalid regex pattern: {}", regex, e);
                return false;
            }
        }
        return false;
    }
    
    private boolean matchesQueryType(String sql) {
        if (pattern != null && !pattern.isEmpty()) {
            String upperSql = sql.toUpperCase().trim();
            return upperSql.startsWith(pattern.toUpperCase());
        }
        return false;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public RuleType getType() { return type; }
    public void setType(RuleType type) { this.type = type; }
    
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    
    public Duration getTtl() { return ttl; }
    public void setTtl(Duration ttl) { this.ttl = ttl; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public List<String> getTables() { return tables; }
    public void setTables(List<String> tables) { this.tables = tables; }
    
    public List<String> getTablesAny() { return tablesAny; }
    public void setTablesAny(List<String> tablesAny) { this.tablesAny = tablesAny; }
    
    public List<String> getTablesAll() { return tablesAll; }
    public void setTablesAll(List<String> tablesAll) { this.tablesAll = tablesAll; }
    
    public String getRegex() { return regex; }
    public void setRegex(String regex) { this.regex = regex; }
}
