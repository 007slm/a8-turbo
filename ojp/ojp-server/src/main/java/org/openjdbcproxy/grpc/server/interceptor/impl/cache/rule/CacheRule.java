package org.openjdbcproxy.grpc.server.interceptor.impl.cache.rule;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 缓存规则
 * 定义缓存匹配规则和TTL配置，支持前端UI的所有规则类型
 */
@Data
@Slf4j
public class CacheRule {
    
    /**
     * 规则ID（唯一标识）
     */
    private String id;
    
    /**
     * 规则名称
     */
    private String name;
    
    /**
     * 规则描述
     */
    private String description;
    
    /**
     * 规则类型
     */
    private RuleType ruleType;
    
    /**
     * 匹配值（根据规则类型不同而不同）
     */
    private String ruleMatch;
    
    /**
     * TTL字符串表示（如：30m, 1h, 1d）
     */
    private String ttl;
    
    /**
     * 规则状态
     */
    private RuleStatus status;
    
    /**
     * 是否为默认规则
     */
    private boolean isDefault;
    
    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;
    
    /**
     * 规则优先级（数字越大优先级越高）
     */
    private int priority;
    
    /**
     * 规则是否启用
     */
    private boolean enabled;
    
    // 表格相关字段
    private List<String> tables;
    private List<String> tablesAny;
    private List<String> tablesAll;
    
    // 查询ID相关字段
    private List<String> queryIds;
    
    // 正则表达式字段
    private String regex;
    
    // 查询类型字段
    private String queryType;
    
    /**
     * 规则类型枚举
     */
    public enum RuleType {
        TABLES("tables", "表格精确匹配"),
        TABLES_ANY("tablesAny", "表格任意匹配"),
        TABLES_ALL("tablesAll", "表格全部匹配"),
        QUERY_IDS("queryIds", "查询ID匹配"),
        REGEX("regex", "正则表达式"),
        ANY("any", "匹配所有");
        
        private final String value;
        private final String description;
        
        RuleType(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() { return value; }
        public String getDescription() { return description; }
    }
    
    /**
     * 规则状态枚举
     */
    public enum RuleStatus {
        ACTIVE("活跃"),
        INACTIVE("非活跃"),
        PENDING("待提交"),
        ERROR("错误");
        
        private final String description;
        
        RuleStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * 检查规则是否匹配给定的SQL和表格
     */
    public boolean matches(String sql, List<String> tables, Object parameters) {
        if (!enabled || status != RuleStatus.ACTIVE) {
            return false;
        }
        
        switch (ruleType) {
            case TABLES:
                return matchesTables(tables);
            case TABLES_ANY:
                return matchesTablesAny(tables);
            case TABLES_ALL:
                return matchesTablesAll(tables);
            case QUERY_IDS:
                return matchesQueryIds(sql, parameters);
            case REGEX:
                return matchesRegex(sql);
            case ANY:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 表格精确匹配：查询涉及的表格必须与规则完全一致
     */
    private boolean matchesTables(List<String> queryTables) {
        if (tables == null || tables.isEmpty()) {
            return false;
        }
        return queryTables.containsAll(tables) && tables.containsAll(queryTables);
    }
    
    /**
     * 表格任意匹配：查询涉及任一指定表格
     */
    private boolean matchesTablesAny(List<String> queryTables) {
        if (tablesAny == null || tablesAny.isEmpty()) {
            return false;
        }
        return queryTables.stream().anyMatch(tablesAny::contains);
    }
    
    /**
     * 表格全部匹配：查询必须涉及所有指定表格
     */
    private boolean matchesTablesAll(List<String> queryTables) {
        if (tablesAll == null || tablesAll.isEmpty()) {
            return false;
        }
        return queryTables.containsAll(tablesAll);
    }
    
    /**
     * 查询ID匹配：精确匹配查询ID（CRC32哈希值）
     */
    private boolean matchesQueryIds(String sql, Object parameters) {
        if (queryIds == null || queryIds.isEmpty()) {
            return false;
        }
        
        // 生成当前查询的ID
        String currentQueryId = generateQueryId(sql, parameters);
        return queryIds.contains(currentQueryId);
    }
    
    /**
     * 正则表达式匹配：匹配SQL语句模式
     */
    private boolean matchesRegex(String sql) {
        if (regex == null || regex.trim().isEmpty()) {
            return false;
        }
        try {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sql).find();
        } catch (Exception e) {
            log.warn("Invalid regex pattern: {}", regex, e);
            return false;
        }
    }
    
    /**
     * 生成查询ID（CRC32哈希）
     */
    private String generateQueryId(String sql, Object parameters) {
        // 简单的哈希实现，实际项目中可以使用更复杂的算法
        String content = sql + (parameters != null ? parameters.toString() : "");
        return Integer.toHexString(content.hashCode());
    }
    
    /**
     * 获取TTL的Duration对象
     */
    public Duration getTtlDuration() {
        if (ttl == null || ttl.trim().isEmpty()) {
            return Duration.ofMinutes(30); // 默认30分钟
        }
        
        try {
            return parseTtl(ttl);
        } catch (Exception e) {
            log.warn("Invalid TTL format: {}, using default", ttl);
            return Duration.ofMinutes(30);
        }
    }
    
    /**
     * 解析TTL字符串
     */
    private Duration parseTtl(String ttlStr) {
        String ttl = ttlStr.trim().toLowerCase();
        
        if (ttl.endsWith("s")) {
            long seconds = Long.parseLong(ttl.substring(0, ttl.length() - 1));
            return Duration.ofSeconds(seconds);
        } else if (ttl.endsWith("m")) {
            long minutes = Long.parseLong(ttl.substring(0, ttl.length() - 1));
            return Duration.ofMinutes(minutes);
        } else if (ttl.endsWith("h")) {
            long hours = Long.parseLong(ttl.substring(0, ttl.length() - 1));
            return Duration.ofHours(hours);
        } else if (ttl.endsWith("d")) {
            long days = Long.parseLong(ttl.substring(0, ttl.length() - 1));
            return Duration.ofDays(days);
        } else {
            // 默认按分钟处理
            long minutes = Long.parseLong(ttl);
            return Duration.ofMinutes(minutes);
        }
    }
    
    /**
     * 设置TTL
     */
    public void setTtl(String ttl) {
        this.ttl = ttl;
        // 验证TTL格式
        if (ttl != null && !ttl.trim().isEmpty()) {
            try {
                parseTtl(ttl);
            } catch (Exception e) {
                log.warn("Invalid TTL format: {}", ttl);
            }
        }
    }
    
    /**
     * 更新最后修改时间
     */
    public void updateLastModified() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 验证规则的有效性
     */
    public boolean isValid() {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        if (ruleType == null) {
            return false;
        }
        
        if (ttl == null || ttl.trim().isEmpty()) {
            return false;
        }
        
        // 验证TTL格式
        try {
            parseTtl(ttl);
        } catch (Exception e) {
            return false;
        }
        
        // 根据规则类型验证必要字段
        switch (ruleType) {
            case TABLES:
                return tables != null && !tables.isEmpty();
            case TABLES_ANY:
                return tablesAny != null && !tablesAny.isEmpty();
            case TABLES_ALL:
                return tablesAll != null && !tablesAll.isEmpty();
            case QUERY_IDS:
                return queryIds != null && !queryIds.isEmpty();
            case REGEX:
                return regex != null && !regex.trim().isEmpty();
            case ANY:
                return true;
            default:
                return false;
        }
    }
}
