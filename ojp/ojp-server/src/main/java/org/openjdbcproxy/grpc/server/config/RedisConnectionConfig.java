package org.openjdbcproxy.grpc.server.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis连接配置
 * 统一使用Spring Data Redis进行Redis操作
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "ojp.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConnectionConfig {
    
    @Autowired
    private RedisConfig redisConfig;
    
    /**
     * 配置ObjectMapper，支持Java 8时间类型
     */
    @Bean
    @Primary
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
    
    /**
     * 配置Redis连接工厂
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("配置Redis连接工厂: {}:{}", redisConfig.getHost(), redisConfig.getPort());
        
        // 创建Redis独立配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisConfig.getHost());
        config.setPort(redisConfig.getPort());
        config.setDatabase(redisConfig.getDatabase());
        
        if (redisConfig.getPassword() != null && !redisConfig.getPassword().isEmpty()) {
            config.setPassword(redisConfig.getPassword());
        }
        
        // 创建Lettuce客户端配置
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
            LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisConfig.getTimeout()))
                .shutdownTimeout(Duration.ofSeconds(5));
        
        if (redisConfig.isSsl()) {
            clientConfigBuilder.useSsl();
        }
        
        LettuceClientConfiguration clientConfig = clientConfigBuilder.build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }
    
    /**
     * 配置RedisTemplate，用于通用操作
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        // 使用String序列化器作为key序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // 使用JSON序列化器作为value序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        
        log.info("Redis通用模板配置完成");
        return template;
    }
    
    /**
     * 配置RedisTemplate，专门用于字符串操作
     */
    @Bean("stringRedisTemplate")
    public RedisTemplate<String, String> stringRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        
        log.info("Redis字符串模板配置完成");
        return template;
    }
    
    /**
     * 配置RedisTemplate，专门用于Hash操作
     */
    @Bean("hashRedisTemplate")
    public RedisTemplate<String, Object> hashRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        // 对于Hash操作，value序列化器可以保持默认
        template.setValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        
        log.info("Redis Hash模板配置完成");
        return template;
    }
}
