package org.openjdbcproxy.grpc.server.smartcache.rule;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cache rule engine that evaluates query contexts against configured rules.
 * This engine processes rules in priority order and applies the first matching rule.
 */
@Slf4j
public class CacheRuleEngine {
    
    private final List<CacheRule> rules = new CopyOnWriteArrayList<>();
    
    /**
     * Adds a cache rule to the engine
     * @param rule the rule to add
     */
    public void addRule(CacheRule rule) {
        rules.add(rule);
        sortRules();
        log.debug("Added cache rule with priority {}: {}", rule.getPriority(), rule.getClass().getSimpleName());
    }
    
    /**
     * Removes a cache rule from the engine
     * @param rule the rule to remove
     */
    public void removeRule(CacheRule rule) {
        rules.remove(rule);
        log.debug("Removed cache rule: {}", rule.getClass().getSimpleName());
    }
    
    /**
     * Clears all rules
     */
    public void clearRules() {
        rules.clear();
        log.debug("Cleared all cache rules");
    }
    
    /**
     * Sets the complete list of rules, replacing existing ones
     * @param newRules the new list of rules
     */
    public void setRules(List<CacheRule> newRules) {
        rules.clear();
        rules.addAll(newRules);
        sortRules();
        log.info("Updated cache rules, total: {}", rules.size());
    }
    
    /**
     * Evaluates a query context against all rules and returns the cache action
     * @param queryContext the query context to evaluate
     * @return the cache action result, or empty if no rules match
     */
    public Optional<CacheRule.CacheAction> evaluate(QueryContext queryContext) {
        log.debug("Evaluating query context: {}", queryContext.getSql());
        
        for (CacheRule rule : rules) {
            try {
                if (rule.getCondition().test(queryContext)) {
                    CacheRule.CacheAction action = rule.getAction();
                    log.debug("Rule matched: {} -> TTL: {}, Enabled: {}", 
                             rule.getClass().getSimpleName(), 
                             action.getTtl(), 
                             action.isEnabled());
                    
                    if (rule.getControl() == CacheRule.Control.STOP) {
                        return Optional.of(action);
                    }
                    
                    // For CONTINUE rules, we still return the first matching action
                    // but allow for more complex scenarios in the future
                    return Optional.of(action);
                }
            } catch (Exception e) {
                log.warn("Error evaluating cache rule {}: {}", rule.getClass().getSimpleName(), e.getMessage());
            }
        }
        
        log.debug("No cache rule matched for query: {}", queryContext.getSql());
        return Optional.empty();
    }
    
    /**
     * Gets the current list of rules (read-only)
     * @return current rules
     */
    public List<CacheRule> getRules() {
        return new ArrayList<>(rules);
    }
    
    /**
     * Gets the number of configured rules
     * @return rule count
     */
    public int getRuleCount() {
        return rules.size();
    }
    
    /**
     * Sorts rules by priority (highest first)
     */
    private void sortRules() {
        rules.sort(Comparator.comparingInt(CacheRule::getPriority).reversed());
    }
}