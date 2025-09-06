package org.openjdbcproxy.cache.util;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL解析工具类
 */
@Slf4j
@Component
public class SqlParseUtil {

    // SQL类型匹配模式
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_PATTERN = Pattern.compile("^\\s*INSERT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("^\\s*UPDATE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_PATTERN = Pattern.compile("^\\s*DELETE\\s+", Pattern.CASE_INSENSITIVE);
    
    // 表名提取模式
    private static final Pattern FROM_PATTERN = Pattern.compile(
        "\\bFROM\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)", 
        Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_PATTERN = Pattern.compile(
        "\\b(?:INNER\\s+|LEFT\\s+|RIGHT\\s+|FULL\\s+)?JOIN\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)", 
        Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_TABLE_PATTERN = Pattern.compile(
        "^\\s*UPDATE\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)", 
        Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_TABLE_PATTERN = Pattern.compile(
        "^\\s*INSERT\\s+INTO\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)", 
        Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_TABLE_PATTERN = Pattern.compile(
        "^\\s*DELETE\\s+FROM\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)", 
        Pattern.CASE_INSENSITIVE);
    
    // 参数占位符模式
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\?|:[a-zA-Z_][a-zA-Z0-9_]*|#\\{[^}]+\\}");
    
    /**
     * 解析SQL语句，创建Query对象
     */
    public static Query parseQuery(String sql, String dataSourceName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(dataSourceName)) {
            throw new IllegalArgumentException("SQL and dataSourceName cannot be empty");
        }
        
        String trimmedSql = sql.trim();
        String normalizedSql = normalizeSql(trimmedSql);
        String queryType = getQueryType(trimmedSql);
        Set<String> tableNames = extractTableNames(trimmedSql, queryType);
        String parameterHash = generateParameterHash(normalizedSql);
        String queryId = generateQueryId(parameterHash, dataSourceName);
        
        return Query.builder()
            .queryId(queryId)
            .sql(trimmedSql)
            .normalizedSql(normalizedSql)
            .tables(new ArrayList<>(tableNames))
            .parameterHash(parameterHash)
            .queryType(Query.QueryType.valueOf(queryType.toUpperCase()))
            .datasourceName(dataSourceName)
            .createdAt(LocalDateTime.now())
            .lastAccessTime(LocalDateTime.now())
            .accessCount(0L)
            .avgResponseTime(0.0)
            .cacheHitCount(0L)
            .cacheHitRate(0.0)
            .build();
    }
    
    /**
     * 规范化SQL语句（去除多余空格，统一大小写）
     */
    public static String normalizeSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        
        return sql.trim()
            .replaceAll("\\s+", " ")  // 多个空白字符替换为单个空格
            .replaceAll("\\s*,\\s*", ", ")  // 逗号前后的空格标准化
            .replaceAll("\\s*\\(\\s*", "(")  // 左括号前后的空格
            .replaceAll("\\s*\\)\\s*", ")")  // 右括号前后的空格
            .replaceAll("\\s*=\\s*", " = ")  // 等号前后的空格
            .replaceAll("\\s*<\\s*", " < ")  // 小于号前后的空格
            .replaceAll("\\s*>\\s*", " > ")  // 大于号前后的空格
            .toUpperCase();  // 转为大写
    }
    
    /**
     * 获取SQL查询类型
     */
    public static String getQueryType(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "UNKNOWN";
        }
        
        String trimmedSql = sql.trim();
        
        if (SELECT_PATTERN.matcher(trimmedSql).find()) {
            return "SELECT";
        } else if (INSERT_PATTERN.matcher(trimmedSql).find()) {
            return "INSERT";
        } else if (UPDATE_PATTERN.matcher(trimmedSql).find()) {
            return "UPDATE";
        } else if (DELETE_PATTERN.matcher(trimmedSql).find()) {
            return "DELETE";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 提取SQL中涉及的表名
     */
    public static Set<String> extractTableNames(String sql, String queryType) {
        Set<String> tableNames = new HashSet<>();
        
        if (!StringUtils.hasText(sql)) {
            return tableNames;
        }
        
        try {
            switch (queryType.toUpperCase()) {
                case "SELECT":
                    extractSelectTableNames(sql, tableNames);
                    break;
                case "INSERT":
                    extractInsertTableNames(sql, tableNames);
                    break;
                case "UPDATE":
                    extractUpdateTableNames(sql, tableNames);
                    break;
                case "DELETE":
                    extractDeleteTableNames(sql, tableNames);
                    break;
                default:
                    log.warn("Unknown query type: {}", queryType);
            }
        } catch (Exception e) {
            log.error("Failed to extract table names from SQL: {}", sql, e);
        }
        
        return tableNames;
    }
    
    /**
     * 生成参数哈希值
     */
    public static String generateParameterHash(String normalizedSql) {
        if (!StringUtils.hasText(normalizedSql)) {
            return "";
        }
        
        // 将参数占位符替换为统一的占位符
        String parameterizedSql = PARAMETER_PATTERN.matcher(normalizedSql).replaceAll("?");
        
        return generateMD5Hash(parameterizedSql);
    }
    
    /**
     * 生成查询ID
     */
    public static String generateQueryId(String parameterHash, String dataSourceName) {
        String combined = dataSourceName + ":" + parameterHash;
        return "query_" + generateMD5Hash(combined).substring(0, 16);
    }
    
    /**
     * 提取SELECT语句中的表名
     */
    private static void extractSelectTableNames(String sql, Set<String> tableNames) {
        // 提取FROM子句中的表名
        Matcher fromMatcher = FROM_PATTERN.matcher(sql);
        while (fromMatcher.find()) {
            String tableName = extractTableName(fromMatcher.group(1));
            if (StringUtils.hasText(tableName)) {
                tableNames.add(tableName);
            }
        }
        
        // 提取JOIN子句中的表名
        Matcher joinMatcher = JOIN_PATTERN.matcher(sql);
        while (joinMatcher.find()) {
            String tableName = extractTableName(joinMatcher.group(1));
            if (StringUtils.hasText(tableName)) {
                tableNames.add(tableName);
            }
        }
    }
    
    /**
     * 提取INSERT语句中的表名
     */
    private static void extractInsertTableNames(String sql, Set<String> tableNames) {
        Matcher matcher = INSERT_TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = extractTableName(matcher.group(1));
            if (StringUtils.hasText(tableName)) {
                tableNames.add(tableName);
            }
        }
    }
    
    /**
     * 提取UPDATE语句中的表名
     */
    private static void extractUpdateTableNames(String sql, Set<String> tableNames) {
        Matcher matcher = UPDATE_TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = extractTableName(matcher.group(1));
            if (StringUtils.hasText(tableName)) {
                tableNames.add(tableName);
            }
        }
    }
    
    /**
     * 提取DELETE语句中的表名
     */
    private static void extractDeleteTableNames(String sql, Set<String> tableNames) {
        Matcher matcher = DELETE_TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = extractTableName(matcher.group(1));
            if (StringUtils.hasText(tableName)) {
                tableNames.add(tableName);
            }
        }
    }
    
    /**
     * 从匹配结果中提取表名（去除schema前缀）
     */
    private static String extractTableName(String fullTableName) {
        if (!StringUtils.hasText(fullTableName)) {
            return null;
        }
        
        // 去除别名（如果有的话）
        String[] parts = fullTableName.trim().split("\\s+");
        String tableNamePart = parts[0];
        
        // 如果包含schema，只取表名部分
        if (tableNamePart.contains(".")) {
            String[] schemaParts = tableNamePart.split("\\.");
            return schemaParts[schemaParts.length - 1];
        }
        
        return tableNamePart;
    }
    
    /**
     * 生成MD5哈希值
     */
    private static String generateMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not available", e);
            return String.valueOf(input.hashCode());
        }
    }
}