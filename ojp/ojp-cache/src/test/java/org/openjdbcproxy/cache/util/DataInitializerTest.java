package org.openjdbcproxy.cache.util;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.config.CacheConfig;
import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.repository.QueryRepository;
import org.openjdbcproxy.cache.repository.impl.RedisQueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 数据初始化测试类
 * 通过测试用例向Redis添加演示数据
 */
@Slf4j
@SpringBootTest(classes = {CacheConfig.class, DataInitializerTest.TestConfig.class})
@SpringJUnitConfig
public class DataInitializerTest {

    @Configuration
    static class TestConfig {
        
        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory("localhost", 6379);
        }
        
        @Bean
        public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, String> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new StringRedisSerializer());
            return template;
        }
        
        @Bean
        public QueryRepository queryRepository(RedisTemplate<String, String> redisTemplate) {
            return new RedisQueryRepository(redisTemplate);
        }
    }
    
    @Autowired
    private QueryRepository queryRepository;
    
    @Test
    public void initializeTestData() {
        log.info("开始初始化演示数据...");
        
        // 添加演示查询数据
        initializeQueries();
        
        log.info("演示数据初始化完成");
    }
    
    private void initializeQueries() {
        // 查询1 - primary_db数据源
        Query query1 = Query.builder()
                .queryId("query_001")
                .sql("SELECT * FROM users WHERE age > 18")
                .datasourceName("primary_db")
                .tables(Arrays.asList("users"))
                .accessCount(150L)
                .cacheHitCount(120L)
                .lastAccessTime(LocalDateTime.now().minusHours(2))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        queryRepository.save(query1);
        
        // 查询2 - primary_db数据源
        Query query2 = Query.builder()
                .queryId("query_002")
                .sql("SELECT o.*, u.name FROM orders o JOIN users u ON o.user_id = u.id")
                .datasourceName("primary_db")
                .tables(Arrays.asList("orders", "users"))
                .accessCount(89L)
                .cacheHitCount(67L)
                .lastAccessTime(LocalDateTime.now().minusMinutes(30))
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        queryRepository.save(query2);
        
        // 查询3 - primary_db数据源
        Query query3 = Query.builder()
                .queryId("query_003")
                .sql("SELECT COUNT(*) FROM products WHERE price > 100")
                .datasourceName("primary_db")
                .tables(Arrays.asList("products"))
                .accessCount(45L)
                .cacheHitCount(38L)
                .lastAccessTime(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();
        queryRepository.save(query3);
        
        // 查询4 - secondary_db数据源
        Query query4 = Query.builder()
                .queryId("query_004")
                .sql("SELECT * FROM inventory WHERE stock < 10")
                .datasourceName("secondary_db")
                .tables(Arrays.asList("inventory"))
                .accessCount(78L)
                .cacheHitCount(52L)
                .lastAccessTime(LocalDateTime.now().minusMinutes(45))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        queryRepository.save(query4);
        
        // 查询5 - secondary_db数据源
        Query query5 = Query.builder()
                .queryId("query_005")
                .sql("SELECT category, AVG(price) FROM products GROUP BY category")
                .datasourceName("secondary_db")
                .tables(Arrays.asList("products"))
                .accessCount(23L)
                .cacheHitCount(19L)
                .lastAccessTime(LocalDateTime.now().minusHours(3))
                .createdAt(LocalDateTime.now().minusDays(4))
                .build();
        queryRepository.save(query5);
        
        log.info("已添加5个演示查询数据");
    }
}