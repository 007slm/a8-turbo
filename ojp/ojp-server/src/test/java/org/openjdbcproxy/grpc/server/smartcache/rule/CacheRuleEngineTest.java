package org.openjdbcproxy.grpc.server.smartcache.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRule.CacheAction;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheRuleEngine
 */
@DisplayName("Cache Rule Engine Tests")
class CacheRuleEngineTest {

    private CacheRuleEngine ruleEngine;
    private QueryContext testQueryContext;

    @BeforeEach
    void setUp() {
        ruleEngine = new CacheRuleEngine();
        
        // Create test query context
        testQueryContext = QueryContext.builder()
                .sql("SELECT * FROM users WHERE id = ?")
                .normalizedSql("select * from users where id = ?")
                .tableNames(List.of("users"))
                .queryType(QueryContext.QueryType.SELECT)
                .inTransaction(false)
                .transactionHasWrites(false)
                .build();
    }

    @Test
    @DisplayName("Should return empty when no rules are configured")
    void shouldReturnEmptyWhenNoRules() {
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should match table-based rule")
    void shouldMatchTableBasedRule() {
        CacheRule tableRule = new CacheRules.TableBasedRule("users", Duration.ofMinutes(5));
        ruleEngine.addRule(tableRule);
        
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        
        assertTrue(result.isPresent());
        assertTrue(result.get().isEnabled());
        assertEquals(Duration.ofMinutes(5), result.get().getTtl());
    }

    @Test
    @DisplayName("Should match regex-based rule")
    void shouldMatchRegexBasedRule() {
        CacheRule regexRule = new CacheRules.RegexBasedRule(".*users.*", Duration.ofMinutes(10));
        ruleEngine.addRule(regexRule);
        
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        
        assertTrue(result.isPresent());
        assertTrue(result.get().isEnabled());
        assertEquals(Duration.ofMinutes(10), result.get().getTtl());
    }

    @Test
    @DisplayName("Should match query type rule")
    void shouldMatchQueryTypeRule() {
        CacheRule queryTypeRule = new CacheRules.QueryTypeRule(QueryContext.QueryType.SELECT, Duration.ofMinutes(15));
        ruleEngine.addRule(queryTypeRule);
        
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        
        assertTrue(result.isPresent());
        assertTrue(result.get().isEnabled());
        assertEquals(Duration.ofMinutes(15), result.get().getTtl());
    }

    @Test
    @DisplayName("Should respect rule priority")
    void shouldRespectRulePriority() {
        // Add rules with different priorities
        CacheRule lowPriorityRule = new CacheRules.TableBasedRule("users", Duration.ofMinutes(5), 
                                                                  CacheRule.Control.CONTINUE, 1);
        CacheRule highPriorityRule = new CacheRules.TableBasedRule("users", Duration.ofMinutes(10), 
                                                                   CacheRule.Control.CONTINUE, 10);
        
        ruleEngine.addRule(lowPriorityRule);
        ruleEngine.addRule(highPriorityRule);
        
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        
        assertTrue(result.isPresent());
        // Should match high priority rule first
        assertEquals(Duration.ofMinutes(10), result.get().getTtl());
    }

    @Test
    @DisplayName("Should handle transaction-aware rule")
    void shouldHandleTransactionAwareRule() {
        CacheRule transactionRule = new CacheRules.TransactionAwareRule();
        ruleEngine.addRule(transactionRule);
        
        // Test with transaction that has writes
        QueryContext transactionContext = QueryContext.builder()
                .sql("SELECT * FROM users")
                .normalizedSql("select * from users")
                .tableNames(List.of("users"))
                .queryType(QueryContext.QueryType.SELECT)
                .inTransaction(true)
                .transactionHasWrites(true)
                .build();
        
        Optional<CacheAction> result = ruleEngine.evaluate(transactionContext);
        
        assertTrue(result.isPresent());
        assertFalse(result.get().isEnabled()); // Should disable caching
    }

    @Test
    @DisplayName("Should handle STOP control correctly")
    void shouldHandleStopControl() {
        CacheRule stopRule = new CacheRules.TableBasedRule("users", Duration.ofMinutes(5), 
                                                           CacheRule.Control.STOP, 10);
        CacheRule continueRule = new CacheRules.TableBasedRule("users", Duration.ofMinutes(10), 
                                                              CacheRule.Control.CONTINUE, 1);
        
        ruleEngine.addRule(stopRule);
        ruleEngine.addRule(continueRule);
        
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        
        assertTrue(result.isPresent());
        // Should match stop rule and not continue to next rule
        assertEquals(Duration.ofMinutes(5), result.get().getTtl());
    }

    @Test
    @DisplayName("Should handle rule evaluation errors gracefully")
    void shouldHandleRuleEvaluationErrors() {
        // Create a rule that throws exception
        CacheRule faultyRule = new CacheRule() {
            @Override
            public java.util.function.Predicate<QueryContext> getCondition() {
                return ctx -> {
                    throw new RuntimeException("Test exception");
                };
            }
            
            @Override
            public CacheAction getAction() {
                return new AbstractCacheRule.SimpleCacheAction(Duration.ofMinutes(5));
            }
            
            @Override
            public Control getControl() {
                return Control.CONTINUE;
            }
        };
        
        CacheRule goodRule = new CacheRules.TableBasedRule("users", Duration.ofMinutes(10));
        
        ruleEngine.addRule(faultyRule);
        ruleEngine.addRule(goodRule);
        
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        
        // Should continue to good rule despite faulty rule
        assertTrue(result.isPresent());
        assertEquals(Duration.ofMinutes(10), result.get().getTtl());
    }

    @Test
    @DisplayName("Should support removing rules")
    void shouldSupportRemovingRules() {
        CacheRule rule1 = new CacheRules.TableBasedRule("users", Duration.ofMinutes(5));
        CacheRule rule2 = new CacheRules.TableBasedRule("products", Duration.ofMinutes(10));
        
        ruleEngine.addRule(rule1);
        ruleEngine.addRule(rule2);
        assertEquals(2, ruleEngine.getRuleCount());
        
        ruleEngine.removeRule(rule1);
        assertEquals(1, ruleEngine.getRuleCount());
        
        // Should no longer match rule1
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        assertTrue(result.isEmpty()); // No rule matches "users" table now
    }

    @Test
    @DisplayName("Should support clearing all rules")
    void shouldSupportClearingAllRules() {
        CacheRule rule1 = new CacheRules.TableBasedRule("users", Duration.ofMinutes(5));
        CacheRule rule2 = new CacheRules.TableBasedRule("products", Duration.ofMinutes(10));
        
        ruleEngine.addRule(rule1);
        ruleEngine.addRule(rule2);
        assertEquals(2, ruleEngine.getRuleCount());
        
        ruleEngine.clearRules();
        assertEquals(0, ruleEngine.getRuleCount());
        
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should support setting complete rule list")
    void shouldSupportSettingCompleteRuleList() {
        CacheRule rule1 = new CacheRules.TableBasedRule("users", Duration.ofMinutes(5));
        CacheRule rule2 = new CacheRules.TableBasedRule("products", Duration.ofMinutes(10));
        CacheRule rule3 = new CacheRules.QueryTypeRule(QueryContext.QueryType.SELECT, Duration.ofMinutes(15));
        
        List<CacheRule> newRules = Arrays.asList(rule1, rule2, rule3);
        ruleEngine.setRules(newRules);
        
        assertEquals(3, ruleEngine.getRuleCount());
        
        Optional<CacheAction> result = ruleEngine.evaluate(testQueryContext);
        assertTrue(result.isPresent());
    }
}