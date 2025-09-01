package org.openjdbcproxy.grpc.server.interceptor.impl;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Context information for query interception
 */
@Data
@Builder
public class QueryInterceptContext {
    
    /**
     * The SQL statement being executed
     */
    private String sql;
    
    /**
     * Query parameters for prepared statements
     */
    private String parameters;
    
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
     * Additional context metadata
     */
    private Map<String, Object> metadata;
}