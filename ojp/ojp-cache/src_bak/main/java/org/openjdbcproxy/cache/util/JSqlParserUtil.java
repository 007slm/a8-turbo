package org.openjdbcproxy.cache.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.openjdbcproxy.cache.entity.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 基于JSQLParser的SQL解析工具类
 * 提供精确的SQL解析能力，替代正则表达式匹配
 */
@Slf4j
@Component
public class JSqlParserUtil {

    // 参数占位符模式（保留用于参数化处理）
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\?|:[a-zA-Z_][a-zA-Z0-9_]*|#\\{[^}]+\\}");
    
    /**
     * 解析SQL语句，创建Query对象
     */
    public static Query parseQuery(String sql, String connHash) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(connHash)) {
            throw new IllegalArgumentException("SQL and connHash cannot be empty");
        }
        
        try {
            String trimmedSql = sql.trim();
            Statement statement = CCJSqlParserUtil.parse(trimmedSql);
            
            String normalizedSql = normalizeSql(trimmedSql);
            String queryType = getQueryType(statement);
            Set<String> tableNames = extractTableNames(statement);
            String parameterHash = generateParameterHash(normalizedSql);
            String queryId = generateQueryId(parameterHash, connHash);
            
            return Query.builder()
                .queryId(queryId)
                .sql(trimmedSql)
                .normalizedSql(normalizedSql)
                .tables(new ArrayList<>(tableNames))
                .parameterHash(parameterHash)
                .queryType(Query.QueryType.valueOf(queryType.toUpperCase()))
                .connHash(connHash)
                .createdAt(LocalDateTime.now())
                .lastAccessTime(LocalDateTime.now())
                .accessCount(0L)
                .avgResponseTime(0.0)
                .cacheHitCount(0L)
                .cacheHitRate(0.0)
                .build();
        } catch (JSQLParserException e) {
            log.warn("Failed to parse SQL with JSQLParser, falling back to regex: {}", sql, e);
            // 降级到原有的正则表达式解析
            return SqlParseUtil.parseQuery(sql, connHash);
        }
    }
    
    /**
     * 获取SQL查询类型
     */
    public static String getQueryType(Statement statement) {
        if (statement instanceof Select) {
            return "SELECT";
        } else if (statement instanceof Insert) {
            return "INSERT";
        } else if (statement instanceof Update) {
            return "UPDATE";
        } else if (statement instanceof Delete) {
            return "DELETE";
        } else {
            return "OTHER";
        }
    }
    
    /**
     * 提取表名
     */
    public static Set<String> extractTableNames(Statement statement) {
        try {
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(statement);
            
            Set<String> tableNames = new HashSet<>();
            for (String tableName : tableList) {
                // 处理schema.table格式，只保留表名
                String cleanTableName = extractTableName(tableName);
                if (StringUtils.hasText(cleanTableName)) {
                    tableNames.add(cleanTableName.toLowerCase());
                }
            }
            return tableNames;
        } catch (Exception e) {
            log.warn("Failed to extract table names from statement: {}", statement, e);
            return new HashSet<>();
        }
    }
    
    /**
     * 规范化SQL语句
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
     * 生成参数哈希
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
    public static String generateQueryId(String parameterHash, String connHash) {
        return generateMD5Hash(parameterHash + "_" + connHash);
    }
    
    /**
     * 提取纯表名（去除schema前缀）
     */
    private static String extractTableName(String fullTableName) {
        if (!StringUtils.hasText(fullTableName)) {
            return "";
        }
        
        // 去除引号
        String cleanName = fullTableName.replaceAll("[`\"'\\[\\]]", "");
        
        // 如果包含点号，取最后一部分作为表名
        if (cleanName.contains(".")) {
            String[] parts = cleanName.split("\\.");
            return parts[parts.length - 1].trim();
        }
        
        return cleanName.trim();
    }
    
    /**
     * 生成MD5哈希
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
    
    /**
     * 检查SQL是否为查询语句（可缓存）
     */
    public static boolean isSelectQuery(Statement statement) {
        return statement instanceof Select;
    }
    
    /**
     * 检查SQL是否为修改语句（需要清除缓存）
     */
    public static boolean isModifyQuery(Statement statement) {
        return statement instanceof Insert || 
               statement instanceof Update || 
               statement instanceof Delete;
    }
    
    /**
     * 获取SQL复杂度评分（用于缓存决策）
     */
    public static int getComplexityScore(Statement statement) {
        if (!(statement instanceof Select)) {
            return 0;
        }
        
        Select select = (Select) statement;
        int score = 0;
        
        // 基础分数
        score += 1;
        
        // 表数量加分
        Set<String> tables = extractTableNames(statement);
        score += tables.size() * 2;
        
        // SQL长度加分
        String sql = statement.toString();
        if (sql.length() > 200) score += 2;
        if (sql.length() > 500) score += 3;
        
        // 关键字加分
        String upperSql = sql.toUpperCase();
        if (upperSql.contains("JOIN")) score += 3;
        if (upperSql.contains("SUBQUERY") || upperSql.contains("(")) score += 2;
        if (upperSql.contains("GROUP BY")) score += 2;
        if (upperSql.contains("ORDER BY")) score += 1;
        if (upperSql.contains("HAVING")) score += 2;
        
        return score;
    }
}