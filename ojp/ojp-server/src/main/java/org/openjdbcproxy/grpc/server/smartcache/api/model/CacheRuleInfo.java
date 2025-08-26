package org.openjdbcproxy.grpc.server.smartcache.api.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.List;

/**
 * 缓存规则信息模型
 * 对应 Redis Smart Cache 中的 Rule 结构，支持多种规则类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheRuleInfo {
    
    /**
     * 规则唯一标识
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
     * 缓存TTL（时间字符串格式，如：30m, 2h, 1d）
     */
    private String ttl;
    
    /**
     * 规则类型
     */
    private RuleType ruleType;
    
    /**
     * 精确匹配的表名列表
     */
    private List<String> tables;
    
    /**
     * 任意匹配的表名列表（OR关系）
     */
    private List<String> tablesAny;
    
    /**
     * 全部匹配的表名列表（AND关系）
     */
    private List<String> tablesAll;
    
    /**
     * 正则表达式匹配
     */
    private String regex;
    
    /**
     * 查询ID列表
     */
    private List<String> queryIds;
    
    /**
     * 规则优先级（数字越小优先级越高）
     */
    private int priority;
    
    /**
     * 匹配条件描述
     */
    private String matches;
    
    /**
     * 是否启用
     */
    private boolean enabled;
    
    /**
     * 创建时间
     */
    private long createdAt;
    
    /**
     * 更新时间
     */
    private long updatedAt;
    
    /**
     * 规则类型枚举
     */
    public enum RuleType {
        ALL("All", "匹配所有查询"),
        REGEX("Regex", "正则表达式匹配"),
        TABLES("Tables Exact", "精确表名匹配"),
        TABLES_ANY("Tables Any", "任意表名匹配（OR）"),
        TABLES_ALL("Tables All", "全部表名匹配（AND）"),
        QUERY_IDS("Query IDs", "查询ID匹配"),
        UNKNOWN("Unknown", "未知类型");

        private final String displayName;
        private final String description;

        RuleType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 从TTL字符串创建Duration对象
     */
    public Duration getTtlDuration() {
        if (ttl == null || ttl.trim().isEmpty()) {
            return Duration.ofMinutes(10); // 默认10分钟
        }
        
        try {
            String ttlStr = ttl.trim().toLowerCase();
            if (ttlStr.endsWith("s")) {
                long seconds = Long.parseLong(ttlStr.substring(0, ttlStr.length() - 1));
                return Duration.ofSeconds(seconds);
            } else if (ttlStr.endsWith("m")) {
                long minutes = Long.parseLong(ttlStr.substring(0, ttlStr.length() - 1));
                return Duration.ofMinutes(minutes);
            } else if (ttlStr.endsWith("h")) {
                long hours = Long.parseLong(ttlStr.substring(0, ttlStr.length() - 1));
                return Duration.ofHours(hours);
            } else if (ttlStr.endsWith("d")) {
                long days = Long.parseLong(ttlStr.substring(0, ttlStr.length() - 1));
                return Duration.ofDays(days);
            } else {
                // 默认按分钟处理
                long minutes = Long.parseLong(ttlStr);
                return Duration.ofMinutes(minutes);
            }
        } catch (NumberFormatException e) {
            return Duration.ofMinutes(10); // 解析失败时使用默认值
        }
    }

    /**
     * 设置TTL为Duration对象
     */
    public void setTtlDuration(Duration duration) {
        if (duration != null) {
            long minutes = duration.toMinutes();
            if (minutes < 60) {
                this.ttl = minutes + "m";
            } else if (minutes < 1440) { // 24小时
                this.ttl = (minutes / 60) + "h";
            } else {
                this.ttl = (minutes / 1440) + "d";
            }
        }
    }

    /**
     * 确定规则类型
     */
    public static RuleType determineRuleType(CacheRuleInfo rule) {
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
        if (rule.getRegex() != null && !rule.getRegex().trim().isEmpty()) {
            return RuleType.REGEX;
        }
        return RuleType.ALL;
    }

    /**
     * 确定匹配条件描述
     */
    public static String determineMatches(CacheRuleInfo rule) {
        if (rule.getTables() != null && !rule.getTables().isEmpty()) {
            return String.join(",", rule.getTables());
        }
        if (rule.getTablesAny() != null && !rule.getTablesAny().isEmpty()) {
            return String.join(" OR ", rule.getTablesAny());
        }
        if (rule.getTablesAll() != null && !rule.getTablesAll().isEmpty()) {
            return String.join(" AND ", rule.getTablesAll());
        }
        if (rule.getQueryIds() != null && !rule.getQueryIds().isEmpty()) {
            return String.join(",", rule.getQueryIds());
        }
        if (rule.getRegex() != null && !rule.getRegex().trim().isEmpty()) {
            return "Regex: " + rule.getRegex();
        }
        return "any";
    }

    /**
     * 验证规则基本有效性
     */
    public boolean isValid() {
        // 必须有TTL
        if (ttl == null || ttl.trim().isEmpty()) {
            return false;
        }
        
        // 必须有至少一个匹配条件
        boolean hasMatchCondition = (tables != null && !tables.isEmpty()) ||
                                  (tablesAny != null && !tablesAny.isEmpty()) ||
                                  (tablesAll != null && !tablesAll.isEmpty()) ||
                                  (queryIds != null && !queryIds.isEmpty()) ||
                                  (regex != null && !regex.trim().isEmpty());
        
        if (!hasMatchCondition && ruleType != RuleType.ALL) {
            return false;
        }
        
        // 优先级必须非负
        return priority >= 0;
    }

    @Override
    public String toString() {
        return "CacheRuleInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ttl='" + ttl + '\'' +
                ", ruleType=" + ruleType +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}
