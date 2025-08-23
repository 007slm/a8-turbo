package com.redis.smartcache.webapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用启动测试
 * 验证Spring Boot应用能够正常启动和初始化
 */
@SpringBootTest
@ActiveProfiles("test")
class RedisSmartCacheWebApiApplicationTests {

    /**
     * 测试应用上下文加载
     */
    @Test
    void contextLoads() {
        // 如果应用上下文加载成功，此测试就会通过
        // 这个测试验证了所有的Bean定义和依赖注入都是正确的
    }
}