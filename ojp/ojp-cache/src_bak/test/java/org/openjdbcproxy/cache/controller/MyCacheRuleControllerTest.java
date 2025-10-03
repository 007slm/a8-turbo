package org.openjdbcproxy.cache.controller;

import org.junit.jupiter.api.Test;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.RuleType;
import org.openjdbcproxy.cache.repository.MyCacheRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.Arrays;


@EnableRedisRepositories(basePackages = "org.openjdbcproxy.cache.repository")
@ComponentScan(basePackages = "org.openjdbcproxy.cache.controller")
public class MyCacheRuleControllerTest {

    @Autowired
    private MyCacheRuleRepository cacheRuleRepository;

    @Test
    public void testAddRule() throws Exception {
        // 创建测试用的CacheRule对象
        CacheRule rule = new CacheRule();
        rule.setId("test-rule-1");
        rule.setName("Test Rule");
        rule.setDescription("A test rule for unit testing");
        rule.setTtl(300);
        rule.setRuleType(RuleType.TABLES_ANY);
        rule.setTablesAny(Arrays.asList("users", "orders"));
        rule.setPriority(100);
        rule.setEnabled(true);
        rule.setConnHash("test-conn-hash");
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());

        cacheRuleRepository.save(rule);
    }
    

}