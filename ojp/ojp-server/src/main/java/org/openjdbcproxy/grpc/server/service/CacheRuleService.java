package org.openjdbcproxy.grpc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.CacheStatsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存规则服务
 * 使用Redis存储缓存规则，提供CRUD操作
 */
@Slf4j
@Service
public class CacheRuleService {

    @Autowired
    private RedisService redisService;

    // Redis键前缀
    private static final String RULES_PREFIX = "ojp:rules";
    private static final String RULE_COUNTER = "ojp:rules:counter";

    /**
     * 获取所有缓存规则
     */
    public List<CacheStatsDto.CacheRuleInfo> getAllRules() {
        try {
            Set<String> ruleKeys = redisService.keys(RULES_PREFIX + ":*");
            List<CacheStatsDto.CacheRuleInfo> rules = new ArrayList<>();
            
            for (String ruleKey : ruleKeys) {
                String ruleId = ruleKey.substring(RULES_PREFIX.length() + 1);
                CacheStatsDto.CacheRuleInfo rule = getRuleById(ruleId);
                if (rule != null) {
                    rules.add(rule);
                }
            }

            // 按优先级排序
            rules.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
            return rules;
        } catch (Exception e) {
            log.error("获取所有缓存规则失败", e);
            throw new RuntimeException("获取所有缓存规则失败", e);
        }
    }

    /**
     * 根据ID获取缓存规则
     */
    public CacheStatsDto.CacheRuleInfo getRuleById(String ruleId) {
        try {
            String key = RULES_PREFIX + ":" + ruleId;
            return redisService.get(key, CacheStatsDto.CacheRuleInfo.class);
        } catch (Exception e) {
            log.error("获取缓存规则失败: {}", ruleId, e);
            return null;
        }
    }

    /**
     * 创建缓存规则
     */
    public CacheStatsDto.CacheRuleInfo createRule(CacheStatsDto.CreateCacheRuleRequest request) {
        try {
            // 生成规则ID
            String ruleId = generateRuleId();
            LocalDateTime now = LocalDateTime.now();

            CacheStatsDto.CacheRuleInfo rule = CacheStatsDto.CacheRuleInfo.builder()
                .ruleId(ruleId)
                .name(request.getName())
                .ruleType(request.getRuleType())
                .pattern(request.getPattern())
                .ttl(request.getTtl())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .description(request.getDescription())
                .matchCount(0L)
                .hitCount(0L)
                .hitRate(0.0)
                .build();

            // 保存到Redis
            String key = RULES_PREFIX + ":" + ruleId;
            redisService.set(key, rule, 365, TimeUnit.DAYS);

            log.info("创建缓存规则成功: {}", ruleId);
            return rule;
        } catch (Exception e) {
            log.error("创建缓存规则失败", e);
            throw new RuntimeException("创建缓存规则失败", e);
        }
    }

    /**
     * 更新缓存规则
     */
    public CacheStatsDto.CacheRuleInfo updateRule(String ruleId, CacheStatsDto.CreateCacheRuleRequest request) {
        try {
            CacheStatsDto.CacheRuleInfo existingRule = getRuleById(ruleId);
            if (existingRule == null) {
                throw new RuntimeException("缓存规则不存在: " + ruleId);
            }

            // 更新规则信息
            existingRule.setName(request.getName());
            existingRule.setRuleType(request.getRuleType());
            existingRule.setPattern(request.getPattern());
            existingRule.setTtl(request.getTtl());
            if (request.getPriority() != null) {
                existingRule.setPriority(request.getPriority());
            }
            existingRule.setDescription(request.getDescription());
            existingRule.setUpdatedAt(LocalDateTime.now());

            // 保存到Redis
            String key = RULES_PREFIX + ":" + ruleId;
            redisService.set(key, existingRule, 365, TimeUnit.DAYS);

            log.info("更新缓存规则成功: {}", ruleId);
            return existingRule;
        } catch (Exception e) {
            log.error("更新缓存规则失败: {}", ruleId, e);
            throw new RuntimeException("更新缓存规则失败", e);
        }
    }

    /**
     * 删除缓存规则
     */
    public boolean deleteRule(String ruleId) {
        try {
            String key = RULES_PREFIX + ":" + ruleId;
            Boolean result = redisService.delete(key);
            
            if (result != null && result) {
                log.info("删除缓存规则成功: {}", ruleId);
                return true;
            } else {
                log.warn("缓存规则不存在或删除失败: {}", ruleId);
                return false;
            }
        } catch (Exception e) {
            log.error("删除缓存规则失败: {}", ruleId, e);
            throw new RuntimeException("删除缓存规则失败", e);
        }
    }

    /**
     * 启用/禁用缓存规则
     */
    public CacheStatsDto.CacheRuleInfo toggleRule(String ruleId, boolean isActive) {
        try {
            CacheStatsDto.CacheRuleInfo rule = getRuleById(ruleId);
            if (rule == null) {
                throw new RuntimeException("缓存规则不存在: " + ruleId);
            }

            rule.setIsActive(isActive);
            rule.setUpdatedAt(LocalDateTime.now());

            // 保存到Redis
            String key = RULES_PREFIX + ":" + ruleId;
            redisService.set(key, rule, 365, TimeUnit.DAYS);

            log.info("{}缓存规则: {}", isActive ? "启用" : "禁用", ruleId);
            return rule;
        } catch (Exception e) {
            log.error("切换缓存规则状态失败: {}", ruleId, e);
            throw new RuntimeException("切换缓存规则状态失败", e);
        }
    }

