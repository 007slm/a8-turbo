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
     * 提取表名 (从 Statement)
     */
    public static Set<String> extractTableNames(Statement statement) {
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(statement);

        Map<String, String> casePreserved = new LinkedHashMap<>();
        for (String tableName : tableList) {
            // 处理schema.table格式，只保留表名
            String cleanTableName = extractTableName(tableName);
            if (StringUtils.hasText(cleanTableName)) {
                String key = cleanTableName.toLowerCase();
                casePreserved.putIfAbsent(key, cleanTableName);
            }
        }
        // 返回保留原始大小写的唯一表名集合
        return new LinkedHashSet<>(casePreserved.values());
    }
    
    /**
     * 提取表名 (从 SQL 字符串)
     */
    public static Set<String> extractTableNames(String sql) {
        if (!StringUtils.hasText(sql)) {
            return Collections.emptySet();
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            return extractTableNames(statement);
        } catch (JSQLParserException e) {
            log.warn("解析 SQL 以提取表名失败: {}", e.getMessage());
            return Collections.emptySet();
        }
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
    public static String generateMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(java.lang.String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 算法不可用", e);
            return java.lang.String.valueOf(input.hashCode());
        }
    }
    public static String generateSlowQueryId(String connHash,String sql){
        return  JSqlParserUtil.generateMD5Hash(connHash + "_" + sql);
    };

    
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
}
