package com.redis.smartcache.webapi.config;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import io.lettuce.core.RedisURI;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 负责创建和管理Redis连接
 */
@Configuration
public class SmartCacheRedisConfig {

    @Autowired
    private RedisConfigProperties redisProperties;

    @Value("${smartcache.application.name:smartcache}")
    private String applicationName;

    /**
     * 创建Redis连接工厂
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        // 创建 Redis 独立配置
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisProperties.getHost());
        redisConfig.setPort(redisProperties.getPort());
        redisConfig.setDatabase(redisProperties.getDatabase());
        
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().trim().isEmpty()) {
            redisConfig.setPassword(redisProperties.getPassword());
        }
        
        if (redisProperties.getUsername() != null && !redisProperties.getUsername().trim().isEmpty()) {
            redisConfig.setUsername(redisProperties.getUsername());
        }

        // 创建 Lettuce 客户端配置
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = 
            LettuceClientConfiguration.builder()
                .commandTimeout(java.time.Duration.ofMillis(redisProperties.getTimeout()));

        if (redisProperties.isSsl()) {
            clientConfigBuilder.useSsl();
        }

        LettuceClientConfiguration clientConfig = clientConfigBuilder.build();
        
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }
    
    /**
     * 创建RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建Redis模块连接（用于支持Redis扩展模块）
     */
    @Bean
    public StatefulRedisModulesConnection<String, String> redisModulesConnection() {
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withDatabase(redisProperties.getDatabase())
                .withTimeout(java.time.Duration.ofMillis(redisProperties.getTimeout()));

        if (redisProperties.getPassword() != null && !redisProperties.getPassword().trim().isEmpty()) {
            uriBuilder.withPassword(redisProperties.getPassword().toCharArray());
        }
        
        if (redisProperties.getUsername() != null && !redisProperties.getUsername().trim().isEmpty()) {
            uriBuilder.withAuthentication(redisProperties.getUsername(), redisProperties.getPassword());
        }

        if (redisProperties.isSsl()) {
            uriBuilder.withSsl(true);
        }

        RedisURI redisURI = uriBuilder.build();
        RedisModulesClient client = RedisModulesClient.create(redisURI);
        
        return client.connect();
    }

    /**
     * 获取Redis连接配置信息
     */
    public RedisConnectionInfo getConnectionInfo() {
        return RedisConnectionInfo.builder()
                .host(redisProperties.getHost())
                .port(redisProperties.getPort())
                .database(redisProperties.getDatabase())
                .username(redisProperties.getUsername())
                .applicationName(applicationName)
                .ssl(redisProperties.isSsl())
                .timeout(redisProperties.getTimeout())
                .build();
    }

    /**
     * Redis连接信息数据类
     */
    public static class RedisConnectionInfo {
        private String host;
        private int port;
        private int database;
        private String username;
        private String applicationName;
        private boolean ssl;
        private long timeout;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final RedisConnectionInfo info = new RedisConnectionInfo();

            public Builder host(String host) {
                info.host = host;
                return this;
            }

            public Builder port(int port) {
                info.port = port;
                return this;
            }

            public Builder database(int database) {
                info.database = database;
                return this;
            }

            public Builder username(String username) {
                info.username = username;
                return this;
            }

            public Builder applicationName(String applicationName) {
                info.applicationName = applicationName;
                return this;
            }

            public Builder ssl(boolean ssl) {
                info.ssl = ssl;
                return this;
            }

            public Builder timeout(long timeout) {
                info.timeout = timeout;
                return this;
            }

            public RedisConnectionInfo build() {
                return info;
            }
        }

        // Getters
        public String getHost() { return host; }
        public int getPort() { return port; }
        public int getDatabase() { return database; }
        public String getUsername() { return username; }
        public String getApplicationName() { return applicationName; }
        public boolean isSsl() { return ssl; }
        public long getTimeout() { return timeout; }
    }
}