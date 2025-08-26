package org.openjdbcproxy.grpc.server.config;

import org.openjdbcproxy.grpc.server.ServerConfiguration;
import org.openjdbcproxy.grpc.server.smartcache.service.SmartCacheRuleService;
import org.openjdbcproxy.grpc.server.smartcache.service.impl.SmartCacheRuleServiceImpl;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring 配置类
 * 配置 HTTP 服务器端口和必要的 Bean
 */
@Configuration
public class SpringConfig {
    
    @Bean
    public ServerConfiguration serverConfiguration() {
        return new ServerConfiguration();
    }
    
    @Bean
    public CacheRuleEngine cacheRuleEngine() {
        return new CacheRuleEngine();
    }
    
    @Bean
    @Primary
    public SmartCacheRuleService smartCacheRuleService(CacheRuleEngine cacheRuleEngine) {
        return new SmartCacheRuleServiceImpl(cacheRuleEngine);
    }
}
