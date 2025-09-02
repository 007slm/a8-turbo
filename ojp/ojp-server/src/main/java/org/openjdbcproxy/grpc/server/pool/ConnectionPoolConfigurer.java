package org.openjdbcproxy.grpc.server.pool;

import com.openjdbcproxy.grpc.ConnectionDetails;
import com.zaxxer.hikari.HikariConfig;
// Removed MicrometerMetricsTrackerFactory import - using direct setMetricRegistry instead
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.constants.CommonConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Properties;

import static org.openjdbcproxy.grpc.SerializationHandler.deserialize;

/**
 * Utility class responsible for configuring HikariCP connection pools.
 * Extracted from StatementServiceImpl to reduce its responsibilities.
 * Enhanced to support Micrometer metrics integration.
 */
@Slf4j
@Component
public class ConnectionPoolConfigurer {
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    /**
     * Configures a HikariCP connection pool with connection details and client properties.
     * Enhanced to support Micrometer metrics integration.
     *
     * @param config            The HikariConfig to configure
     * @param connectionDetails The connection details containing properties
     */
    public void configureHikariPool(HikariConfig config, ConnectionDetails connectionDetails) {
        Properties clientProperties = extractClientProperties(connectionDetails);

        // Configure basic connection pool settings first
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Configure HikariCP pool settings using client properties or defaults
        config.setMaximumPoolSize(getIntProperty(clientProperties, "ojp.connection.pool.maximumPoolSize", CommonConstants.DEFAULT_MAXIMUM_POOL_SIZE));
        config.setMinimumIdle(getIntProperty(clientProperties, "ojp.connection.pool.minimumIdle", CommonConstants.DEFAULT_MINIMUM_IDLE));
        config.setIdleTimeout(getLongProperty(clientProperties, "ojp.connection.pool.idleTimeout", CommonConstants.DEFAULT_IDLE_TIMEOUT));
        config.setMaxLifetime(getLongProperty(clientProperties, "ojp.connection.pool.maxLifetime", CommonConstants.DEFAULT_MAX_LIFETIME));
        config.setConnectionTimeout(getLongProperty(clientProperties, "ojp.connection.pool.connectionTimeout", CommonConstants.DEFAULT_CONNECTION_TIMEOUT));
        
        // Additional settings for high concurrency scenarios
        config.setLeakDetectionThreshold(60000); // 60 seconds - detect connection leaks
        config.setValidationTimeout(5000);       // 5 seconds - faster validation timeout
        config.setInitializationFailTimeout(10000); // 10 seconds - fail fast on initialization issues
        
        // Set pool name for better monitoring - generate meaningful name based on connection info
        String poolName = generatePoolName(config.getJdbcUrl(), config.getUsername());
        config.setPoolName(poolName);
        
        // Enable JMX for monitoring if not explicitly disabled
        config.setRegisterMbeans(true);
        
        // Configure Micrometer metrics if available - using direct setMetricRegistry as per HikariCP docs
        if (meterRegistry != null) {
            config.setMetricRegistry(meterRegistry);
            log.info("HikariCP configured with direct MetricRegistry support for pool: {}", poolName);
        }

        log.info("HikariCP configured with maximumPoolSize={}, minimumIdle={}, connectionTimeout={}ms, poolName={}",
                config.getMaximumPoolSize(), config.getMinimumIdle(), config.getConnectionTimeout(), poolName);
    }
    
    /**
     * Static method for backward compatibility.
     * This method does not support Micrometer metrics integration.
     *
     * @param config            The HikariConfig to configure
     * @param connectionDetails The connection details containing properties
     */
    public static void configureHikariPoolStatic(HikariConfig config, ConnectionDetails connectionDetails) {
        Properties clientProperties = extractClientProperties(connectionDetails);

        // Configure basic connection pool settings first
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Configure HikariCP pool settings using client properties or defaults
        config.setMaximumPoolSize(getIntProperty(clientProperties, "ojp.connection.pool.maximumPoolSize", CommonConstants.DEFAULT_MAXIMUM_POOL_SIZE));
        config.setMinimumIdle(getIntProperty(clientProperties, "ojp.connection.pool.minimumIdle", CommonConstants.DEFAULT_MINIMUM_IDLE));
        config.setIdleTimeout(getLongProperty(clientProperties, "ojp.connection.pool.idleTimeout", CommonConstants.DEFAULT_IDLE_TIMEOUT));
        config.setMaxLifetime(getLongProperty(clientProperties, "ojp.connection.pool.maxLifetime", CommonConstants.DEFAULT_MAX_LIFETIME));
        config.setConnectionTimeout(getLongProperty(clientProperties, "ojp.connection.pool.connectionTimeout", CommonConstants.DEFAULT_CONNECTION_TIMEOUT));
        
        // Additional settings for high concurrency scenarios
        config.setLeakDetectionThreshold(60000); // 60 seconds - detect connection leaks
        config.setValidationTimeout(5000);       // 5 seconds - faster validation timeout
        config.setInitializationFailTimeout(10000); // 10 seconds - fail fast on initialization issues
        
        // Set pool name for better monitoring - generate meaningful name based on connection info
        String poolName = generatePoolName(config.getJdbcUrl(), config.getUsername());
        config.setPoolName(poolName);
        
        // Enable JMX for monitoring if not explicitly disabled
        config.setRegisterMbeans(true);

        log.info("HikariCP configured (static) with maximumPoolSize={}, minimumIdle={}, connectionTimeout={}ms, poolName={}",
                config.getMaximumPoolSize(), config.getMinimumIdle(), config.getConnectionTimeout(), poolName);
    }

