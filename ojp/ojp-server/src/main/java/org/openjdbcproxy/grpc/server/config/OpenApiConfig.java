package org.openjdbcproxy.grpc.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 配置类
 * 用于配置和启用 OJP Server 的 API 文档
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置 OpenAPI 文档基本信息
     *
     * @return OpenAPI 对象
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OJP Server API")
                        .description("Open JDBC Proxy (OJP) Server API 文档。OJP 是一个开源的 JDBC 驱动和 Layer 7 数据库代理服务，旨在解决现代架构（如微服务、事件驱动、Serverless）中数据库连接管理的挑战。")
                        .version("0.0.8-alpha")
                        .contact(new Contact()
                                .name("OJP 开发团队")
                                .url("https://github.com/Open-JDBC-Proxy/ojp")
                                .email("openjdbcproxy@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8010")
                                .description("本地开发服务器"),
                        new Server()
                                .url("https://localhost:5173")
                                .description("前端代理服务器")));
    }
}