package com.redis.smartcache.webapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.smartcache.webapi.model.RuleInfo;
import com.redis.smartcache.webapi.service.RedisSmartCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 控制器集成测试
 * 测试各个Controller的HTTP接口功能
 */
@WebMvcTest
@ActiveProfiles("test")
class ControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisSmartCacheService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 设置模拟服务的默认行为
        when(redisService.testConnection()).thenReturn(true);
        when(redisService.ping()).thenReturn("PONG");
        when(redisService.getApplicationName()).thenReturn("smartcache");
    }

    /**
     * 测试Redis连接状态接口
     */
    @Test
    void testGetConnectionStatus() throws Exception {
        Map<String, Object> status = Map.of(
            "connected", true,
            "host", "localhost",
            "port", 6379,
            "applicationName", "smartcache"
        );
        
        when(redisService.getConnectionStatus()).thenReturn(status);

        mockMvc.perform(get("/api/redis/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.applicationName").value("smartcache"));
    }

    /**
     * 测试Ping接口
     */
    @Test
    void testPingRedis() throws Exception {
        mockMvc.perform(get("/api/redis/ping"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("PONG"));
    }

    /**
     * 测试获取查询列表接口
     */
    @Test
    void testGetQueries() throws Exception {
        when(redisService.getQueries(any(), any(), any(), any()))
                .thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/queries")
                .param("sortBy", "queryTime")
                .param("sortDirection", "DESC")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * 测试获取表格列表接口
     */
    @Test
    void testGetTables() throws Exception {
        when(redisService.getTables(any(), any())).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/tables"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * 测试创建规则接口
     */
    @Test
    void testCreateRule() throws Exception {
        RuleInfo ruleInfo = RuleInfo.builder()
                .ttl("30m")
                .ruleType(RuleInfo.RuleType.TABLES_ANY)
                .tablesAny(Arrays.asList("users"))
                .build();

        Map<String, Object> validation = Map.of("valid", true, "errors", Arrays.asList());
        when(redisService.validateRule(any(RuleInfo.class))).thenReturn(validation);
        when(redisService.createRule(any(RuleInfo.class))).thenReturn(ruleInfo);

        mockMvc.perform(post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ruleInfo)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ttl").value("30m"));
    }

    /**
     * 测试规则验证失败的情况
     */
    @Test
    void testCreateRuleValidationFailure() throws Exception {
        RuleInfo invalidRule = RuleInfo.builder()
                .ttl("") // 无效的TTL
                .build();

        Map<String, Object> validation = Map.of(
            "valid", false, 
            "errors", Arrays.asList("TTL不能为空")
        );
        when(redisService.validateRule(any(RuleInfo.class))).thenReturn(validation);

        mockMvc.perform(post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRule)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("规则验证失败: TTL不能为空"));
    }

    /**
     * 测试获取总体统计接口
     */
    @Test
    void testGetOverviewStats() throws Exception {
        when(redisService.getOverviewStats()).thenReturn(
            new com.redis.smartcache.webapi.model.StatsModels.OverviewStats()
        );

        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * 测试配置管理接口
     */
    @Test
    void testGetConfig() throws Exception {
        Map<String, Object> config = Map.of(
            "applicationName", "smartcache",
            "redis", Map.of("host", "localhost", "port", 6379)
        );
        
        when(redisService.getConfig()).thenReturn(config);

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationName").value("smartcache"));
    }

    /**
     * 测试CORS配置
     */
    @Test
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/api/redis/ping")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "*"));
    }

    /**
     * 测试全局异常处理
     */
    @Test
    void testGlobalExceptionHandling() throws Exception {
        when(redisService.getConnectionStatus()).thenThrow(new RuntimeException("Redis连接失败"));

        mockMvc.perform(get("/api/redis/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("获取连接状态失败: Redis连接失败"));
    }

    /**
     * 测试参数验证
     */
    @Test
    void testParameterValidation() throws Exception {
        mockMvc.perform(get("/api/queries")
                .param("sortDirection", "INVALID")) // 无效的排序方向
                .andExpect(status().isOk()); // 应该正常处理，使用默认值
    }

    /**
     * 测试JSON格式错误处理
     */
    @Test
    void testInvalidJsonHandling() throws Exception {
        mockMvc.perform(post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请求体格式错误，请检查JSON格式"));
    }
}