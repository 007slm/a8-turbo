package org.openjdbcproxy.grpc.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlowQuerySegregationConfig {
    
    @Autowired
    private ServerConfiguration serverConfiguration;
    
    @Bean
    public QueryPerformanceMonitor queryPerformanceMonitor() {
        return new QueryPerformanceMonitor();
    }
    
    @Bean
    public SlowQuerySegregationManager slowQuerySegregationManager() {
        QueryPerformanceMonitor performanceMonitor = queryPerformanceMonitor();
        int totalSlots = 100; // 默认值，实际使用中会根据数据源连接池大小动态调整
        int slowSlotPercentage = serverConfiguration.getSlowQuerySlotPercentage();
        long idleTimeoutMs = serverConfiguration.getSlowQueryIdleTimeout();
        long slowSlotTimeoutMs = serverConfiguration.getSlowQuerySlowSlotTimeout();
        long fastSlotTimeoutMs = serverConfiguration.getSlowQueryFastSlotTimeout();
        boolean enabled = serverConfiguration.isSlowQuerySegregationEnabled();
        
        return new SlowQuerySegregationManager(
                performanceMonitor,
                totalSlots,
                slowSlotPercentage,
                idleTimeoutMs,
                slowSlotTimeoutMs,
                fastSlotTimeoutMs,
                enabled
        );
    }
}