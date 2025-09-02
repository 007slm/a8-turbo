package org.openjdbcproxy.grpc.server.interceptor.impl.cache;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供缓存使用的数据源（例如 StarRocks）。
 * 仅在 smart.cache.starrocks.url 存在时才会初始化连接池，且按需惰性创建。
 */
@Slf4j
@Component
public class CacheDataSourceProvider implements DisposableBean {
    @Value("${ojp.server.interceptors.cache.starrocks.url:}")
    private String srUrl;

    @Value("${ojp.server.interceptors.cache.starrocks.username:}")
    private String srUsername;

    @Value("${ojp.server.interceptors.cache.starrocks.password:}")
    private String srPassword;

    @Value("${ojp.server.interceptors.cache.starrocks.driver:com.starrocks.jdbc.Driver}")
    private String srDriver;

    @Value("${ojp.server.interceptors.cache.starrocks.pool.max:10}")
    private int srPoolMax;

    @Value("${ojp.server.interceptors.cache.starrocks.pool.min:2}")
    private int srPoolMin;

    @Value("${ojp.server.interceptors.cache.starrocks.pool.idle-timeout:600000}")
    private long srPoolIdleTimeoutMs;

    @Value("${ojp.server.interceptors.cache.starrocks.pool.max-lifetime:1800000}")
    private long srPoolMaxLifetimeMs;

    private final Map<String, HikariDataSource> dataSourceByDbName = new ConcurrentHashMap<>();
    // 以 connHash 为维度的缓存数据源映射，便于与上游连接一一对应
    private final Map<String, HikariDataSource> dataSourceByConnHash = new ConcurrentHashMap<>();

    /**
     * 按 connHash 维度确保存在一个缓存数据源（可用于隔离不同上游连接的缓存库）。
     * 如果业务不需要分hash隔离，也可以复用同一池，此处先按 hash 建立便于扩展。
     */
    public void ensureDataSourceByDbName(String databaseName) {
        dataSourceByDbName.computeIfAbsent(databaseName, this::createStarrocksDataSource);
    }

    public java.sql.Connection acquireConnectionByDbName(String databaseName) throws java.sql.SQLException {
        if (databaseName == null || databaseName.isBlank()) {
            throw new java.sql.SQLException("Database name required for smart cache");
        }
        HikariDataSource ds = dataSourceByDbName.computeIfAbsent(databaseName, this::createStarrocksDataSource);
        return ds.getConnection();
    }

    /**
     * 以 connHash 维度确保存在一个缓存数据源。
     */
    public void ensureDataSourceByConnHash(String connHash) {
        dataSourceByConnHash.computeIfAbsent(connHash, this::createStarrocksDataSourceForConnHash);
    }

    /**
     * 获取基于 connHash 的缓存连接。
     */
    public java.sql.Connection acquireConnectionByConnHash(String connHash) throws java.sql.SQLException {
        HikariDataSource ds = dataSourceByConnHash.computeIfAbsent(connHash, this::createStarrocksDataSourceForConnHash);
        return ds.getConnection();
    }

    private HikariDataSource createStarrocksDataSource(String key) {
        try {
            HikariConfig cfg = new HikariConfig();
            String jdbcUrl = resolveJdbcUrlForDatabase(key);
            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(srUsername);
            cfg.setPassword(srPassword);
            cfg.setDriverClassName(srDriver);
            cfg.setMaximumPoolSize(srPoolMax);
            cfg.setMinimumIdle(srPoolMin);
            cfg.setIdleTimeout(srPoolIdleTimeoutMs);
            cfg.setMaxLifetime(srPoolMaxLifetimeMs);
            cfg.setPoolName("smart-cache-starrocks-" + key);
            return new HikariDataSource(cfg);
        } catch (Exception e) {
            log.error("Failed to create StarRocks datasource for key {}", key, e);
            throw e;
        }
    }

    /**
     * 基于 connHash 创建数据源。
     */
    private HikariDataSource createStarrocksDataSourceForConnHash(String connHash) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(srUrl);
        cfg.setUsername(srUsername);
        cfg.setPassword(srPassword);
        cfg.setDriverClassName(srDriver);
        cfg.setMaximumPoolSize(srPoolMax);
        cfg.setMinimumIdle(srPoolMin);
        cfg.setIdleTimeout(srPoolIdleTimeoutMs);
        cfg.setMaxLifetime(srPoolMaxLifetimeMs);
        cfg.setPoolName("smart-cache-starrocks-" + connHash.substring(0, Math.min(8, connHash.length())));
        return new HikariDataSource(cfg);
    }

    /**
     * 依据数据库名构造最终缓存 JDBC URL。
     * 支持两种方式：
     * 1) 若配置中包含占位符 ${db}，则进行替换
     * 2) 否则在末尾尝试追加 "/" + db（若原URL不以数据库路径结束）
     */
    private String resolveJdbcUrlForDatabase(String databaseName) {
        String starrocksUrl = srUrl;
        // 简单追加，避免重复斜杠
        if (starrocksUrl.endsWith("/" + databaseName) || starrocksUrl.endsWith("/" + databaseName + "?")) {
            return starrocksUrl;
        }
        if (starrocksUrl.contains("?")) {
            int idx = starrocksUrl.indexOf('?');
            String base = starrocksUrl.substring(0, idx);
            String qs = starrocksUrl.substring(idx);
            if (base.endsWith("/")) {
                return base + databaseName + qs;
            }
            return base + "/" + databaseName + qs;
        }
        if (starrocksUrl.endsWith("/")) {
            return starrocksUrl + databaseName;
        }
        return starrocksUrl + "/" + databaseName;
    }

    @Override
    public void destroy() {
        for (HikariDataSource ds : dataSourceByDbName.values()) {
            try { ds.close(); } catch (Exception ignore) {}
        }
        dataSourceByDbName.clear();
        for (HikariDataSource ds : dataSourceByConnHash.values()) {
            try { ds.close(); } catch (Exception ignore) {}
        }
        dataSourceByConnHash.clear();
    }
}


