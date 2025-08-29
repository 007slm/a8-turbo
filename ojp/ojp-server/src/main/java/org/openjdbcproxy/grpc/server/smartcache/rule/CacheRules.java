package org.openjdbcproxy.grpc.server.smartcache.rule;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Concrete cache rule implementations for common scenarios
 */
public class CacheRules {
    
    /**
     * Table-based cache rule that matches queries based on table names
     */
    public static class TableBasedRule extends AbstractCacheRule {
        private final Predicate<QueryContext> condition;
        
        public TableBasedRule(String tableName, Duration ttl) {
            super(enableCache(ttl));
            this.condition = ctx -> ctx.getTableNames() != null && 
                                  ctx.getTableNames().contains(tableName.toLowerCase());
        }
        
        public TableBasedRule(String tableName, Duration ttl, Control control, int priority) {
            super(enableCache(ttl), control, priority);
            this.condition = ctx -> ctx.getTableNames() != null && 
                                  ctx.getTableNames().contains(tableName.toLowerCase());
        }
        
        @Override
        public Predicate<QueryContext> getCondition() {
            return condition;
        }
    }
    
    /**
     * Regex-based cache rule that matches SQL patterns
     */
    public static class RegexBasedRule extends AbstractCacheRule {
        private final Pattern pattern;
        
        public RegexBasedRule(String regex, Duration ttl) {
            super(enableCache(ttl));
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
        
        public RegexBasedRule(String regex, Duration ttl, Control control, int priority) {
            super(enableCache(ttl), control, priority);
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
        
        @Override
        public Predicate<QueryContext> getCondition() {
            return ctx -> ctx.getNormalizedSql() != null && 
                         pattern.matcher(ctx.getNormalizedSql()).find();
        }
    }
    
    /**
     * Query type based rule (e.g., only cache SELECT statements)
     */
    public static class QueryTypeRule extends AbstractCacheRule {
        private final QueryContext.QueryType targetType;
        
        public QueryTypeRule(QueryContext.QueryType queryType, Duration ttl) {
            super(enableCache(ttl));
            this.targetType = queryType;
        }
        
        public QueryTypeRule(QueryContext.QueryType queryType, Duration ttl, Control control, int priority) {
            super(enableCache(ttl), control, priority);
            this.targetType = queryType;
        }
        
        @Override
        public Predicate<QueryContext> getCondition() {
            return ctx -> ctx.getQueryType() == targetType;
        }
    }
    
    /**
     * Transaction-aware rule that disables caching for queries in transactions with writes
     */
    public static class TransactionAwareRule extends AbstractCacheRule {
        
        public TransactionAwareRule() {
            super(disableCache(), Control.STOP, Integer.MAX_VALUE); // Highest priority
        }
        
        @Override
        public Predicate<QueryContext> getCondition() {
            return ctx -> ctx.isInTransaction() && ctx.isTransactionHasWrites();
        }
    }
    
    /**
     * Composite rule that combines multiple conditions with AND logic
     */
    public static class CompositeAndRule extends AbstractCacheRule {
        private final Predicate<QueryContext> condition;
        
        @SafeVarargs
        public CompositeAndRule(Duration ttl, Predicate<QueryContext>... conditions) {
            super(enableCache(ttl));
            this.condition = ctx -> {
                for (Predicate<QueryContext> cond : conditions) {
                    if (!cond.test(ctx)) {
                        return false;
                    }
                }
                return true;
            };
        }
        
        @Override
        public Predicate<QueryContext> getCondition() {
            return condition;
        }
    }
    
    /**
     * Default rule that disables caching for all non-SELECT queries
     */
    public static class DefaultRule extends AbstractCacheRule {
        
        public DefaultRule() {
            super(disableCache(), Control.STOP, -1); // Lowest priority
        }
        
        @Override
        public Predicate<QueryContext> getCondition() {
            return ctx -> ctx.getQueryType() != QueryContext.QueryType.SELECT;
        }
    }
}