package org.openjdbcproxy.grpc.server.smartcache.config;

import lombok.Builder;
import lombok.Data;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRule;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRules;
import org.openjdbcproxy.grpc.server.smartcache.rule.QueryContext;

import java.time.Duration;

/**
 * Configuration entry for a cache rule.
 * This defines how cache rules are configured and converted to runtime rules.
 */
@Data
@Builder
public class CacheRuleConfigEntry {
    
    /**
     * Rule name for identification
     */
    private String name;
    
    /**
     * Rule description
     */
    private String description;
    
    /**
     * Whether this rule is enabled
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * Rule type
     */
    private RuleType type;
    
    /**
     * Rule pattern (interpretation depends on type)
     */
    private String pattern;
    
    /**
     * Cache TTL for this rule
     */
    @Builder.Default
    private Duration ttl = Duration.ofMinutes(10);
    
    /**
     * Rule priority (higher number = higher priority)
     */
    @Builder.Default
    private int priority = 0;
    
    /**
     * Control behavior after rule matches
     */
    @Builder.Default
    private CacheRule.Control control = CacheRule.Control.CONTINUE;
    
    /**
     * Cache key suffix for this rule
     */
    private String keySuffix;
    
    /**
     * Rule types supported
     */
    public enum RuleType {
        TABLE_NAME,        // Match by table name
        REGEX,             // Match by SQL regex pattern
        QUERY_TYPE,        // Match by query type (SELECT, INSERT, etc.)
        TRANSACTION_AWARE, // Special rule for transaction safety
        DEFAULT            // Default catch-all rule
    }
    
    /**
     * Validates the rule configuration
     */
    public void validate() throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name cannot be null or empty");
        }
        
        if (type == null) {
            throw new IllegalArgumentException("Rule type cannot be null");
        }
        
        if (pattern == null || pattern.trim().isEmpty()) {
            if (type != RuleType.TRANSACTION_AWARE && type != RuleType.DEFAULT) {
                throw new IllegalArgumentException("Rule pattern cannot be null or empty for type: " + type);
            }
        }
        
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("Rule TTL must be positive");
        }
        
        if (control == null) {
            throw new IllegalArgumentException("Rule control cannot be null");
        }
        
        // Type-specific validation
        switch (type) {
            case QUERY_TYPE:
                try {
                    QueryContext.QueryType.valueOf(pattern.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid query type pattern: " + pattern);
                }
                break;
            case REGEX:
                try {
                    java.util.regex.Pattern.compile(pattern);
                } catch (java.util.regex.PatternSyntaxException e) {
                    throw new IllegalArgumentException("Invalid regex pattern: " + pattern);
                }
                break;
        }
    }
    
    /**
     * Converts configuration entry to runtime cache rule
     */
    public CacheRule toRuntimeRule() {
        if (!enabled) {
            return null;
        }
        
        return switch (type) {
            case TABLE_NAME -> new CacheRules.TableBasedRule(pattern, ttl, control, priority);
            case REGEX -> new CacheRules.RegexBasedRule(pattern, ttl, control, priority);
            case QUERY_TYPE -> new CacheRules.QueryTypeRule(
                    QueryContext.QueryType.valueOf(pattern.toUpperCase()), ttl, control, priority);
            case TRANSACTION_AWARE -> new CacheRules.TransactionAwareRule();
            case DEFAULT -> new CacheRules.DefaultRule();
        };
    }
    
    /**
     * Creates a table-based rule configuration
     */
    public static CacheRuleConfigEntry tableRule(String name, String tableName, Duration ttl) {
        return CacheRuleConfigEntry.builder()
                .name(name)
                .description("Cache rule for table: " + tableName)
                .type(RuleType.TABLE_NAME)
                .pattern(tableName)
                .ttl(ttl)
                .build();
    }
    
    /**
     * Creates a regex-based rule configuration
     */
    public static CacheRuleConfigEntry regexRule(String name, String regex, Duration ttl) {
        return CacheRuleConfigEntry.builder()
                .name(name)
                .description("Cache rule for regex: " + regex)
                .type(RuleType.REGEX)
                .pattern(regex)
                .ttl(ttl)
                .build();
    }
    
    /**
     * Creates a query type rule configuration
     */
    public static CacheRuleConfigEntry queryTypeRule(String name, QueryContext.QueryType queryType, Duration ttl) {
        return CacheRuleConfigEntry.builder()
                .name(name)
                .description("Cache rule for query type: " + queryType)
                .type(RuleType.QUERY_TYPE)
                .pattern(queryType.name())
                .ttl(ttl)
                .build();
    }
    
    /**
     * Creates transaction-aware rule configuration
     */
    public static CacheRuleConfigEntry transactionAwareRule() {
        return CacheRuleConfigEntry.builder()
                .name("transaction_aware")
                .description("Prevents caching in unsafe transaction states")
                .type(RuleType.TRANSACTION_AWARE)
                .priority(Integer.MAX_VALUE)
                .control(CacheRule.Control.STOP)
                .build();
    }
    
    /**
     * Creates default rule configuration
     */
    public static CacheRuleConfigEntry defaultRule() {
        return CacheRuleConfigEntry.builder()
                .name("default")
                .description("Default rule to disable caching for non-SELECT queries")
                .type(RuleType.DEFAULT)
                .priority(-1)
                .control(CacheRule.Control.STOP)
                .build();
    }
}