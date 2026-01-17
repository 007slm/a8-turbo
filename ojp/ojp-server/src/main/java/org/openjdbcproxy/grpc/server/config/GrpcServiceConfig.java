package org.openjdbcproxy.grpc.server.config;

import io.grpc.ServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;


/**
 * gRPC 服务配置类
 * 
 * 注意：gRPC 服务会自动发现，不需要手动注册
 * - StatementServiceImpl 使用 @Service 注解，Spring gRPC 会自动发现
 * - 健康检查服务由 Spring gRPC 自动配置提供
 * - 拦截器使用 @GlobalServerInterceptor 注解自动发现
 */
@Configuration
public class GrpcServiceConfig {
    
}
