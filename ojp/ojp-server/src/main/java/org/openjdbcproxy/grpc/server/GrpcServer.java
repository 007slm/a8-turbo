package org.openjdbcproxy.grpc.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * OJP Server 主启动类
 * 使用 Spring gRPC 自动配置同时支持 HTTP 和 gRPC 服务
 * 
 * 根据 Spring gRPC 文档：
 * - spring-grpc-server-web-spring-boot-starter 会自动配置 servlet 容器运行 gRPC 服务器
 * - 同时支持 HTTP 和 gRPC 请求
 * - gRPC 服务映射到 /<service-name>/* 路径
 */
@Slf4j
@SpringBootApplication
public class GrpcServer {
    
    private static ConfigurableApplicationContext applicationContext;
    
    public static void main(String[] args) {
        try {
            log.info("启动 OJP Server...");
            
            // 启动 Spring Boot 应用
            applicationContext = SpringApplication.run(GrpcServer.class, args);
            
            log.info("OJP Server 启动成功!");
            log.info("统一服务端口: 8010 (HTTP + gRPC)");
            log.info("HTTP 服务路径: /api/*");
            log.info("gRPC 服务路径: /<service-name>/*");
            log.info("健康检查: http://localhost:8010/api/actuator/health");
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("收到关闭信号，正在关闭 OJP Server...");
                if (applicationContext != null) {
                    applicationContext.close();
                }
                log.info("OJP Server 已关闭");
            }));
            
        } catch (Exception e) {
            log.error("启动 OJP Server 失败", e);
            System.exit(1);
        }
    }
    
    /**
     * 获取应用上下文
     */
    public static ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }
    
    /**
     * 关闭服务器
     */
    public static void shutdown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
