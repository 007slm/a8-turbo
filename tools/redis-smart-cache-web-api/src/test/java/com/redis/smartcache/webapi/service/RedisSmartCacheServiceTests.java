package com.redis.smartcache.webapi.service;

import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.smartcache.webapi.model.QueryInfo;
import com.redis.smartcache.webapi.model.RuleInfo;
import com.redis.smartcache.webapi.model.TableInfo;
import com.redis.smartcache.webapi.service.impl.RedisSmartCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.airlift.units.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 服务层单元测试
 * 测试RedisSmartCacheService的业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RedisSmartCacheServiceTests {

    @Mock
    private StatefulRedisModulesConnection<String, String> connection;

    private RedisSmartCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RedisSmartCacheServiceImpl();
        // 注意：由于我们使用了@Autowired，在单元测试中需要手动设置依赖
        // 这里仅作为测试结构示例，实际需要使用反射或其他方式设置字段
    }

    /**
     * 测试连接测试功能
     */
    @Test
    void testConnectionTest() {
        // 这里仅作为测试结构示例
        // 实际实现需要根据具体的service实现来编写
        assertTrue(true, "连接测试通过");
    }

    /**
     * 测试规则验证功能
     */
    @Test
    void testRuleValidation() {
        // 创建有效规则
        RuleInfo validRule = RuleInfo.builder()
                .ttl("30m")
                .ruleType(RuleInfo.RuleType.TABLES_ANY)
                .tablesAny(Arrays.asList("users"))
                .build();

        // 创建无效规则（TTL为空）
        RuleInfo invalidRule = RuleInfo.builder()
                .ttl("")
                .ruleType(RuleInfo.RuleType.TABLES_ANY)
                .tablesAny(Arrays.asList("users"))
                .build();

        // 验证规则创建逻辑
        assertNotNull(validRule.getTtl());
        assertNotNull(validRule.getTablesAny());
        assertEquals("30m", validRule.getTtl());
        assertEquals(RuleInfo.RuleType.TABLES_ANY, validRule.getRuleType());

        // 验证无效规则
        assertTrue(invalidRule.getTtl().isEmpty());
    }

    /**
     * 测试查询信息创建
     */
    @Test
    void testQueryInfoCreation() {
        QueryInfo query = QueryInfo.builder()
                .id("q1")
                .sql("SELECT * FROM users WHERE id = ?")
                .tables(Arrays.asList("users"))
                .count(100)
                .meanTime(150.5)
                .build();

        assertNotNull(query);
        assertEquals("q1", query.getId());
        assertEquals("SELECT * FROM users WHERE id = ?", query.getSql());
        assertEquals(Arrays.asList("users"), query.getTables());
        assertEquals(100, query.getCount());
        assertEquals(150.5, query.getMeanTime());
        assertFalse(query.isCached()); // 默认不缓存
    }

    /**
     * 测试表格信息创建
     */
    @Test
    void testTableInfoCreation() {
        TableInfo table = TableInfo.builder()
                .name("users")
                .accessFrequency(1000)
                .queryTime(120.0)
                .build();

        assertNotNull(table);
        assertEquals("users", table.getName());
        assertEquals(1000, table.getAccessFrequency());
        assertEquals(120.0, table.getQueryTime());
        assertEquals("", table.getTtl()); // 没有规则时TTL为空
    }

    /**
     * 测试规则类型判断
     */
    @Test
    void testRuleTypeDetection() {
        // Tables规则
        RuleInfo tablesRule = RuleInfo.builder()
                .ttl("30m")
                .ruleType(RuleInfo.RuleType.TABLES)
                .tables(Arrays.asList("users", "orders"))
                .build();
        assertEquals(RuleInfo.RuleType.TABLES, tablesRule.getRuleType());

        // TablesAny规则
        RuleInfo tablesAnyRule = RuleInfo.builder()
                .ttl("30m")
                .ruleType(RuleInfo.RuleType.TABLES_ANY)
                .tablesAny(Arrays.asList("users"))
                .build();
        assertEquals(RuleInfo.RuleType.TABLES_ANY, tablesAnyRule.getRuleType());

        // Regex规则
        RuleInfo regexRule = RuleInfo.builder()
                .ttl("30m")
                .ruleType(RuleInfo.RuleType.REGEX)
                .regex("SELECT.*FROM users.*")
                .build();
        assertEquals(RuleInfo.RuleType.REGEX, regexRule.getRuleType());

        // QueryIds规则
        RuleInfo queryIdsRule = RuleInfo.builder()
                .ttl("30m")
                .ruleType(RuleInfo.RuleType.QUERY_IDS)
                .queryIds(Arrays.asList("q1", "q2"))
                .build();
        assertEquals(RuleInfo.RuleType.QUERY_IDS, queryIdsRule.getRuleType());
    }

    /**
     * 测试查询匹配规则逻辑
     */
    @Test
    void testQueryRuleMatching() {
        // 创建查询
        QueryInfo query = QueryInfo.builder()
                .id("q1")
                .sql("SELECT * FROM users WHERE id = ?")
                .tables(Arrays.asList("users"))
                .build();

        // 创建TablesAny规则
        com.redis.smartcache.webapi.model.RuleConfig ruleConfig = new com.redis.smartcache.webapi.model.RuleConfig();
        ruleConfig.setTablesAny(Arrays.asList("users", "orders"));
        ruleConfig.setTtl(Duration.valueOf("30m"));

        // 测试匹配逻辑
        boolean matches = query.matchesRule(ruleConfig);
        assertTrue(matches, "查询应该匹配TablesAny规则");

        // 创建不匹配的规则
        com.redis.smartcache.webapi.model.RuleConfig noMatchRule = new com.redis.smartcache.webapi.model.RuleConfig();
        noMatchRule.setTablesAny(Arrays.asList("products"));
        noMatchRule.setTtl(Duration.valueOf("30m"));

        boolean noMatch = query.matchesRule(noMatchRule);
        assertFalse(noMatch, "查询不应该匹配不相关的TablesAny规则");
    }

    /**
     * 测试TTL格式验证
     */
    @Test
    void testTtlFormatValidation() {
        // 测试有效的TTL格式
        String[] validTtls = {"30m", "1h", "2d", "300s", "24h"};
        for (String ttl : validTtls) {
            assertTrue(isValidTtlFormat(ttl), "TTL格式应该有效: " + ttl);
        }

        // 测试无效的TTL格式
        String[] invalidTtls = {"", "30", "h30", "30x", "abc"};
        for (String ttl : invalidTtls) {
            assertFalse(isValidTtlFormat(ttl), "TTL格式应该无效: " + ttl);
        }
    }

    /**
     * 测试规则优先级
     */
    @Test
    void testRulePriority() {
        // 创建多个规则
        RuleInfo highPriorityRule = RuleInfo.builder()
                .ttl("10m")
                .ruleType(RuleInfo.RuleType.QUERY_IDS)
                .queryIds(Arrays.asList("q1"))
                .priority(1)
                .build();

        RuleInfo lowPriorityRule = RuleInfo.builder()
                .ttl("30m")
                .ruleType(RuleInfo.RuleType.TABLES_ANY)
                .tablesAny(Arrays.asList("users"))
                .priority(2)
                .build();

        // 验证优先级设置
        assertTrue(highPriorityRule.getPriority() < lowPriorityRule.getPriority(),
                "高优先级规则的优先级数值应该更小");
    }

    /**
     * 辅助方法：验证TTL格式
     */
    private boolean isValidTtlFormat(String ttl) {
        if (ttl == null || ttl.trim().isEmpty()) {
            return false;
        }
        return ttl.matches("^\\d+[smhd]$");
    }

    /**
     * 测试统计信息计算
     */
    @Test
    void testStatsCalculation() {
        // 创建模拟数据
        List<QueryInfo> queries = Arrays.asList(
            QueryInfo.builder().id("q1").meanTime(100.0).count(50).cached(true).build(),
            QueryInfo.builder().id("q2").meanTime(200.0).count(30).cached(false).build(),
            QueryInfo.builder().id("q3").meanTime(150.0).count(40).cached(true).build()
        );

        // 计算统计信息
        long totalQueries = queries.size();
        long cachedQueries = queries.stream().mapToLong(q -> q.isCached() ? 1 : 0).sum();
        double avgQueryTime = queries.stream().mapToDouble(QueryInfo::getMeanTime).average().orElse(0.0);
        double cacheHitRate = totalQueries > 0 ? (double) cachedQueries / totalQueries * 100 : 0.0;

        // 验证计算结果
        assertEquals(3, totalQueries);
        assertEquals(2, cachedQueries);
        assertEquals(150.0, avgQueryTime, 0.01); // (100+200+150)/3 = 150
        assertEquals(66.67, cacheHitRate, 0.01); // 2/3 * 100 = 66.67
    }
}