package org.openjdbcproxy.cache.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.SessionInfo;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存数据源提供者
 * 管理缓存数据源连接池，支持StarRocks等OLAP数据库
 */
@Slf4j
@Component
public class CacheDataSourceProvider implements DisposableBean {
    
    @Value("${ojp.server.interceptors.cache.starrocks.url:}")
    private String starrocksUrl;
    
    @Value("${ojp.server.interceptors.cache.starrocks.username:root}")
    private String starrocksUsername;
    
    @Value("${ojp.server.interceptors.cache.starrocks.password:}")
    private String starrocksPassword;
    
    @Value("${ojp.server.interceptors.cache.starrocks.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String starrocksDriverClassName;
    
    // 按数据库名称缓存数据源
    private final ConcurrentHashMap<String, HikariDataSource> dataSourcesByDbName = new ConcurrentHashMap<>();
    
    // 按连接哈希缓存数据源
    private final ConcurrentHashMap<String, HikariDataSource> dataSourcesByConnHash = new ConcurrentHashMap<>();
    
    /**
     * 根据数据库名称获取连接
     */
    public Connection acquireConnectionByDbName(String connHash) throws SQLException {
        if (starrocksUrl == null || starrocksUrl.isEmpty()) {
            log.warn("StarRocks URL not configured, cache data source unavailable");
            return null;
        }
        
        HikariDataSource dataSource = ensureDataSourceByDbName(connHash);
        return dataSource != null ? dataSource.getConnection() : null;
    }
    
    /**
     * 根据会话信息获取连接
     */
    public Connection acquireConnectionBySessionInfo(SessionInfo sessionInfo) throws SQLException {
        String connHash = generateConnectionHash(sessionInfo);
        HikariDataSource dataSource = ensureDataSourceByConnHash(connHash, "default");
        return dataSource != null ? dataSource.getConnection() : null;
    }
    
    /**
     * 确保数据库名称对应的数据源存在
     */
    private HikariDataSource ensureDataSourceByDbName(String connHash) {
        return dataSourcesByDbName.computeIfAbsent(connHash, dbName -> {
            return createStarrocksDataSource(dbName);
        });
    }
    
    /**
     * 确保连接哈希对应的数据源存在（公共方法）
     */
    public void ensureDataSourceByConnHash(String connHash) {
        // 使用默认数据库名称，如果需要特定数据库名称，可以通过其他方法传入
        ensureDataSourceByConnHash(connHash, "cache");
    }
    
    /**
     * 确保连接哈希对应的数据源存在
     */
    private HikariDataSource ensureDataSourceByConnHash(String connHash, String databaseName) {
        return dataSourcesByConnHash.computeIfAbsent(connHash, hash -> {
            return createStarrocksDataSourceForConnHash(hash, databaseName);
        });
    }
    
    /**
     * 创建StarRocks数据源
     */
    private HikariDataSource createStarrocksDataSource(String databaseName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(resolveJdbcUrlForDatabase(databaseName));
        config.setUsername(starrocksUsername);
        config.setPassword(starrocksPassword);
        config.setDriverClassName(starrocksDriverClassName);
        
        // 连接池配置
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        // 连接池名称
        config.setPoolName("StarRocks-Cache-" + databaseName);
        
        // 连接测试
        config.setConnectionTestQuery("SELECT 1");
        
        log.info("Creating StarRocks data source for database: {}, URL: {}", 
                databaseName, config.getJdbcUrl());
        
        return new HikariDataSource(config);
    }
    
    /**
     * 为连接哈希创建StarRocks数据源
     */
    private HikariDataSource createStarrocksDataSourceForConnHash(String connHash, String databaseName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(resolveJdbcUrlForDatabase(databaseName));
        config.setUsername(starrocksUsername);
        config.setPassword(starrocksPassword);
        config.setDriverClassName(starrocksDriverClassName);
        
        // 连接池配置（较小的池大小）
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1800000);
        
        // 连接池名称
        config.setPoolName("StarRocks-Cache-Hash-" + connHash.substring(0, 8));
        
        // 连接测试
        config.setConnectionTestQuery("SELECT 1");
        
        log.info("Creating StarRocks data source for connection hash: {}, database: {}", 
                connHash, databaseName);
        
        return new HikariDataSource(config);
    }
    
    /**
     * 解析数据库的JDBC URL
     */
    private String resolveJdbcUrlForDatabase(String databaseName) {
        if (starrocksUrl.contains("?")) {
            // URL已包含参数
            return starrocksUrl.replace("?", "/" + databaseName + "?");
        } else {
            // URL不包含参数
            return starrocksUrl + "/" + databaseName;
        }
    }
    
    /**
     * 生成连接标识符，直接使用connHash提高可读性
     */
    private String generateConnectionHash(SessionInfo sessionInfo) {
        // 直接使用connHash作为连接标识符，提高可读性
        return sessionInfo.getConnHash();
    }
    
    /**
     * 获取数据源统计信息
     */
    public String getDataSourceStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Cache DataSource Statistics:\n");
        stats.append("By Database Name: ").append(dataSourcesByDbName.size()).append(" pools\n");
        stats.append("By Connection Hash: ").append(dataSourcesByConnHash.size()).append(" pools\n");
        
        dataSourcesByDbName.forEach((dbName, ds) -> {
            if (ds != null && !ds.isClosed()) {
                stats.append("  - ").append(dbName)
                     .append(": active=").append(ds.getHikariPoolMXBean().getActiveConnections())
                     .append(", idle=").append(ds.getHikariPoolMXBean().getIdleConnections())
                     .append(", total=").append(ds.getHikariPoolMXBean().getTotalConnections())
                     .append("\n");
            }
        });
        
        return stats.toString();
    }
    
    @Override
    public void destroy() {
        log.info("Closing cache data sources...");
        
        dataSourcesByDbName.values().forEach(ds -> {
            if (ds != null && !ds.isClosed()) {
                ds.close();
            }
        });
        
        dataSourcesByConnHash.values().forEach(ds -> {
            if (ds != null && !ds.isClosed()) {
                ds.close();
            }
        });
        
        dataSourcesByDbName.clear();
        dataSourcesByConnHash.clear();
        
        log.info("All cache data sources closed");
    }
}