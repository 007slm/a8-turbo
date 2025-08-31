package org.openjdbcproxy.grpc.server.config;

import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import org.openjdbcproxy.grpc.server.OjpServerTelemetry;
import org.openjdbcproxy.grpc.server.ServerConfiguration;
import org.springframework.beans.factory.DisposableBean;
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