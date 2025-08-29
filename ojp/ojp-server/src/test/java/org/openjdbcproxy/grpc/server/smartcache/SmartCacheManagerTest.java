package org.openjdbcproxy.grpc.server.smartcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openjdbcproxy.grpc.server.smartcache.cache.StarRocksCacheConfig;
import org.openjdbcproxy.grpc.server.smartcache.config.CacheRuleConfigEntry;
import org.openjdbcproxy.grpc.server.smartcache.config.SmartCacheConfig;
import org.openjdbcproxy.grpc.server.smartcache.rule.QueryContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SmartCacheManager
 */
@DisplayName("Smart Cache Manager Tests")
class SmartCacheManagerTest {

    private SmartCacheManager cacheManager;
    private SmartCacheConfig testConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test configuration
        StarRocksCacheConfig starRocksConfig = StarRocksCacheConfig.builder()
                .jdbcUrl("jdbc:h2:mem:testcache")
                .username("test")
                .password("test")
                .driverClassName("org.h2.Driver")
                .build();

        List<CacheRuleConfigEntry> rules = Arrays.asList(
                CacheRuleConfigEntry.tableRule("users_cache", "users", Duration.ofMinutes(5)),
                CacheRuleConfigEntry.queryTypeRule("select_cache", QueryContext.QueryType.SELECT, Duration.ofMinutes(10)),
                CacheRuleConfigEntry.transactionAwareRule()
        );

        testConfig = SmartCacheConfig.builder()
                .enabled(true)
                .starRocksConfig(starRocksConfig)
                .cacheRules(rules)
                .metricsEnabled(true)
                .transactionAwareEnabled(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (cacheManager != null) {
            cacheManager.close();
        }
    }

    @Test
    @DisplayName("Should create cache manager with valid configuration")
    void shouldCreateCacheManagerWithValidConfig() {
        assertDoesNotThrow(() -> {
            cacheManager = new SmartCacheManager(testConfig);
            assertNotNull(cacheManager);
            assertTrue(cacheManager.isEnabled());
        });
    }

    @Test
    @DisplayName("Should create disabled cache manager when config is disabled")
    void shouldCreateDisabledCacheManager() {
        SmartCacheConfig disabledConfig = SmartCacheConfig.builder()
                .enabled(false)
                .build();
        
        cacheManager = new SmartCacheManager(disabledConfig);
        assertNotNull(cacheManager);
        assertFalse(cacheManager.isEnabled());
    }

    @Test
    @DisplayName("Should validate configuration on creation")
    void shouldValidateConfigurationOnCreation() {
        SmartCacheConfig invalidConfig = SmartCacheConfig.builder()
                .enabled(true)
                .starRocksConfig(null) // Invalid - missing StarRocks config
                .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            new SmartCacheManager(invalidConfig);
        });
    }

    @Test
    @DisplayName("Should initialize components correctly")
    void shouldInitializeComponentsCorrectly() throws Exception {
        cacheManager = new SmartCacheManager(testConfig);
        
        // Should not throw exception
        cacheManager.initialize();
        
        // Check components are available
        assertNotNull(cacheManager.getInterceptor());
        assertNotNull(cacheManager.getMetrics());
        assertNotNull(cacheManager.getRuleEngine());
        assertNotNull(cacheManager.getTransactionTracker());
        
        // Check rule engine has loaded rules
        assertEquals(3, cacheManager.getRuleEngine().getRuleCount());
    }

    @Test
    @DisplayName("Should update cache rules dynamically")
    void shouldUpdateCacheRulesDynamically() {
        cacheManager = new SmartCacheManager(testConfig);
        
        List<CacheRuleConfigEntry> newRules = Arrays.asList(
                CacheRuleConfigEntry.tableRule("products_cache", "products", Duration.ofMinutes(15))
        );
        
        cacheManager.updateCacheRules(newRules);
        
        assertEquals(1, cacheManager.getRuleEngine().getRuleCount());
    }

    @Test
    @DisplayName("Should provide health status")
    void shouldProvideHealthStatus() {
        cacheManager = new SmartCacheManager(testConfig);
        
        SmartCacheManager.CacheHealthStatus health = cacheManager.getHealthStatus();
        
        assertNotNull(health);
        assertTrue(health.isEnabled());
        assertEquals(3, health.getRuleCount());
        assertNotNull(health.getMessage());
    }

    @Test
    @DisplayName("Should handle disabled cache gracefully")
    void shouldHandleDisabledCacheGracefully() {
        SmartCacheConfig disabledConfig = SmartCacheConfig.builder()
                .enabled(false)
                .metricsEnabled(false)
                .build();
        
        cacheManager = new SmartCacheManager(disabledConfig);
        
        SmartCacheManager.CacheHealthStatus health = cacheManager.getHealthStatus();
        
        assertNotNull(health);
        assertFalse(health.isEnabled());
        assertTrue(health.isHealthy());
        assertEquals("Smart cache is disabled", health.getMessage());
    }

    @Test
    @DisplayName("Should close resources properly")
    void shouldCloseResourcesProperly() {
        cacheManager = new SmartCacheManager(testConfig);
        
        // Should not throw exception
        assertDoesNotThrow(() -> cacheManager.close());
        
        // Should be able to call close multiple times
        assertDoesNotThrow(() -> cacheManager.close());
    }
}