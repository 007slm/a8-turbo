package org.openjdbcproxy.grpc.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.ApiResponse;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.rule.CacheRule;
import org.openjdbcproxy.grpc.server.service.CacheRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 缓存规则管理控制器
 * 提供缓存规则的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/rules")
public class CacheRuleController {

    @Autowired
    private CacheRuleService cacheRuleService;

    /**
     * 获取所有缓存规则
     * GET /api/cache/rules
     */
    @GetMapping
    public ApiResponse<List<CacheRule>> getAllRules() {
        try {
            List<CacheRule> rules = cacheRuleService.getAllRules();
            return ApiResponse.success(rules, "获取缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get cache rules", e);
            return ApiResponse.error("GET_RULES_ERROR", "获取缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 根据名称获取缓存规则
     * GET /api/cache/rules/{name}
     */
    @GetMapping("/{name}")
    public ApiResponse<CacheRule> getRuleByName(@PathVariable String name) {
        try {
            return cacheRuleService.getRuleByName(name)
                    .map(rule -> ApiResponse.success(rule, "获取缓存规则成功"))
                    .orElse(ApiResponse.error("RULE_NOT_FOUND", "缓存规则不存在: " + name));
        } catch (Exception e) {
            log.error("Failed to get cache rule: {}", name, e);
            return ApiResponse.error("GET_RULE_ERROR", "获取缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 创建缓存规则
     * POST /api/cache/rules
     */
    @PostMapping
    public ApiResponse<CacheRule> createRule(@RequestBody CacheRule rule) {
        try {
            CacheRule createdRule = cacheRuleService.createRule(rule);
            return ApiResponse.success(createdRule, "创建缓存规则成功");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cache rule: {}", e.getMessage());
            return ApiResponse.error("INVALID_RULE", "无效的缓存规则: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create cache rule", e);
            return ApiResponse.error("CREATE_RULE_ERROR", "创建缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 更新缓存规则
     * PUT /api/cache/rules/{name}
     */
    @PutMapping("/{name}")
    public ApiResponse<CacheRule> updateRule(@PathVariable String name, @RequestBody CacheRule rule) {
        try {
            CacheRule updatedRule = cacheRuleService.updateRule(name, rule);
            return ApiResponse.success(updatedRule, "更新缓存规则成功");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cache rule update: {}", e.getMessage());
            return ApiResponse.error("INVALID_RULE", "无效的缓存规则: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update cache rule: {}", name, e);
            return ApiResponse.error("UPDATE_RULE_ERROR", "更新缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 删除缓存规则
     * DELETE /api/cache/rules/{name}
     */
    @DeleteMapping("/{name}")
    public ApiResponse<Boolean> deleteRule(@PathVariable String name) {
        try {
            boolean deleted = cacheRuleService.deleteRule(name);
            if (deleted) {
                return ApiResponse.success(true, "删除缓存规则成功");
            } else {
                return ApiResponse.error("RULE_NOT_FOUND", "缓存规则不存在: " + name);
            }
        } catch (Exception e) {
            log.error("Failed to delete cache rule: {}", name, e);
            return ApiResponse.error("DELETE_RULE_ERROR", "删除缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用缓存规则
     * PATCH /api/cache/rules/{name}/toggle
     */
    @PatchMapping("/{name}/toggle")
    public ApiResponse<CacheRule> toggleRule(@PathVariable String name, @RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                return ApiResponse.error("INVALID_REQUEST", "缺少enabled参数");
            }
            
            CacheRule rule = cacheRuleService.toggleRule(name, enabled);
            String message = enabled ? "启用缓存规则成功" : "禁用缓存规则成功";
            return ApiResponse.success(rule, message);
        } catch (IllegalArgumentException e) {
            log.warn("Rule not found for toggle: {}", name);
            return ApiResponse.error("RULE_NOT_FOUND", "缓存规则不存在: " + name);
        } catch (Exception e) {
            log.error("Failed to toggle cache rule: {}", name, e);
            return ApiResponse.error("TOGGLE_RULE_ERROR", "切换缓存规则状态失败: " + e.getMessage());
        }
    }

    /**
     * 创建表名规则
     * POST /api/cache/rules/table
     */
    @PostMapping("/table")
    public ApiResponse<CacheRule> createTableRule(@RequestBody CreateTableRuleRequest request) {
        try {
            Duration ttl = Duration.parse(request.getTtl());
            CacheRule rule = cacheRuleService.createTableRule(
                request.getName(), 
                request.getDescription(), 
                request.getTables(), 
                ttl
            );
            return ApiResponse.success(rule, "创建表名规则成功");
        } catch (Exception e) {
            log.error("Failed to create table rule", e);
            return ApiResponse.error("CREATE_TABLE_RULE_ERROR", "创建表名规则失败: " + e.getMessage());
        }
    }

    /**
     * 创建正则表达式规则
     * POST /api/cache/rules/regex
     */
    @PostMapping("/regex")
    public ApiResponse<CacheRule> createRegexRule(@RequestBody CreateRegexRuleRequest request) {
        try {
            Duration ttl = Duration.parse(request.getTtl());
            CacheRule rule = cacheRuleService.createRegexRule(
                request.getName(), 
                request.getDescription(), 
                request.getRegex(), 
                ttl
            );
            return ApiResponse.success(rule, "创建正则表达式规则成功");
        } catch (Exception e) {
            log.error("Failed to create regex rule", e);
            return ApiResponse.error("CREATE_REGEX_RULE_ERROR", "创建正则表达式规则失败: " + e.getMessage());
        }
    }

    /**
     * 创建查询类型规则
     * POST /api/cache/rules/query-type
     */
    @PostMapping("/query-type")
    public ApiResponse<CacheRule> createQueryTypeRule(@RequestBody CreateQueryTypeRuleRequest request) {
        try {
            Duration ttl = Duration.parse(request.getTtl());
            CacheRule rule = cacheRuleService.createQueryTypeRule(
                request.getName(), 
                request.getDescription(), 
                request.getQueryType(), 
                ttl
            );
            return ApiResponse.success(rule, "创建查询类型规则成功");
        } catch (Exception e) {
            log.error("Failed to create query type rule", e);
            return ApiResponse.error("CREATE_QUERY_TYPE_RULE_ERROR", "创建查询类型规则失败: " + e.getMessage());
        }
    }

    /**
     * 创建全局规则
     * POST /api/cache/rules/global
     */
    @PostMapping("/global")
    public ApiResponse<CacheRule> createGlobalRule(@RequestBody CreateGlobalRuleRequest request) {
        try {
            Duration ttl = Duration.parse(request.getTtl());
            CacheRule rule = cacheRuleService.createGlobalRule(
                request.getName(), 
                request.getDescription(), 
                ttl
            );
            return ApiResponse.success(rule, "创建全局规则成功");
        } catch (Exception e) {
            log.error("Failed to create global rule", e);
            return ApiResponse.error("CREATE_GLOBAL_RULE_ERROR", "创建全局规则失败: " + e.getMessage());
        }
    }

    /**
     * 获取规则版本号
     * GET /api/cache/rules/version
     */
    @GetMapping("/version")
    public ApiResponse<Long> getRulesVersion() {
        try {
            Long version = cacheRuleService.getRulesVersion();
            return ApiResponse.success(version, "获取规则版本号成功");
        } catch (Exception e) {
            log.error("Failed to get rules version", e);
            return ApiResponse.error("GET_VERSION_ERROR", "获取规则版本号失败: " + e.getMessage());
        }
    }

    /**
     * 清空所有规则
     * DELETE /api/cache/rules
     */
    @DeleteMapping
    public ApiResponse<Void> clearAllRules() {
        try {
            cacheRuleService.clearAllRules();
            return ApiResponse.success(null, "清空所有缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to clear all rules", e);
            return ApiResponse.error("CLEAR_RULES_ERROR", "清空缓存规则失败: " + e.getMessage());
        }
    }

    // 请求对象类
    public static class CreateTableRuleRequest {
        private String name;
        private String description;
        private List<String> tables;
        private String ttl;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<String> getTables() { return tables; }
        public void setTables(List<String> tables) { this.tables = tables; }
        
        public String getTtl() { return ttl; }
        public void setTtl(String ttl) { this.ttl = ttl; }
    }

    public static class CreateRegexRuleRequest {
        private String name;
        private String description;
        private String regex;
        private String ttl;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getRegex() { return regex; }
        public void setRegex(String regex) { this.regex = regex; }
        
        public String getTtl() { return ttl; }
        public void setTtl(String ttl) { this.ttl = ttl; }
    }

    public static class CreateQueryTypeRuleRequest {
        private String name;
        private String description;
        private String queryType;
        private String ttl;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        
        public String getTtl() { return ttl; }
        public void setTtl(String ttl) { this.ttl = ttl; }
    }

    public static class CreateGlobalRuleRequest {
        private String name;
        private String description;
        private String ttl;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getTtl() { return ttl; }
        public void setTtl(String ttl) { this.ttl = ttl; }
    }
}