    /**
     * Extracts client properties from connection details.
     *
     * @param connectionDetails The connection details
     * @return Properties object or null if not available
     */
    private static Properties extractClientProperties(ConnectionDetails connectionDetails) {
        if (connectionDetails.getProperties().isEmpty()) {
            return null;
        }

        try {
            Properties clientProperties = deserialize(connectionDetails.getProperties().toByteArray(), Properties.class);
            log.info("Received {} properties from client for connection pool configuration", clientProperties.size());
            return clientProperties;
        } catch (Exception e) {
            log.warn("Failed to deserialize client properties, using defaults: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generates a meaningful pool name based on database connection information.
     * Format: OJP-{database_type}-{database_name}-{username}
     * 
     * @param jdbcUrl The JDBC URL
     * @param username The database username
     * @return A meaningful pool name
     */
    private static String generatePoolName(String jdbcUrl, String username) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            return "OJP-Pool-Unknown-" + System.currentTimeMillis();
        }
        
        try {
            // Extract database type from JDBC URL (e.g., mysql, postgresql, h2, etc.)
            String dbType = "unknown";
            if (jdbcUrl.startsWith("jdbc:")) {
                String[] parts = jdbcUrl.split(":");
                if (parts.length > 1) {
                    dbType = parts[1];
                }
            }
            
            // Extract database name from JDBC URL
            String dbName = "unknown";
            if (jdbcUrl.contains("/")) {
                String urlPart = jdbcUrl.substring(jdbcUrl.lastIndexOf("/") + 1);
                // Remove query parameters if present
                if (urlPart.contains("?")) {
                    urlPart = urlPart.substring(0, urlPart.indexOf("?"));
                }
                // Remove semicolon parameters if present (for SQL Server, etc.)
                if (urlPart.contains(";")) {
                    urlPart = urlPart.substring(0, urlPart.indexOf(";"));
                }
                if (!urlPart.trim().isEmpty()) {
                    dbName = urlPart.trim();
                }
            }
            
            // Clean username (handle null/empty)
            String cleanUsername = (username != null && !username.trim().isEmpty()) ? username.trim() : "anonymous";
            
            // Generate pool name with meaningful components
            String poolName = String.format("OJP-%s-%s-%s", dbType, dbName, cleanUsername);
            
            // Replace special characters that might cause issues in monitoring systems
            poolName = poolName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
            
            // Limit length to avoid overly long names
            if (poolName.length() > 50) {
                poolName = poolName.substring(0, 47) + "...";
            }
            
            return poolName;
            
        } catch (Exception e) {
            log.warn("Failed to generate meaningful pool name from URL: {}, using fallback", jdbcUrl, e);
            return "OJP-Pool-Fallback-" + System.currentTimeMillis();
        }
    }
    
    /**
     * Gets an integer property with a default value.
     *
     * @param properties   The properties object
     * @param key         The property key
     * @param defaultValue The default value
     * @return The property value or default
     */
    private static int getIntProperty(Properties properties, String key, int defaultValue) {
        if (properties == null || !properties.containsKey(key)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for property '{}': {}, using default: {}", key, properties.getProperty(key), defaultValue);
            return defaultValue;
        }
    }

    /**
     * Gets a long property with a default value.
     *
     * @param properties   The properties object
     * @param key         The property key
     * @param defaultValue The default value
     * @return The property value or default
     */
    private static long getLongProperty(Properties properties, String key, long defaultValue) {
        if (properties == null || !properties.containsKey(key)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(properties.getProperty(key));
        } catch (NumberFormatException e) {
            log.warn("Invalid long value for property '{}': {}, using default: {}", key, properties.getProperty(key), defaultValue);
            return defaultValue;
        }
    }
}