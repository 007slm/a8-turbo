package org.openjdbcproxy.grpc.server.smartcache.service;

import org.openjdbcproxy.grpc.server.smartcache.api.model.*;

import java.util.List;
import java.util.Map;

/**
 * 智能缓存规则服务接口
 * 提供缓存规则的完整管理功能，包括CRUD操作、验证、统计等
 */
public interface SmartCacheRuleService {
    
    /**
     * 获取所有规则
     * @return 规则列表
     */
    List<CacheRuleInfo> getAllRules();
    
    /**
     * 根据ID获取规则
     * @param ruleId 规则ID
     * @return 规则信息，如果不存在返回null
     */
    CacheRuleInfo getRuleById(String ruleId);
    
    /**
     * 创建新规则
     * @param ruleInfo 规则信息
     * @return 创建后的规则（包含ID和时间戳）
     */
    CacheRuleInfo createRule(CacheRuleInfo ruleInfo);
    
    /**
     * 更新规则
     * @param ruleId 规则ID
     * @param ruleInfo 更新的规则信息
     * @return 更新后的规则
     */
    CacheRuleInfo updateRule(String ruleId, CacheRuleInfo ruleInfo);
    
    /**
     * 删除规则
     * @param ruleId 规则ID
     * @return 是否删除成功
     */
    boolean deleteRule(String ruleId);
    
    /**
     * 批量提交规则
     * @param rules 规则列表
     * @return 批量操作结果
     */
    BatchRuleResult batchSubmitRules(List<CacheRuleInfo> rules);
    
    /**
     * 验证规则
     * @param ruleInfo 规则信息
     * @return 验证结果，包含valid字段和errors字段
     */
    Map<String, Object> validateRule(CacheRuleInfo ruleInfo);
    
    /**
     * 获取规则统计信息
     * @return 规则统计
     */
    RuleStats getRuleStats();
    
    /**
     * 搜索规则
     * @param keyword 搜索关键词
     * @param ruleType 规则类型
     * @param sortBy 排序字段
     * @param sortDirection 排序方向
     * @param limit 限制数量
     * @return 匹配的规则列表
     */
    List<CacheRuleInfo> searchRules(String keyword, String ruleType, 
                                   String sortBy, String sortDirection, int limit);
    
    /**
     * 复制规则
     * @param ruleId 源规则ID
     * @param newName 新规则名称
     * @return 复制的规则
     */
    CacheRuleInfo copyRule(String ruleId, String newName);
    
    /**
     * 切换规则状态（启用/禁用）
     * @param ruleId 规则ID
     * @return 切换后的规则
     */
    CacheRuleInfo toggleRule(String ruleId);
    
    /**
     * 重新排序规则（根据优先级）
     * @return 重新排序后的规则列表
     */
    List<CacheRuleInfo> reorderRules();
    
    /**
     * 启用规则
     * @param ruleId 规则ID
     * @return 是否启用成功
     */
    boolean enableRule(String ruleId);
    
    /**
     * 禁用规则
     * @param ruleId 规则ID
     * @return 是否禁用成功
     */
    boolean disableRule(String ruleId);
    
    /**
     * 获取启用的规则列表
     * @return 启用的规则列表
     */
    List<CacheRuleInfo> getEnabledRules();
    
    /**
     * 获取禁用的规则列表
     * @return 禁用的规则列表
     */
    List<CacheRuleInfo> getDisabledRules();
    
    /**
     * 根据类型获取规则
     * @param ruleType 规则类型
     * @return 指定类型的规则列表
     */
    List<CacheRuleInfo> getRulesByType(CacheRuleInfo.RuleType ruleType);
    
    /**
     * 根据优先级获取规则
     * @param priority 优先级
     * @return 指定优先级的规则列表
     */
    List<CacheRuleInfo> getRulesByPriority(int priority);
    
    /**
     * 检查规则名称是否已存在
     * @param name 规则名称
     * @param excludeRuleId 排除的规则ID（用于更新时检查）
     * @return 是否存在
     */
    boolean isRuleNameExists(String name, String excludeRuleId);
    
    /**
     * 生成唯一的规则ID
     * @return 唯一ID
     */
    String generateRuleId();
    
    /**
     * 获取规则总数
     * @return 规则总数
     */
    int getRuleCount();
    
    /**
     * 清空所有规则
     * @return 是否清空成功
     */
    boolean clearAllRules();
    
    /**
     * 导入规则（从配置或其他来源）
     * @param rules 规则列表
     * @return 导入结果
     */
    BatchRuleResult importRules(List<CacheRuleInfo> rules);
    
    /**
     * 导出规则
     * @return 规则列表
     */
    List<CacheRuleInfo> exportRules();
    
    /**
     * 备份规则配置
     * @return 备份数据
     */
    byte[] backupRules();
    
    /**
     * 恢复规则配置
     * @param backupData 备份数据
     * @return 是否恢复成功
     */
    boolean restoreRules(byte[] backupData);
}
