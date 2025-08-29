package org.openjdbcproxy.grpc.server.smartcache.rule;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Cache rule interface defining the contract for query caching rules.
 * This interface is inspired by Redis Smart Cache but adapted for OJP Server architecture.
 */
public interface CacheRule {

    /**
     * Control enum to determine whether to continue or stop rule evaluation
     */
    enum Control {
        STOP, CONTINUE
    }

    /**
     * Gets the condition predicate that determines if this rule matches a query
     * @return predicate for query matching
     */
    Predicate<QueryContext> getCondition();

    /**
     * Gets the caching action to perform when this rule matches
     * @return cache action
     */
    CacheAction getAction();

    /**
     * Gets the control behavior after this rule is processed
     * @return control behavior
     */
    Control getControl();

    /**
     * Gets the priority of this rule (higher number = higher priority)
     * @return rule priority
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Cache action that defines what to do when a rule matches
     */
    interface CacheAction {
        /**
         * Gets the TTL for cached results
         * @return cache TTL
         */
        Duration getTtl();

        /**
         * Whether to enable caching for this query
         * @return true if caching should be enabled
         */
        boolean isEnabled();

        /**
         * Gets the cache key suffix for this rule
         * @return cache key suffix, null for default
         */
        default String getKeySuffix() {
            return null;
        }
    }
}