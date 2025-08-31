package org.openjdbcproxy.grpc.server;

import org.openjdbcproxy.constants.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for the OJP Server that loads settings from Spring configuration.
 * Supports JVM arguments, environment variables, and application.properties/yml files.
 */
@Component
public class ServerConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ServerConfiguration.class);

    // Default values
    public static final int DEFAULT_SERVER_PORT = CommonConstants.DEFAULT_PORT_NUMBER;
    public static final int DEFAULT_PROMETHEUS_PORT = 9090;
    public static final String DEFAULT_OPENTELEMETRY_ENDPOINT = "";
    public static final int DEFAULT_THREAD_POOL_SIZE = 200;
    public static final int DEFAULT_MAX_REQUEST_SIZE = 4 * 1024 * 1024; // 4MB
    public static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final boolean DEFAULT_ACCESS_LOGGING = false;
    public static final List<String> DEFAULT_ALLOWED_IPS = List.of(IpWhitelistValidator.ALLOW_ALL_IPS); // Allow all by default
    public static final long DEFAULT_CONNECTION_IDLE_TIMEOUT = 30000; // 30 seconds
    public static final List<String> DEFAULT_PROMETHEUS_ALLOWED_IPS = List.of(IpWhitelistValidator.ALLOW_ALL_IPS); // Allow all by default
    public static final long DEFAULT_CIRCUIT_BREAKER_TIMEOUT = 60000; // 60 seconds
    public static final int DEFAULT_CIRCUIT_BREAKER_THRESHOLD = 3; // 3 failures before opening the circuit breaker.
    public static final boolean DEFAULT_SLOW_QUERY_SEGREGATION_ENABLED = true; // Enable slow query segregation by default
    public static final int DEFAULT_SLOW_QUERY_SLOT_PERCENTAGE = 20; // 20% of slots for slow queries
    public static final long DEFAULT_SLOW_QUERY_IDLE_TIMEOUT = 10000; // 10 seconds idle timeout
    public static final long DEFAULT_SLOW_QUERY_SLOW_SLOT_TIMEOUT = 120000; // 120 seconds slow slot timeout
    public static final long DEFAULT_SLOW_QUERY_FAST_SLOT_TIMEOUT = 60000; // 60 seconds fast slot timeout

    // Configuration values with Spring @Value injection
    @Value("${ojp.server.port:" + CommonConstants.DEFAULT_PORT_NUMBER + "}")
    private int serverPort;

    @Value("${ojp.prometheus.port:" + DEFAULT_PROMETHEUS_PORT + "}")
    private int prometheusPort;


    @Value("${ojp.opentelemetry.endpoint:'" + DEFAULT_OPENTELEMETRY_ENDPOINT + "'}")
    private String openTelemetryEndpoint;

    @Value("${ojp.server.threadPoolSize:" + DEFAULT_THREAD_POOL_SIZE + "}")
    private int threadPoolSize;

    @Value("${ojp.server.maxRequestSize:" + DEFAULT_MAX_REQUEST_SIZE + "}")
    private int maxRequestSize;

    @Value("${ojp.server.logLevel:'" + DEFAULT_LOG_LEVEL + "'}")
    private String logLevel;

    @Value("${ojp.server.allowedIps:}")
    private String allowedIpsString;

    @Value("${ojp.server.connectionIdleTimeout:" + DEFAULT_CONNECTION_IDLE_TIMEOUT + "}")
    private long connectionIdleTimeout;

    @Value("${ojp.prometheus.allowedIps:}")
    private String prometheusAllowedIpsString;

    @Value("${ojp.server.circuitBreakerTimeout:" + DEFAULT_CIRCUIT_BREAKER_TIMEOUT + "}")
    private long circuitBreakerTimeout;

    @Value("${ojp.server.circuitBreakerThreshold:" + DEFAULT_CIRCUIT_BREAKER_THRESHOLD + "}")
    private int circuitBreakerThreshold;

    @Value("${ojp.server.slowQuerySegregation.enabled:" + DEFAULT_SLOW_QUERY_SEGREGATION_ENABLED + "}")
    private boolean slowQuerySegregationEnabled;

    @Value("${ojp.server.slowQuerySegregation.slowSlotPercentage:" + DEFAULT_SLOW_QUERY_SLOT_PERCENTAGE + "}")
    private int slowQuerySlotPercentage;

    @Value("${ojp.server.slowQuerySegregation.idleTimeout:" + DEFAULT_SLOW_QUERY_IDLE_TIMEOUT + "}")
    private long slowQueryIdleTimeout;

    @Value("${ojp.server.slowQuerySegregation.slowSlotTimeout:" + DEFAULT_SLOW_QUERY_SLOW_SLOT_TIMEOUT + "}")
    private long slowQuerySlowSlotTimeout;

    @Value("${ojp.server.slowQuerySegregation.fastSlotTimeout:" + DEFAULT_SLOW_QUERY_FAST_SLOT_TIMEOUT + "}")
    private long slowQueryFastSlotTimeout;

    public ServerConfiguration() {
        // Spring will automatically inject the values via @Value annotations
        logConfigurationSummary();
    }

    /**
     * Converts a comma-separated string to a list of strings.
     * Returns default list if the string is empty or null.
     */
    private List<String> parseStringToList(String value, List<String> defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>(defaultValue);
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Logs a summary of the current configuration.
     */
    private void logConfigurationSummary() {
        logger.info("OJP Server Configuration:");
        logger.info("  Server Port: {}", serverPort);
        logger.info("  Prometheus Port: {}", prometheusPort);
        logger.info("  OpenTelemetry Endpoint: {}", openTelemetryEndpoint);
        logger.info("  Thread Pool Size: {}", threadPoolSize);
        logger.info("  Max Request Size: {} bytes", maxRequestSize);
        logger.info("  Log Level: {}", logLevel);
        logger.info("  Allowed IPs: {}", getAllowedIps());
        logger.info("  Connection Idle Timeout: {} ms", connectionIdleTimeout);
        logger.info("  Prometheus Allowed IPs: {}", getPrometheusAllowedIps());
        logger.info("  Circuit Breaker Timeout: {} ms", circuitBreakerTimeout);
        logger.info("  Circuit Breaker Threshold: {} ", circuitBreakerThreshold);
        logger.info("  Slow Query Segregation Enabled: {}", slowQuerySegregationEnabled);
        logger.info("  Slow Query Slot Percentage: {}%", slowQuerySlotPercentage);
        logger.info("  Slow Query Idle Timeout: {} ms", slowQueryIdleTimeout);
        logger.info("  Slow Query Slow Slot Timeout: {} ms", slowQuerySlowSlotTimeout);
        logger.info("  Slow Query Fast Slot Timeout: {} ms", slowQueryFastSlotTimeout);
    }

    // Getters
    public int getServerPort() {
        return serverPort;
    }

    public int getPrometheusPort() {
        return prometheusPort;
    }


    public String getOpenTelemetryEndpoint() {
        return openTelemetryEndpoint;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public List<String> getAllowedIps() {
        return parseStringToList(allowedIpsString, DEFAULT_ALLOWED_IPS);
    }

    public long getConnectionIdleTimeout() {
        return connectionIdleTimeout;
    }

    public List<String> getPrometheusAllowedIps() {
        return parseStringToList(prometheusAllowedIpsString, DEFAULT_PROMETHEUS_ALLOWED_IPS);
    }

    public long getCircuitBreakerTimeout() {
        return circuitBreakerTimeout;
    }

    public int getCircuitBreakerThreshold() {
        return circuitBreakerThreshold;
    }

    public boolean isSlowQuerySegregationEnabled() {
        return slowQuerySegregationEnabled;
    }

    public int getSlowQuerySlotPercentage() {
        return slowQuerySlotPercentage;
    }

    public long getSlowQueryIdleTimeout() {
        return slowQueryIdleTimeout;
    }

    public long getSlowQuerySlowSlotTimeout() {
        return slowQuerySlowSlotTimeout;
    }

    public long getSlowQueryFastSlotTimeout() {
        return slowQueryFastSlotTimeout;
    }
}