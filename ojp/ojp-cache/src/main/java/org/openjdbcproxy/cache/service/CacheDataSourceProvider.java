package org.openjdbcproxy.cache.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.utils.JdbcUrlUtil;
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
    
    private final ConcurrentHashMap<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    /**
     * 根据数据库名称获取连接
     */
    public Connection acquireConnectionByDbName(String connHash) throws SQLException {
        HikariDataSource dataSource = dataSources.computeIfAbsent(connHash, _connHash -> {
            return createStarRocksDataSource(_connHash);
        });
        return dataSource.getConnection();
    }

    /**
     * 创建StarRocks数据源
     */
    private HikariDataSource createStarRocksDataSource(String connHash) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(resolveStarRockJdbcUrl(connHash));
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
        config.setPoolName("StarRocks-Cache-" + connHash);
        
        // 连接测试
        config.setConnectionTestQuery("SELECT 1");
        
        log.info("正在为数据库创建 StarRocks 数据源: {}, URL: {}",
                connHash, config.getJdbcUrl());
        
        return new HikariDataSource(config);
    }
    
    /**
     * 解析数据库的JDBC URL
     */
    private String resolveStarRockJdbcUrl(String connHash) {
        String databaseName = JdbcUrlUtil.extractDatabaseName(connHash);
        if (starrocksUrl.contains("?")) {
            // URL已包含参数
            return starrocksUrl.replace("?", "/" + databaseName + "?");
        } else {
            // URL不包含参数
            return starrocksUrl + "/" + databaseName;
        }
    }
    
    @Override
    public void destroy() {
        dataSources.values().forEach(ds -> {
            if (ds != null && !ds.isClosed()) {
                ds.close();
            }
        });
        dataSources.clear();
    }
}