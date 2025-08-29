package org.openjdbcproxy.grpc.server.smartcache.interceptor;

import lombok.Builder;
import lombok.Data;
import org.openjdbcproxy.grpc.server.smartcache.rule.QueryContext;
import org.openjdbcproxy.grpc.server.smartcache.parser.SqlParser;

import java.util.Map;
import java.util.HashMap;

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
     * Additional context metadata
     */
    private Map<String, Object> metadata;
    
    /**
     * Converts to QueryContext for rule evaluation
     */
    public QueryContext toQueryContext() {
        SqlParser parser = new SqlParser();
        String normalizedSql = parser.normalizeSql(sql);
        
        return QueryContext.builder()
                .sql(sql)
                .normalizedSql(normalizedSql)
                .tableNames(parser.extractTableNames(sql))
                .parameters(parameters)
                .sessionId(sessionId)
                .connectionHash(connectionHash)
                .databaseName(databaseName)
                .schemaName(schemaName)
                .queryType(parser.getQueryType(sql))
                .inTransaction(false) // Will be updated by transaction tracker
                .transactionHasWrites(false) // Will be updated by transaction tracker
                .metadata(metadata != null ? metadata : new HashMap<>())
                .build();
    }
}