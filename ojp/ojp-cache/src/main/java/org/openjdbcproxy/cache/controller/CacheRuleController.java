package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.model.SeatunnelJobView;
import org.openjdbcproxy.cache.monitor.TableSyncStateManager;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.service.SeatunnelJobService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 缓存规则管理REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/rules")
@Tag(name = "Cache Rule Management", description = "缓存规则管理API")
@RequiredArgsConstructor
public class CacheRuleController {

    private final CacheRuleRepository cacheRuleRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SeatunnelJobService seatunnelJobService;
    private final TableSyncStateManager tableSyncStateManager;
    private final org.openjdbcproxy.cache.service.SqlTranslationService sqlTranslationService;

    @GetMapping("/list")
    @Operation(summary = "获取缓存规则列表", description = "获取所有缓存规则")
    public List<CacheRule> getRuleList() {
        log.info("获取缓存规则列表（按数据库分组）");
        List<CacheRule> rules = cacheRuleRepository.findAll();
        List<CacheRule> enriched = new ArrayList<>(rules.size());
        for (CacheRule rule : rules) {
            enriched.add(enrichRuleWithJobs(rule));
        }
        log.info("成功获取缓存规则列表，数据库数量: {}", rules.size());
        return enriched;
    }

    @PostMapping
    @Operation(summary = "创建更新缓存规则", description = "创建更新缓存规则")
    public CacheRule createRule(
            @Parameter(description = "缓存规则信息") @RequestBody CacheRule request) {
        CacheRule previousRule = null;
        if (StringUtils.isNotBlank(request.getId())) {
            previousRule = cacheRuleRepository.findById(request.getId()).orElse(null);
        }

        CacheRule persistedRule = cacheRuleRepository.save(request);
        updateRedisIndexes(persistedRule, previousRule);

        log.info("规则已保存/更新: id={}, name={}, enabled={}, connHash={}",
                persistedRule.getId(), persistedRule.getName(), persistedRule.isEnabled(), persistedRule.getConnHash());

        // Trigger SQL translation if needed
        sqlTranslationService.processRule(persistedRule);

        // Reconcile global state and update this rule's job mappings
        reconcileAll();

        return enrichRuleWithJobs(cacheRuleRepository.findById(persistedRule.getId()).orElse(persistedRule));
    }

    @PutMapping("/{ruleId}")
    @Operation(summary = "更新缓存规则", description = "更新缓存规则并同步作业")
    public CacheRule updateRule(
            @Parameter(description = "规则ID") @PathVariable("ruleId") String ruleId,
            @Parameter(description = "缓存规则信息") @RequestBody CacheRule request) {
        CacheRule previousRule = cacheRuleRepository.findById(ruleId).orElse(null);
        request.setId(ruleId);
        CacheRule persistedRule = cacheRuleRepository.save(request);
        updateRedisIndexes(persistedRule, previousRule);

        log.info("规则已更新: id={}, name={}, enabled={}, connHash={}",
                persistedRule.getId(), persistedRule.getName(), persistedRule.isEnabled(), persistedRule.getConnHash());

        // Trigger SQL translation if needed
        sqlTranslationService.processRule(persistedRule);

        // Reconcile global state
        reconcileAll();

        return enrichRuleWithJobs(cacheRuleRepository.findById(ruleId).orElse(persistedRule));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步所有缓存规则作业", description = "手动触发所有缓存规则的Seatunnel作业同步")
    public List<CacheRule> syncAllRules() {
        log.info("触发全量缓存规则同步与校准...");
        reconcileAll();
        return cacheRuleRepository.findAll().stream()
                .map(this::enrichRuleWithJobs)
                .toList();
    }

    /**
     * Reconciles the state of all Seatunnel jobs against all active rules.
     * Updates each rule's job mappings accordingly.
     */
    private void reconcileAll() {
        List<CacheRule> allRules = cacheRuleRepository.findAll();
        Map<String, String> globalActiveJobs = seatunnelJobService.reconcile(allRules);

        for (CacheRule rule : allRules) {
            Map<String, String> jobIds = seatunnelJobService.resolveJobIds(rule, globalActiveJobs);
            if (!Objects.equals(jobIds, rule.getSeatunnelJobIds())) {
                rule.setSeatunnelJobIds(jobIds);
                // Update timestamp only if jobs changed - or maybe valid to keep existing
                // update time
                // For now just save the mapping update
                cacheRuleRepository.save(rule);
            }
        }
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "删除缓存规则", description = "根据ID删除缓存规则")
    public void deleteRule(
            @Parameter(description = "规则ID") @PathVariable("ruleId") String ruleId) {
        cacheRuleRepository.findById(ruleId).ifPresent(rule -> {
            tableSyncStateManager.removeForRule(rule);
            if (rule.getTables() != null && !rule.getTables().isEmpty()) {
                for (String table : rule.getTables()) {
                    String key = "ojp:cache:rule:table:" + table;
                    redisTemplate.opsForSet().remove(key, rule.getId());
                }
            }
        });
        cacheRuleRepository.deleteById(ruleId);
        // After deletion, reconcile to remove any now-unused jobs
        reconcileAll();
    }

    private CacheRule enrichRuleWithJobs(CacheRule rule) {
        if (rule == null) {
            return null;
        }
        List<SeatunnelJobView> jobs = seatunnelJobService.describeRuleJobs(rule);
        rule.setSeatunnelJobs(jobs);
        return rule;
    }

    private void updateRedisIndexes(CacheRule latest, CacheRule previous) {
        if (previous != null && previous.getTables() != null) {
            for (String table : previous.getTables()) {
                String key = "ojp:cache:rule:table:" + table;
                redisTemplate.opsForSet().remove(key, previous.getId());
            }
        }

        if (latest.getTables() != null) {
            for (String table : latest.getTables()) {
                String key = "ojp:cache:rule:table:" + table;
                redisTemplate.opsForSet().add(key, latest.getId());
            }
        }

        // 更新本地倒排索引
        tableSyncStateManager.rebuildForRule(latest);
    }

}
