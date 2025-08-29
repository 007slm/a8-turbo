package org.openjdbcproxy.grpc.server.config;

import io.grpc.health.v1.HealthCheckResponse;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import org.openjdbcproxy.grpc.server.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring 配置类
 * 配置 gRPC 服务和必要的 Bean
 */
@Configuration
public class SpringConfig {
    
    // ServerConfiguration 现在使用 @Component 注解，Spring 会自动管理
    // 不需要手动创建 Bean
    
    // SessionManagerImpl 和 CircuitBreaker 现在使用 @Component 注解，Spring 会自动管理
    // 不需要手动创建 Bean
    
    // StatementServiceImpl 现在使用 @Component 注解，Spring 会自动管理
    // 不需要手动创建 Bean
    
    @Bean
    public OjpServerTelemetry ojpServerTelemetry() {
        return new OjpServerTelemetry();
    }
    
    @Bean
    public GrpcTelemetry grpcTelemetry(OjpServerTelemetry ojpServerTelemetry, 
                                      ServerConfiguration config) {
        if (config.isOpenTelemetryEnabled()) {
            return ojpServerTelemetry.createGrpcTelemetry(
                config.getPrometheusPort(), 
                config.getPrometheusAllowedIps()
            );
        } else {
            return ojpServerTelemetry.createNoOpGrpcTelemetry();
        }
    }
    
    // OjpHealthManager 现在由 Spring gRPC 自动配置处理
    // 不需要手动创建 Bean
}