    /**
     * 获取查询相关的缓存规则
     */
    public List<CacheStatsDto.CacheRuleInfo> getCacheRulesForQuery(String queryId) {
        try {
            List<CacheStatsDto.CacheRuleInfo> allRules = getAllRules();
            List<CacheStatsDto.CacheRuleInfo> matchingRules = new ArrayList<>();

            for (CacheStatsDto.CacheRuleInfo rule : allRules) {
                if (rule.getIsActive() && matchesQuery(rule, queryId)) {
                    matchingRules.add(rule);
                }
            }

            return matchingRules;
        } catch (Exception e) {
            log.error("获取查询缓存规则失败: {}", queryId, e);
            throw new RuntimeException("获取查询缓存规则失败", e);
        }
    }

    /**
     * 获取表格相关的缓存规则
     */
    public List<CacheStatsDto.CacheRuleInfo> getCacheRulesForTable(String tableName) {
        try {
            List<CacheStatsDto.CacheRuleInfo> allRules = getAllRules();
            List<CacheStatsDto.CacheRuleInfo> matchingRules = new ArrayList<>();

            for (CacheStatsDto.CacheRuleInfo rule : allRules) {
                if (rule.getIsActive() && matchesTable(rule, tableName)) {
                    matchingRules.add(rule);
                }
            }

            return matchingRules;
        } catch (Exception e) {
            log.error("获取表格缓存规则失败: {}", tableName, e);
            throw new RuntimeException("获取表格缓存规则失败", e);
        }
    }

    /**
     * 为查询创建缓存规则
     */
    public CacheStatsDto.CacheRuleInfo createCacheRuleForQuery(String queryId, CacheStatsDto.CreateCacheRuleRequest request) {
        try {
            // 设置默认值
            if (request.getName() == null) {
                request.setName("Query Rule for " + queryId);
            }
            if (request.getRuleType() == null) {
                request.setRuleType("QUERY");
            }
            if (request.getPattern() == null) {
                request.setPattern(queryId);
            }
            if (request.getTtl() == null) {
                request.setTtl("30m");
            }
            if (request.getDescription() == null) {
                request.setDescription("Auto-generated rule for query: " + queryId);
            }

            return createRule(request);
        } catch (Exception e) {
            log.error("为查询创建缓存规则失败: {}", queryId, e);
            throw new RuntimeException("为查询创建缓存规则失败", e);
        }
    }

    /**
     * 为表格创建缓存规则
     */
    public CacheStatsDto.CacheRuleInfo createCacheRuleForTable(String tableName, CacheStatsDto.CreateCacheRuleRequest request) {
        try {
            // 设置默认值
            if (request.getName() == null) {
                request.setName("Table Rule for " + tableName);
            }
            if (request.getRuleType() == null) {
                request.setRuleType("TABLES");
            }
            if (request.getPattern() == null) {
                request.setPattern(tableName);
            }
            if (request.getTtl() == null) {
                request.setTtl("1h");
            }
            if (request.getDescription() == null) {
                request.setDescription("Auto-generated rule for table: " + tableName);
            }

            return createRule(request);
        } catch (Exception e) {
            log.error("为表格创建缓存规则失败: {}", tableName, e);
            throw new RuntimeException("为表格创建缓存规则失败", e);
        }
    }

    /**
     * 记录规则匹配
     */
    public void recordRuleMatch(String ruleId, boolean isHit) {
        try {
            CacheStatsDto.CacheRuleInfo rule = getRuleById(ruleId);
            if (rule != null) {
                rule.setMatchCount(rule.getMatchCount() + 1);
                if (isHit) {
                    rule.setHitCount(rule.getHitCount() + 1);
                }
                
                // 计算命中率
                if (rule.getMatchCount() > 0) {
                    rule.setHitRate((double) rule.getHitCount() / rule.getMatchCount() * 100);
                }
                
                rule.setUpdatedAt(LocalDateTime.now());

                // 保存到Redis
                String key = RULES_PREFIX + ":" + ruleId;
                redisService.set(key, rule, 365, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("记录规则匹配失败: {}", ruleId, e);
        }
    }

    // ====================== 私有辅助方法 ======================

    private String generateRuleId() {
        Long counter = redisService.increment(RULE_COUNTER);
        return "rule_" + counter;
    }

    private boolean matchesQuery(CacheStatsDto.CacheRuleInfo rule, String queryId) {
        if ("QUERY".equals(rule.getRuleType())) {
            return queryId.contains(rule.getPattern()) || rule.getPattern().contains(queryId);
        }
        return false;
    }

    private boolean matchesTable(CacheStatsDto.CacheRuleInfo rule, String tableName) {
        if ("TABLES".equals(rule.getRuleType())) {
            return tableName.equals(rule.getPattern()) || tableName.matches(rule.getPattern());
        }
        return false;
    }
}
