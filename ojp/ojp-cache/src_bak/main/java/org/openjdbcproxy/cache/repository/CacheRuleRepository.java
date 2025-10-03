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
     * 根据表名获取相关的缓存规则
     * @param tableName 表名
     * @param connHash 连接哈希值（可选）
     * @return 缓存规则列表
     */
    List<CacheRule> findByTable(String tableName, String connHash);

    /**
     * 更新缓存规则
     * @param rule 缓存规则对象
     * @return 更新后的缓存规则对象
     */
    CacheRule update(CacheRule rule);

    /**
     * 根据连接哈希查找启用的规则，按优先级排序
     */
    List<CacheRule> findEnabledRulesByConnHashOrderByPriority(String connHash);

    /**
     * 删除缓存规则
     * @param ruleId 规则ID
     * @return 是否删除成功
     */
    boolean deleteById(String ruleId);

    /**
     * 获取缓存规则总数
     * @return 规则总数
     */
    long count();
}