package org.openjdbcproxy.grpc.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Redis配置类
 * 负责创建和管理Redis连接，支持智能缓存功能
 */
@Configuration
public class RedisConfig {

    @Value("${ojp.redis.host:localhost}")
    private String redisHost;

    @Value("${ojp.redis.port:6379}")
    private int redisPort;

    @Value("${ojp.redis.database:0}")
    private int redisDatabase;

    @Value("${ojp.redis.password:}")
    private String redisPassword;

    @Value("${ojp.redis.username:}")
    private String redisUsername;

    @Value("${ojp.redis.timeout:10000}")
    private long redisTimeout;

    /**
     * 创建Redis连接工厂
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);
        
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }
        
        if (redisUsername != null && !redisUsername.trim().isEmpty()) {
            redisConfig.setUsername(redisUsername);
        }

        return new LettuceConnectionFactory(redisConfig);
    }
    
    /**
     * 创建RedisTemplate<String, Object>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // 配置支持Java 8时间类型的ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 创建RedisTemplate<String, String>
     */
    @Bean("customStringRedisTemplate")
    public RedisTemplate<String, String> customStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}