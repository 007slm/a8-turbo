package org.openjdbcproxy.grpc.server.smartcache.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.smartcache.api.model.*;
import org.openjdbcproxy.grpc.server.smartcache.service.SmartCacheRuleService;

import java.util.List;
import java.util.Map;

/**
 * Smart Cache 规则管理 API 控制器
 * 提供缓存规则的完整 CRUD 操作，参考 Redis Smart Cache Web API 设计
 * 基于 gRPC 架构，不依赖 Spring 框架
 */
@RequiredArgsConstructor
@Slf4j
public class SmartCacheRuleController {
    
    private final SmartCacheRuleService ruleService;

    /**
     * 获取所有规则
     */
    public ApiResponse<List<CacheRuleInfo>> getRules() {
        try {
            List<CacheRuleInfo> rules = ruleService.getAllRules();
            return ApiResponse.success("获取规则列表成功", rules);
        } catch (Exception e) {
            log.error("获取规则列表失败", e);
            return ApiResponse.error("获取规则列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取规则
     */
    public ApiResponse<CacheRuleInfo> getRule(String ruleId) {
        try {
            CacheRuleInfo rule = ruleService.getRuleById(ruleId);
            if (rule != null) {
                return ApiResponse.success("获取规则成功", rule);
            } else {
                return ApiResponse.error("规则不存在: " + ruleId);
            }
        } catch (Exception e) {
            log.error("获取规则失败: {}", ruleId, e);
            return ApiResponse.error("获取规则失败: " + e.getMessage());
        }
    }

    /**
     * 创建新规则
     */
    public ApiResponse<CacheRuleInfo> createRule(CacheRuleInfo ruleInfo) {
        try {
            // 验证规则
            Map<String, Object> validation = ruleService.validateRule(ruleInfo);
            if (!(Boolean) validation.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) validation.get("errors");
                return ApiResponse.error("规则验证失败: " + String.join(", ", errors));
            }
            
            CacheRuleInfo createdRule = ruleService.createRule(ruleInfo);
            return ApiResponse.success("创建规则成功", createdRule);
            
        } catch (Exception e) {
            log.error("创建规则失败", e);
            return ApiResponse.error("创建规则失败: " + e.getMessage());
        }
    }

    /**
     * 更新规则
     */
    public ApiResponse<CacheRuleInfo> updateRule(String ruleId, CacheRuleInfo ruleInfo) {
        try {
            // 验证规则
            Map<String, Object> validation = ruleService.validateRule(ruleInfo);
            if (!(Boolean) validation.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) validation.get("errors");
                return ApiResponse.error("规则验证失败: " + String.join(", ", errors));
            }
            
            CacheRuleInfo updatedRule = ruleService.updateRule(ruleId, ruleInfo);
            return ApiResponse.success("更新规则成功", updatedRule);
            
        } catch (Exception e) {
            log.error("更新规则失败: {}", ruleId, e);
            return ApiResponse.error("更新规则失败: " + e.getMessage());
        }
    }

    /**
     * 删除规则
     */
    public ApiResponse<Void> deleteRule(String ruleId) {
        try {
            boolean deleted = ruleService.deleteRule(ruleId);
            if (deleted) {
                return ApiResponse.success("删除规则成功", null);
            } else {
                return ApiResponse.error("规则不存在: " + ruleId);
            }
        } catch (Exception e) {
            log.error("删除规则失败: {}", ruleId, e);
            return ApiResponse.error("删除规则失败: " + e.getMessage());
        }
    }

    /**
     * 批量提交规则
     */
    public ApiResponse<BatchRuleResult> batchSubmitRules(List<CacheRuleInfo> rules) {
        try {
            BatchRuleResult result = ruleService.batchSubmitRules(rules);
            return ApiResponse.success("批量提交规则成功", result);
            
        } catch (Exception e) {
            log.error("批量提交规则失败", e);
            return ApiResponse.error("批量提交规则失败: " + e.getMessage());
        }
    }

    /**
     * 规则验证
     */
    public ApiResponse<Map<String, Object>> validateRule(CacheRuleInfo ruleInfo) {
        try {
            Map<String, Object> validation = ruleService.validateRule(ruleInfo);
            return ApiResponse.success("规则验证完成", validation);
            
        } catch (Exception e) {
            log.error("规则验证失败", e);
            return ApiResponse.error("规则验证失败: " + e.getMessage());
        }
    }

    /**
     * 获取规则统计
     */
    public ApiResponse<RuleStats> getRuleStats() {
        try {
            RuleStats stats = ruleService.getRuleStats();
            return ApiResponse.success("获取规则统计成功", stats);
            
        } catch (Exception e) {
            log.error("获取规则统计失败", e);
            return ApiResponse.error("获取规则统计失败: " + e.getMessage());
        }
    }

    /**
     * 搜索规则
     */
    public ApiResponse<List<CacheRuleInfo>> searchRules(String keyword, String ruleType, 
                                                       String sortBy, String sortDirection, int limit) {
        try {
            List<CacheRuleInfo> rules = ruleService.searchRules(keyword, ruleType, sortBy, sortDirection, limit);
            return ApiResponse.success("搜索规则成功", rules);
            
        } catch (Exception e) {
            log.error("搜索规则失败", e);
            return ApiResponse.error("搜索规则失败: " + e.getMessage());
        }
    }

    /**
     * 复制规则
     */
    public ApiResponse<CacheRuleInfo> copyRule(String ruleId, String newName) {
        try {
            CacheRuleInfo copiedRule = ruleService.copyRule(ruleId, newName);
            return ApiResponse.success("复制规则成功", copiedRule);
            
        } catch (Exception e) {
            log.error("复制规则失败: {}", ruleId, e);
            return ApiResponse.error("复制规则失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用规则
     */
    public ApiResponse<CacheRuleInfo> toggleRule(String ruleId) {
        try {
            CacheRuleInfo toggledRule = ruleService.toggleRule(ruleId);
            return ApiResponse.success("切换规则状态成功", toggledRule);
            
        } catch (Exception e) {
            log.error("切换规则状态失败: {}", ruleId, e);
            return ApiResponse.error("切换规则状态失败: " + e.getMessage());
        }
    }

    /**
     * 重新排序规则
     */
    public ApiResponse<List<CacheRuleInfo>> reorderRules() {
        try {
            List<CacheRuleInfo> reorderedRules = ruleService.reorderRules();
            return ApiResponse.success("重新排序规则成功", reorderedRules);
            
        } catch (Exception e) {
            log.error("重新排序规则失败", e);
            return ApiResponse.error("重新排序规则失败: " + e.getMessage());
        }
    }
}
