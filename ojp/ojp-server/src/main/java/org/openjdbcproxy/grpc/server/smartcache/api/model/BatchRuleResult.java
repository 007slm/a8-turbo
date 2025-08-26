package org.openjdbcproxy.grpc.server.smartcache.api.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 批量规则操作结果模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRuleResult {
    
    /**
     * 总规则数量
     */
    private int totalCount;
    
    /**
     * 成功处理的规则数量
     */
    private int successCount;
    
    /**
     * 失败的规则数量
     */
    private int failureCount;
    
    /**
     * 成功创建的规则列表
     */
    private List<CacheRuleInfo> createdRules;
    
    /**
     * 成功更新的规则列表
     */
    private List<CacheRuleInfo> updatedRules;
    
    /**
     * 失败的规则列表（包含错误信息）
     */
    private List<RuleError> errors;
    
    /**
     * 规则错误信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleError {
        private int index;
        private CacheRuleInfo rule;
        private String errorMessage;
        private String errorCode;
    }
    
    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return failureCount == 0;
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100;
    }
}
