package com.redis.smartcache.webapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Redis Smart Cache Web API 应用启动类
 * 
 * 这是一个基于Spring Boot 3的Web RESTful API应用，
 * 为Redis Smart Cache提供HTTP接口服务，配合前端UI实现缓存规则的可视化管理。
 * 
 * 主要功能：
 * - 查询管理：查看和管理数据库查询信息
 * - 表格管理：查看和管理数据表统计信息  
 * - 规则管理：创建、更新、删除缓存规则
 * - 统计监控：提供各种统计和监控数据
 * - 配置管理：管理应用配置和Redis连接
 */
@SpringBootApplication
@EnableConfigurationProperties
public class RedisSmartCacheWebApiApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisSmartCacheWebApiApplication.class);

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        
        ConfigurableApplicationContext context = SpringApplication.run(RedisSmartCacheWebApiApplication.class, args);
        Environment env = context.getEnvironment();
        
        logApplicationStartup(env);
    }

    /**
     * 记录应用启动信息
     */
    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        
        String serverPort = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        if (contextPath == null || contextPath.isEmpty()) {
            contextPath = "/";
        }
        
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("无法确定主机地址", e);
        }

        String applicationName = env.getProperty("spring.application.name");
        String activeProfiles = String.join(", ", env.getActiveProfiles());
        if (activeProfiles.isEmpty()) {
            activeProfiles = "default";
        }

        logger.info("""
            
            ----------------------------------------------------------
            🚀 Redis Smart Cache Web API 启动成功！
            ----------------------------------------------------------
            📋 应用信息:
                应用名称:     {}
                激活配置:     {}
                服务端口:     {}
                上下文路径:   {}
            
            🌐 访问地址:
                本地访问:     {}://localhost:{}{}
                外部访问:     {}://{}:{}{}
                API文档:      {}://localhost:{}/swagger-ui.html
                健康检查:     {}://localhost:{}/actuator/health
            
            🔧 配置信息:
                Redis主机:    {}
                Redis端口:    {}
                应用名称:     {}
            ----------------------------------------------------------
            """,
            applicationName != null ? applicationName : "redis-smart-cache-web-api",
            activeProfiles,
            serverPort,
            contextPath,
            protocol, serverPort, contextPath,
            protocol, hostAddress, serverPort, contextPath,
            protocol, serverPort,
            protocol, serverPort,
            env.getProperty("smartcache.redis.host", "localhost"),
            env.getProperty("smartcache.redis.port", "6379"),
            env.getProperty("smartcache.application.name", "smartcache")
        );
    }
}