package org.openjdbcproxy.grpc.server.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.openjdbcproxy.grpc.server.ServerConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring 配置类
 * 配置 gRPC 服务和必要的 Bean
 */
@Configuration
public class SpringConfig {

    /**
     * 配置 MeterRegistry Bean
     * 如果 Spring Boot 没有自动配置，则手动创建
     */
    @Bean
    @ConditionalOnMissingBean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
}