package org.openjdbcproxy.grpc.server.smartcache.cache;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages connection pool for StarRocks cache database.
 * This is separate from the main database connection pools to ensure isolation.
 */
@Slf4j
public class StarRocksCacheManager implements AutoCloseable {
    
    private final StarRocksCacheConfig config;
    private final HikariDataSource dataSource;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    public StarRocksCacheManager(StarRocksCacheConfig config) {
        this.config = config;
        this.dataSource = createDataSource();
        log.info("StarRocks cache manager created with config: {}", config.getJdbcUrl());
    }
    
    /**
     * Creates and configures the HikariCP data source for StarRocks
     */
    private HikariDataSource createDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriverClassName());
        
        // Pool configuration
        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(config.getMinimumIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout().toMillis());
        hikariConfig.setIdleTimeout(config.getIdleTimeout().toMillis());
        hikariConfig.setMaxLifetime(config.getMaxLifetime().toMillis());
        hikariConfig.setValidationTimeout(config.getValidationTimeout().toMillis());
        
        // Pool naming and validation
        hikariConfig.setPoolName(config.getPoolName());
        hikariConfig.setConnectionTestQuery(config.getConnectionTestQuery());
        
        // Performance optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(hikariConfig);
    }
    
    /**
     * Initializes the cache tables if they don't exist
     */
    public void initialize() throws SQLException {
        if (initialized.get()) {
            return;
        }
        
        log.info("Initializing StarRocks cache database schema...");
        
        try (Connection conn = getConnection()) {
            createCacheTableIfNotExists(conn);
            createIndexesIfNotExist(conn);
            initialized.set(true);
            log.info("StarRocks cache database initialized successfully");
        }
    }
    
    /**
     * Creates the cache table if it doesn't exist
     */
    private void createCacheTableIfNotExists(Connection conn) throws SQLException {
        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.%s (
                cache_key VARCHAR(512) NOT NULL,
                query_hash VARCHAR(64) NOT NULL,
                result_data LONGTEXT NOT NULL,
                metadata JSON,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                expires_at DATETIME NOT NULL,
                hit_count BIGINT DEFAULT 0,
                last_hit_at DATETIME
            ) ENGINE=OLAP
            DUPLICATE KEY(cache_key)
            DISTRIBUTED BY HASH(cache_key) BUCKETS 10
            PROPERTIES (
                "replication_num" = "1"
            )
            """, config.getCacheDatabaseName(), config.getCacheTableName());
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            log.debug("Cache table created or verified: {}.{}", 
                     config.getCacheDatabaseName(), config.getCacheTableName());
        }
    }
    
    /**
     * Creates indexes for better performance
     */
    private void createIndexesIfNotExist(Connection conn) throws SQLException {
        String indexSql = String.format("""
            CREATE INDEX IF NOT EXISTS idx_expires_at 
            ON %s.%s (expires_at)
            """, config.getCacheDatabaseName(), config.getCacheTableName());
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(indexSql);
            log.debug("Cache table indexes created or verified");
        }
    }
    
    /**
     * Gets a connection from the pool
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Stores a query result in the cache
     */
    public void putCache(String cacheKey, String queryHash, String resultData, 
                        String metadata, Duration ttl) throws SQLException {
        
        String sql = String.format("""
            INSERT INTO %s.%s (cache_key, query_hash, result_data, metadata, expires_at)
            VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL ? SECOND))
            ON DUPLICATE KEY UPDATE
                result_data = VALUES(result_data),
                metadata = VALUES(metadata),
                expires_at = VALUES(expires_at),
                created_at = NOW()
            """, config.getCacheDatabaseName(), config.getCacheTableName());
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cacheKey);
            stmt.setString(2, queryHash);
            stmt.setString(3, resultData);
            stmt.setString(4, metadata);
            stmt.setLong(5, ttl.getSeconds());
            
            int affected = stmt.executeUpdate();
            log.debug("Cache entry stored: key={}, affected={}", cacheKey, affected);
        }
    }
    
    /**
     * Retrieves a cached query result
     */
    public CacheEntry getCache(String cacheKey) throws SQLException {
        String sql = String.format("""
            SELECT result_data, metadata, created_at, expires_at
            FROM %s.%s
            WHERE cache_key = ? AND expires_at > NOW()
            """, config.getCacheDatabaseName(), config.getCacheTableName());
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cacheKey);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Update hit count asynchronously
                    updateHitCount(cacheKey);
                    
                    return CacheEntry.builder()
                            .resultData(rs.getString("result_data"))
                            .metadata(rs.getString("metadata"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .expiresAt(rs.getTimestamp("expires_at").toLocalDateTime())
                            .build();
                }
            }
        }
        
        return null; // Cache miss
    }
    
    /**
     * Updates hit count for cache statistics
     */
    private void updateHitCount(String cacheKey) {
        // Run asynchronously to avoid impacting query performance
        new Thread(() -> {
            try {
                String sql = String.format("""
                    UPDATE %s.%s 
                    SET hit_count = hit_count + 1, last_hit_at = NOW()
                    WHERE cache_key = ?
                    """, config.getCacheDatabaseName(), config.getCacheTableName());
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, cacheKey);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                log.warn("Failed to update hit count for key {}: {}", cacheKey, e.getMessage());
            }
        }).start();
    }
    
    /**
     * Cleans up expired cache entries
     */
    public int cleanupExpiredEntries() throws SQLException {
        String sql = String.format("""
            DELETE FROM %s.%s WHERE expires_at < NOW()
            """, config.getCacheDatabaseName(), config.getCacheTableName());
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            int deleted = stmt.executeUpdate(sql);
            log.info("Cleaned up {} expired cache entries", deleted);
            return deleted;
        }
    }
    
    /**
     * Gets cache statistics
     */
    public CacheStats getStats() throws SQLException {
        String sql = String.format("""
            SELECT 
                COUNT(*) as total_entries,
                COUNT(CASE WHEN expires_at > NOW() THEN 1 END) as active_entries,
                COUNT(CASE WHEN expires_at <= NOW() THEN 1 END) as expired_entries,
                SUM(hit_count) as total_hits,
                AVG(hit_count) as avg_hits_per_entry
            FROM %s.%s
            """, config.getCacheDatabaseName(), config.getCacheTableName());
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return CacheStats.builder()
                        .totalEntries(rs.getLong("total_entries"))
                        .activeEntries(rs.getLong("active_entries"))
                        .expiredEntries(rs.getLong("expired_entries"))
                        .totalHits(rs.getLong("total_hits"))
                        .avgHitsPerEntry(rs.getDouble("avg_hits_per_entry"))
                        .build();
            }
        }
        
        return CacheStats.builder().build();
    }
    
    /**
     * Checks if the cache manager is healthy
     */
    public boolean isHealthy() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            log.warn("StarRocks cache health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("StarRocks cache manager closed");
        }
    }
}