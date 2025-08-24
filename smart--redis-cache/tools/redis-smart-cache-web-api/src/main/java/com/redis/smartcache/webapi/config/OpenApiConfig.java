package com.redis.smartcache.webapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 配置类
 * 配置Swagger UI和API文档
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${smartcache.application.name:Redis Smart Cache}")
    private String applicationName;

    /**
     * 配置OpenAPI文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Redis Smart Cache Web API")
                        .version("1.0.0")
                        .description("Redis智能缓存管理系统的RESTful API接口文档。" +
                                   "提供查询管理、表格管理、规则管理、统计监控等功能的HTTP接口。")
                        .contact(new Contact()
                                .name("Redis Smart Cache Team")
                                .email("support@redis.com")
                                .url("https://github.com/redis-field-engineering/redis-smart-cache"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("本地开发环境"),
                        new Server()
                                .url("http://localhost:" + serverPort + "/api")
                                .description("API接口根路径")
                ));
    }
}