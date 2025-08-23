package com.redis.smartcache.webapi.model;

import io.airlift.units.Duration;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * 独立的规则配置类
 * 根据项目规范，不依赖外部库的内部配置类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleConfig {
    
    public static final Duration DEFAULT_TTL = Duration.valueOf("300s");
    
    private Duration ttl;
    private List<String> queryIds;
    private List<String> tables;
    private List<String> tablesAny;
    private List<String> tablesAll;
    private String regex;

    // 构造函数
    public RuleConfig() {
        this.ttl = DEFAULT_TTL;
    }

    public RuleConfig(Duration ttl) {
        this.ttl = ttl != null ? ttl : DEFAULT_TTL;
    }

    // Getters and Setters
    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl != null ? ttl : DEFAULT_TTL;
    }

    public List<String> getQueryIds() {
        return queryIds;
    }

    public void setQueryIds(List<String> queryIds) {
        this.queryIds = queryIds;
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

    // 便利方法
    public boolean isGlobal() {
        return (queryIds == null || queryIds.isEmpty()) &&
               (tables == null || tables.isEmpty()) &&
               (tablesAny == null || tablesAny.isEmpty()) &&
               (tablesAll == null || tablesAll.isEmpty()) &&
               (regex == null || regex.trim().isEmpty());
    }

    public boolean matchesTable(String tableName) {
        if (tableName == null) return false;
        
        if (tables != null && tables.contains(tableName)) return true;
        if (tablesAny != null && tablesAny.contains(tableName)) return true;
        if (tablesAll != null && tablesAll.contains(tableName)) return true;
        
        return false;
    }

    public boolean matchesQuery(String queryId) {
        return queryIds != null && queryIds.contains(queryId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleConfig that = (RuleConfig) o;
        return Objects.equals(ttl, that.ttl) &&
               Objects.equals(queryIds, that.queryIds) &&
               Objects.equals(tables, that.tables) &&
               Objects.equals(tablesAny, that.tablesAny) &&
               Objects.equals(tablesAll, that.tablesAll) &&
               Objects.equals(regex, that.regex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ttl, queryIds, tables, tablesAny, tablesAll, regex);
    }

    @Override
    public String toString() {
        return "RuleConfig{" +
                "ttl=" + ttl +
                ", queryIds=" + queryIds +
                ", tables=" + tables +
                ", tablesAny=" + tablesAny +
                ", tablesAll=" + tablesAll +
                ", regex='" + regex + '\'' +
                '}';
    }
}