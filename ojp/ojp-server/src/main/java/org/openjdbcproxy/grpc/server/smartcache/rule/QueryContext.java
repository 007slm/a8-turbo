package org.openjdbcproxy.grpc.server.smartcache.rule;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Query context containing all information needed for cache rule evaluation.
 * This includes SQL statement, table names, parameters, and session information.
 */
@Data
@Builder
public class QueryContext {
    
    /**
     * The SQL statement being executed
     */
    private String sql;
    
    /**
     * Normalized SQL statement (for caching consistency)
     */
    private String normalizedSql;
    
    /**
     * List of table names involved in the query
     */
    private List<String> tableNames;
    
    /**
     * Query parameters for prepared statements
     */
    private Map<Integer, Object> parameters;
    
    /**
     * Session identifier
     */
    private String sessionId;
    
    /**
     * Connection hash
     */
    private String connectionHash;
    
    /**
     * Database name
     */
    private String databaseName;
    
    /**
     * Schema name
     */
    private String schemaName;
    
    /**
     * Query type (SELECT, INSERT, UPDATE, DELETE, etc.)
     */
    private QueryType queryType;
    
    /**
     * Whether the query is part of an active transaction
     */
    private boolean inTransaction;
    
    /**
     * Whether the transaction has performed any writes
     */
    private boolean transactionHasWrites;
    
    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
    
    /**
     * Query type enumeration
     */
    public enum QueryType {
        SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, TRUNCATE, UNKNOWN
    }
}