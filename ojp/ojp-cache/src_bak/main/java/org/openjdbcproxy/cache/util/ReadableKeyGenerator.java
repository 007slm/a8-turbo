package org.openjdbcproxy.cache.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * 可读性Redis键名生成器
 * 生成更友好、可读性更强的Redis键名，便于调试和监控
 */
@Slf4j
@Component
public class ReadableKeyGenerator {
    
    private static final String KEY_SEPARATOR = ":";
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_-]");
    
    /**
     * 生成可读的连接标识符
     * 格式: {host}_{port}_{database}_{user}_{shortHash}
     * 
     * @param jdbcUrl JDBC连接URL
     * @param username 用户名
     * @param password 密码（用于生成短哈希，不直接暴露）
     * @return 可读的连接标识符
     */
    public String generateReadableConnectionId(String jdbcUrl, String username, String password) {
        try {
            // 解析JDBC URL
            ConnectionInfo connInfo = parseJdbcUrl(jdbcUrl);
            
            // 生成短哈希（8位）用于区分相同配置的不同连接
            String shortHash = generateShortHash(jdbcUrl + username + password);
            
            // 构建可读标识符
            StringBuilder builder = new StringBuilder();
            builder.append(sanitize(connInfo.host));
            builder.append("_").append(connInfo.port);
            if (StringUtils.hasText(connInfo.database)) {
                builder.append("_").append(sanitize(connInfo.database));
            }
            if (StringUtils.hasText(username)) {
                builder.append("_").append(sanitize(username));
            }
            builder.append("_").append(shortHash);
            
            return builder.toString();
            
        } catch (Exception e) {
            log.warn("Failed to generate readable connection ID, falling back to hash", e);
            return "conn_" + generateShortHash(jdbcUrl + username + password);
        }
    }
    
    /**
     * 生成缓存键
     * 格式: ojp:cache:query:{readableConnId}:{sqlHash}:{paramsHash}
     */
    public String generateCacheKey(String readableConnId, String sqlHash, String paramsHash) {
        return String.join(KEY_SEPARATOR, "ojp", "cache", "query", readableConnId, sqlHash, paramsHash);
    }
    
    /**
     * 生成统计键
     * 格式: ojp:stats:{readableConnId}:{metric}
     */
    public String generateStatsKey(String readableConnId, String metric) {
        return String.join(KEY_SEPARATOR, "ojp", "stats", readableConnId, metric);
    }
    
    /**
     * 生成规则键
     * 格式: ojp:rules:{readableConnId}:{ruleType}
     */
    public String generateRulesKey(String readableConnId, String ruleType) {
        return String.join(KEY_SEPARATOR, "ojp", "rules", readableConnId, ruleType);
    }
    
    /**
     * 解析JDBC URL获取连接信息
     */
    private ConnectionInfo parseJdbcUrl(String jdbcUrl) {
        ConnectionInfo info = new ConnectionInfo();
        
        try {
            // 移除jdbc:前缀
            String url = jdbcUrl;
            if (url.startsWith("jdbc:")) {
                url = url.substring(5);
            }
            
            // 处理不同数据库类型
            if (url.startsWith("mysql://") || url.startsWith("postgresql://") || url.startsWith("starrocks://")) {
                URI uri = new URI(url);
                info.host = uri.getHost() != null ? uri.getHost() : "localhost";
                info.port = uri.getPort() > 0 ? String.valueOf(uri.getPort()) : getDefaultPort(url);
                info.database = extractDatabase(uri.getPath());
            } else if (url.startsWith("oracle:")) {
                // Oracle格式处理
                info = parseOracleUrl(url);
            } else {
                // 其他格式的简单处理
                info.host = "unknown";
                info.port = "0";
                info.database = "default";
            }
            
        } catch (Exception e) {
            log.debug("Failed to parse JDBC URL: {}", jdbcUrl, e);
            info.host = "unknown";
            info.port = "0";
            info.database = "default";
        }
        
        return info;
    }
    
    /**
     * 获取默认端口
     */
    private String getDefaultPort(String url) {
        if (url.startsWith("mysql://")) return "3306";
        if (url.startsWith("postgresql://")) return "5432";
        if (url.startsWith("starrocks://")) return "9030";
        return "0";
    }
    
    /**
     * 从路径中提取数据库名
     */
    private String extractDatabase(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return "default";
        }
        String dbName = path.startsWith("/") ? path.substring(1) : path;
        int queryIndex = dbName.indexOf('?');
        if (queryIndex > 0) {
            dbName = dbName.substring(0, queryIndex);
        }
        return StringUtils.hasText(dbName) ? dbName : "default";
    }
    
    /**
     * 解析Oracle URL
     */
    private ConnectionInfo parseOracleUrl(String url) {
        ConnectionInfo info = new ConnectionInfo();
        info.host = "oracle";
        info.port = "1521";
        info.database = "default";
        
        // 简单的Oracle URL解析
        if (url.contains("@")) {
            String[] parts = url.split("@");
            if (parts.length > 1) {
                String hostPart = parts[1];
                if (hostPart.contains(":")) {
                    String[] hostPortDb = hostPart.split(":");
                    if (hostPortDb.length >= 2) {
                        info.host = hostPortDb[0];
                        if (hostPortDb[1].contains("/")) {
                            String[] portDb = hostPortDb[1].split("/");
                            info.port = portDb[0];
                            if (portDb.length > 1) {
                                info.database = portDb[1];
                            }
                        } else {
                            info.port = hostPortDb[1];
                        }
                    }
                }
            }
        }
        
        return info;
    }
    
    /**
     * 清理字符串，移除无效字符
     */
    private String sanitize(String input) {
        if (!StringUtils.hasText(input)) {
            return "unknown";
        }
        return INVALID_CHARS.matcher(input).replaceAll("_").toLowerCase();
    }
    
    /**
     * 生成8位短哈希
     */
    private String generateShortHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) { // 只取前4个字节，生成8位十六进制
                sb.append(String.format("%02x", hashBytes[i] & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not available", e);
            return String.format("%08x", input.hashCode());
        }
    }
    
    /**
     * 连接信息内部类
     */
    private static class ConnectionInfo {
        String host = "unknown";
        String port = "0";
        String database = "default";
    }
}