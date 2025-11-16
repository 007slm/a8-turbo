package org.openjdbcproxy.grpc.server.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * SQL parser for extracting query information needed for cache decisions.
 * This is a simplified parser focused on the most common SQL patterns.
 */
@Slf4j
public class SqlParser {
    
    // Regex patterns for SQL analysis
    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "^\\s*INSERT\\s+", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern UPDATE_PATTERN = Pattern.compile(
            "^\\s*UPDATE\\s+", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern DELETE_PATTERN = Pattern.compile(
            "^\\s*DELETE\\s+", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern CREATE_PATTERN = Pattern.compile(
            "^\\s*CREATE\\s+", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern DROP_PATTERN = Pattern.compile(
            "^\\s*DROP\\s+", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern ALTER_PATTERN = Pattern.compile(
            "^\\s*ALTER\\s+", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern TRUNCATE_PATTERN = Pattern.compile(
            "^\\s*TRUNCATE\\s+", Pattern.CASE_INSENSITIVE);
    
    // Pattern to extract table names from SELECT statements
    private static final Pattern FROM_PATTERN = Pattern.compile(
            "\\bFROM\\s+([\\w\\.`\"\\[\\]]+(?:\\s*,\\s*[\\w\\.`\"\\[\\]]+)*)", 
            Pattern.CASE_INSENSITIVE);
    
    // Pattern to extract table names from JOIN clauses
    private static final Pattern JOIN_PATTERN = Pattern.compile(
            "\\bJOIN\\s+([\\w\\.`\"\\[\\]]+)", 
            Pattern.CASE_INSENSITIVE);
    
    // Pattern to extract table names from INSERT statements
    private static final Pattern INSERT_INTO_PATTERN = Pattern.compile(
            "\\bINSERT\\s+INTO\\s+([\\w\\.`\"\\[\\]]+)", 
            Pattern.CASE_INSENSITIVE);
    
    // Pattern to extract table names from UPDATE statements
    private static final Pattern UPDATE_TABLE_PATTERN = Pattern.compile(
            "\\bUPDATE\\s+([\\w\\.`\"\\[\\]]+)", 
            Pattern.CASE_INSENSITIVE);
    
    // Pattern to extract table names from DELETE statements
    private static final Pattern DELETE_FROM_PATTERN = Pattern.compile(
            "\\bDELETE\\s+FROM\\s+([\\w\\.`\"\\[\\]]+)", 
            Pattern.CASE_INSENSITIVE);
    
    /**
     * Determines the query type from SQL statement
     */
    public QueryType getQueryType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return QueryType.UNKNOWN;
        }
        
        String trimmedSql = sql.trim();
        
        if (SELECT_PATTERN.matcher(trimmedSql).find()) {
            return QueryType.SELECT;
        } else if (INSERT_PATTERN.matcher(trimmedSql).find()) {
            return QueryType.INSERT;
        } else if (UPDATE_PATTERN.matcher(trimmedSql).find()) {
            return QueryType.UPDATE;
        } else if (DELETE_PATTERN.matcher(trimmedSql).find()) {
            return QueryType.DELETE;
        } else if (CREATE_PATTERN.matcher(trimmedSql).find()) {
            return QueryType.CREATE;
        } else if (DROP_PATTERN.matcher(trimmedSql).find()) {
            return QueryType.DROP;
        } else if (ALTER_PATTERN.matcher(trimmedSql).find()) {
            return QueryType.ALTER;
        } else if (TRUNCATE_PATTERN.matcher(trimmedSql).find()) {
            return QueryType.TRUNCATE;
        }
        
        return QueryType.UNKNOWN;
    }
    
    /**
     * Extracts table names from SQL statement
     */
    public List<String> extractTableNames(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> tables = new ArrayList<>();
        QueryType queryType = getQueryType(sql);
        
        switch (queryType) {
            case QueryType.SELECT:
                tables.addAll(extractTablesFromSelect(sql));
                break;
            case QueryType.INSERT:
                tables.addAll(extractTablesFromInsert(sql));
                break;
            case QueryType.UPDATE:
                tables.addAll(extractTablesFromUpdate(sql));
                break;
            case QueryType.DELETE:
                tables.addAll(extractTablesFromDelete(sql));
                break;
            default:
                // For other query types, try to extract from general patterns
                tables.addAll(extractTablesGeneric(sql));
        }
        
        return cleanTableNames(tables);
    }
    
    /**
     * Extracts table names from SELECT statements
     */
    private List<String> extractTablesFromSelect(String sql) {
        List<String> tables = new ArrayList<>();
        
        // Extract from FROM clause
        Matcher fromMatcher = FROM_PATTERN.matcher(sql);
        if (fromMatcher.find()) {
            String fromClause = fromMatcher.group(1);
            tables.addAll(Arrays.asList(fromClause.split("\\s*,\\s*")));
        }
        
        // Extract from JOIN clauses
        Matcher joinMatcher = JOIN_PATTERN.matcher(sql);
        while (joinMatcher.find()) {
            tables.add(joinMatcher.group(1));
        }
        
        return tables;
    }
    
    /**
     * Extracts table names from INSERT statements
     */
    private List<String> extractTablesFromInsert(String sql) {
        List<String> tables = new ArrayList<>();
        
        Matcher insertMatcher = INSERT_INTO_PATTERN.matcher(sql);
        if (insertMatcher.find()) {
            tables.add(insertMatcher.group(1));
        }
        
        return tables;
    }
    
    /**
     * Extracts table names from UPDATE statements
     */
    private List<String> extractTablesFromUpdate(String sql) {
        List<String> tables = new ArrayList<>();
        
        Matcher updateMatcher = UPDATE_TABLE_PATTERN.matcher(sql);
        if (updateMatcher.find()) {
            tables.add(updateMatcher.group(1));
        }
        
        return tables;
    }
    
    /**
     * Extracts table names from DELETE statements
     */
    private List<String> extractTablesFromDelete(String sql) {
        List<String> tables = new ArrayList<>();
        
        Matcher deleteMatcher = DELETE_FROM_PATTERN.matcher(sql);
        if (deleteMatcher.find()) {
            tables.add(deleteMatcher.group(1));
        }
        
        return tables;
    }
    
    /**
     * Generic table extraction for other query types
     */
    private List<String> extractTablesGeneric(String sql) {
        // This is a fallback method for complex queries
        // Could be enhanced with a proper SQL parser library if needed
        return new ArrayList<>();
    }
    
    /**
     * Cleans and normalizes table names
     */
    private List<String> cleanTableNames(List<String> tables) {
        return tables.stream()
                .map(this::cleanTableName)
                .filter(name -> !name.isEmpty())
                .distinct()
                // 处理schema前缀，只保留表名部分
                .map(tableName -> {
                    // 如果表名包含schema（以点号分隔），只取最后一部分
                    if (tableName.contains(".")) {
                        String[] parts = tableName.split("\\.");
                        return parts[parts.length - 1];
                    }
                    return tableName;
                })
                .toList();
    }
    
    /**
     * Cleans a single table name by removing quotes, brackets, and aliases
     */
    private String cleanTableName(String tableName) {
        if (tableName == null) {
            return "";
        }
        
        // Remove quotes and brackets
        String cleaned = tableName.replaceAll("[`\"\\[\\]]", "");
        
        // Remove alias (everything after the first space)
        int spaceIndex = cleaned.indexOf(' ');
        if (spaceIndex > 0) {
            cleaned = cleaned.substring(0, spaceIndex);
        }
        
        return cleaned.trim();
    }
    
    /**
     * Normalizes SQL for consistent caching
     */
    public String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        
        return sql.trim()
                .replaceAll("\\s+", " ")  // Replace multiple spaces with single space
                .toLowerCase();           // Convert to lowercase for consistency
    }
    
    /**
     * Checks if the query is a write operation
     */
    public boolean isWriteOperation(String sql) {
        QueryType queryType = getQueryType(sql);
        return queryType == QueryType.INSERT ||
               queryType == QueryType.UPDATE ||
               queryType == QueryType.DELETE ||
               queryType == QueryType.CREATE ||
               queryType == QueryType.DROP ||
               queryType == QueryType.ALTER ||
               queryType == QueryType.TRUNCATE;
    }

    public boolean isReadOperation(String sql) {
        QueryType queryType = getQueryType(sql);
        return queryType == QueryType.SELECT;
    }
}