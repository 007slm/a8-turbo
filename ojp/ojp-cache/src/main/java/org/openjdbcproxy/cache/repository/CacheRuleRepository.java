package org.openjdbcproxy.cache.repository;

import org.openjdbcproxy.cache.entity.CacheRule;

import java.util.List;
import java.util.Optional;

/**
 * 缓存规则存储接口
 * 负责缓存规则的持久化和检索
 */
public interface CacheRuleRepository {

    /**
     * 保存缓存规则
     * @param rule 缓存规则对象
     * @return 保存后的缓存规则对象
     */
    CacheRule save(CacheRule rule);

    /**
     * 根据规则ID获取缓存规则
     * @param ruleId 规则ID
     * @return 缓存规则对象（可能为空）
     */
    Optional<CacheRule> findById(String ruleId);

    /**
     * 获取所有缓存规则
     * @return 缓存规则列表
     */
    List<CacheRule> findAll();

    /**
     * 根据数据源获取缓存规则
     * @param datasourceName 数据源名称
     * @return 缓存规则列表
     */
    List<CacheRule> findByDatasource(String datasourceName);

    /**
     * 获取启用的缓存规则（按优先级排序）
     * @param datasourceName 数据源名称（可选）
     * @return 缓存规则列表
     */
    List<CacheRule> findEnabledRulesOrderByPriority(String datasourceName);

    /**
     * 根据表名获取相关的缓存规则
     * @param tableName 表名
     * @param datasourceName 数据源名称（可选）
     * @return 缓存规则列表
     */
    List<CacheRule> findByTable(String tableName, String datasourceName);

    /**
     * 更新缓存规则
     * @param rule 缓存规则对象
     * @return 更新后的缓存规则对象
     */
    CacheRule update(CacheRule rule);

    /**
     * 删除缓存规则
     * @param ruleId 规则ID
     * @return 是否删除成功
     */
    boolean deleteById(String ruleId);

    /**
     * 删除指定数据源的所有缓存规则
     * @param datasourceName 数据源名称
     * @return 删除的记录数
     */
    int deleteByDatasource(String datasourceName);

    /**
     * 启用或禁用缓存规则
     * @param ruleId 规则ID
     * @param enabled 是否启用
     * @return 是否操作成功
     */
    boolean updateEnabled(String ruleId, boolean enabled);

    /**
     * 批量启用或禁用缓存规则
     * @param ruleIds 规则ID列表
     * @param enabled 是否启用
     * @return 更新的记录数
     */
    int batchUpdateEnabled(List<String> ruleIds, boolean enabled);

    /**
     * 更新规则优先级
     * @param ruleId 规则ID
     * @param priority 新的优先级
     * @return 是否操作成功
     */
    boolean updatePriority(String ruleId, int priority);

    /**
     * 批量更新规则优先级
     * @param rulePriorities 规则ID和优先级的映射
     * @return 更新的记录数
     */
    int batchUpdatePriorities(java.util.Map<String, Integer> rulePriorities);

    /**
     * 获取缓存规则总数
     * @return 规则总数
     */
    long count();

    /**
     * 获取指定数据源的缓存规则总数
     * @param datasourceName 数据源名称
     * @return 规则总数
     */
    long countByDatasource(String datasourceName);

    /**
     * 获取启用的缓存规则总数
     * @param datasourceName 数据源名称（可选）
     * @return 启用的规则总数
     */
    long countEnabled(String datasourceName);

    /**
     * 检查规则名称是否已存在
     * @param name 规则名称
     * @param datasourceName 数据源名称（可选）
     * @param excludeRuleId 排除的规则ID（用于更新时检查）
     * @return 是否存在
     */
    boolean existsByName(String name, String datasourceName, String excludeRuleId);

    /**
     * 批量保存缓存规则
     * @param rules 缓存规则列表
     * @return 保存的记录数
     */
    int batchSave(List<CacheRule> rules);

    /**
     * 获取默认规则（优先级最低的规则）
     * @param datasourceName 数据源名称（可选）
     * @return 默认规则（可能为空）
     */
    Optional<CacheRule> findDefaultRule(String datasourceName);

    /**
     * 重新加载缓存规则（用于缓存刷新）
     * @param datasourceName 数据源名称（可选）
     * @return 重新加载的规则列表
     */
    List<CacheRule> reloadRules(String datasourceName);
}