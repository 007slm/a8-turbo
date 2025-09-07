package org.openjdbcproxy.grpc.server.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import org.openjdbcproxy.grpc.server.OjpServerTelemetry;
import org.openjdbcproxy.grpc.server.ServerConfiguration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring 配置类
 * 配置 gRPC 服务和必要的 Bean
 */
@Configuration
public class SpringConfig implements DisposableBean {

    // 保存对GrpcTelemetry的引用，以便在销毁时进行清理
    OjpServerTelemetry ojpServerTelemetry;
    
    /**
     * 配置 MeterRegistry Bean
     * 如果 Spring Boot 没有自动配置，则手动创建
     */
    @Bean
    @ConditionalOnMissingBean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public OjpServerTelemetry ojpServerTelemetry() {
        ojpServerTelemetry = new OjpServerTelemetry();
        return ojpServerTelemetry;
    }
    
    @Bean
    public GrpcTelemetry grpcTelemetry(OjpServerTelemetry ojpServerTelemetry, 
                                      ServerConfiguration config) {
        GrpcTelemetry grpcTelemetry = ojpServerTelemetry.createGrpcTelemetry(
                config.getPrometheusPort(),
                config.getPrometheusAllowedIps()
        );
        return grpcTelemetry;
    }
    
    @Override
    public void destroy() {
        this.ojpServerTelemetry.close();
    }
}