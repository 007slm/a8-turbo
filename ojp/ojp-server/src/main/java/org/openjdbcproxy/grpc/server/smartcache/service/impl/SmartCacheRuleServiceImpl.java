package org.openjdbcproxy.grpc.server.smartcache.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.smartcache.api.model.*;
import org.openjdbcproxy.grpc.server.smartcache.service.SmartCacheRuleService;
import org.openjdbcproxy.grpc.server.smartcache.rule.CacheRuleEngine;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 智能缓存规则服务实现类
 * 基于内存存储，支持运行时动态配置
 */
@Slf4j
public class SmartCacheRuleServiceImpl implements SmartCacheRuleService {
    
    // 内存中的规则存储
    private final Map<String, CacheRuleInfo> rules = new ConcurrentHashMap<>();
    
    // 规则引擎引用
    private final CacheRuleEngine ruleEngine;
    
    // ID生成器
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    // 规则统计
    private final Map<String, Long> ruleMatchCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> ruleHitCounts = new ConcurrentHashMap<>();
    
    public SmartCacheRuleServiceImpl(CacheRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
        log.info("SmartCacheRuleService initialized with rule engine");
    }
    
    @Override
    public List<CacheRuleInfo> getAllRules() {
        List<CacheRuleInfo> allRules = new ArrayList<>(rules.values());
        allRules.sort(Comparator.comparing(CacheRuleInfo::getPriority));
        return allRules;
    }
    
    @Override
    public CacheRuleInfo getRuleById(String ruleId) {
        return rules.get(ruleId);
    }
    
    @Override
    public CacheRuleInfo createRule(CacheRuleInfo ruleInfo) {
        String ruleId = generateRuleId();
        ruleInfo.setId(ruleId);
        
        long now = System.currentTimeMillis();
        ruleInfo.setCreatedAt(now);
        ruleInfo.setUpdatedAt(now);
        
        if (ruleInfo.getRuleType() == null) {
            ruleInfo.setRuleType(CacheRuleInfo.determineRuleType(ruleInfo));
        }
        ruleInfo.setMatches(CacheRuleInfo.determineMatches(ruleInfo));
        
        rules.put(ruleId, ruleInfo);
        syncToRuleEngine();
        
        log.info("Created cache rule: {} - {}", ruleId, ruleInfo.getName());
        return ruleInfo;
    }
    
    @Override
    public CacheRuleInfo updateRule(String ruleId, CacheRuleInfo ruleInfo) {
        CacheRuleInfo existingRule = rules.get(ruleId);
        if (existingRule == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }
        
        ruleInfo.setId(ruleId);
        ruleInfo.setCreatedAt(existingRule.getCreatedAt());
        ruleInfo.setUpdatedAt(System.currentTimeMillis());
        
        if (ruleInfo.getRuleType() == null) {
            ruleInfo.setRuleType(CacheRuleInfo.determineRuleType(ruleInfo));
        }
        ruleInfo.setMatches(CacheRuleInfo.determineMatches(ruleInfo));
        
        rules.put(ruleId, ruleInfo);
        syncToRuleEngine();
        
        log.info("Updated cache rule: {} - {}", ruleId, ruleInfo.getName());
        return ruleInfo;
    }
    
    @Override
    public boolean deleteRule(String ruleId) {
        CacheRuleInfo removedRule = rules.remove(ruleId);
        if (removedRule != null) {
            syncToRuleEngine();
            ruleMatchCounts.remove(ruleId);
            ruleHitCounts.remove(ruleId);
            log.info("Deleted cache rule: {} - {}", ruleId, removedRule.getName());
            return true;
        }
        return false;
    }
    
    @Override
    public BatchRuleResult batchSubmitRules(List<CacheRuleInfo> rulesList) {
        BatchRuleResult result = BatchRuleResult.builder()
                .totalCount(rulesList.size())
                .successCount(0)
                .failureCount(0)
                .createdRules(new ArrayList<>())
                .updatedRules(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();
        
        for (int i = 0; i < rulesList.size(); i++) {
            CacheRuleInfo rule = rulesList.get(i);
            try {
                if (rule.getId() != null && rules.containsKey(rule.getId())) {
                    CacheRuleInfo updatedRule = updateRule(rule.getId(), rule);
                    result.getUpdatedRules().add(updatedRule);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } else {
                    CacheRuleInfo createdRule = createRule(rule);
                    result.getCreatedRules().add(createdRule);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                }
            } catch (Exception e) {
                BatchRuleResult.RuleError error = BatchRuleResult.RuleError.builder()
                        .index(i)
                        .rule(rule)
                        .errorMessage(e.getMessage())
                        .errorCode("RULE_ERROR")
                        .build();
                result.getErrors().add(error);
                result.setFailureCount(result.getFailureCount() + 1);
                log.error("Failed to process rule at index {}: {}", i, e.getMessage());
            }
        }
        
        log.info("Batch submit completed: {} success, {} failure", 
                result.getSuccessCount(), result.getFailureCount());
        return result;
    }
    
    @Override
    public Map<String, Object> validateRule(CacheRuleInfo ruleInfo) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        if (ruleInfo.getName() == null || ruleInfo.getName().trim().isEmpty()) {
            errors.add("Rule name is required");
        }
        
        if (ruleInfo.getTtl() == null || ruleInfo.getTtl().trim().isEmpty()) {
            errors.add("TTL is required");
        } else {
            try {
                Duration ttl = ruleInfo.getTtlDuration();
                if (ttl.isZero() || ttl.isNegative()) {
                    errors.add("TTL must be positive");
                }
            } catch (Exception e) {
                errors.add("Invalid TTL format: " + ruleInfo.getTtl());
            }
        }
        
        if (ruleInfo.getName() != null && isRuleNameExists(ruleInfo.getName(), ruleInfo.getId())) {
            errors.add("Rule name already exists: " + ruleInfo.getName());
        }
        
        boolean hasMatchCondition = (ruleInfo.getTables() != null && !ruleInfo.getTables().isEmpty()) ||
                                  (ruleInfo.getTablesAny() != null && !ruleInfo.getTablesAny().isEmpty()) ||
                                  (ruleInfo.getTablesAll() != null && !ruleInfo.getTablesAll().isEmpty()) ||
                                  (ruleInfo.getQueryIds() != null && !ruleInfo.getQueryIds().isEmpty()) ||
                                  (ruleInfo.getRegex() != null && !ruleInfo.getRegex().trim().isEmpty());
        
        if (!hasMatchCondition && ruleInfo.getRuleType() != CacheRuleInfo.RuleType.ALL) {
            errors.add("At least one match condition is required");
        }
        
        if (ruleInfo.getPriority() < 0) {
            errors.add("Priority must be non-negative");
        }
        
        validation.put("valid", errors.isEmpty());
        validation.put("errors", errors);
        
        return validation;
    }
    
    @Override
    public RuleStats getRuleStats() {
        int totalRules = rules.size();
        int enabledRules = (int) rules.values().stream().filter(CacheRuleInfo::isEnabled).count();
        int disabledRules = totalRules - enabledRules;
        
        Map<CacheRuleInfo.RuleType, Integer> rulesByType = rules.values().stream()
                .collect(Collectors.groupingBy(CacheRuleInfo::getRuleType, 
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
        
        Map<Integer, Integer> rulesByPriority = rules.values().stream()
                .collect(Collectors.groupingBy(CacheRuleInfo::getPriority, 
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
        
        List<RuleStats.TableUsage> topTables = getTopTables();
        RuleStats.RuleMatchStats matchStats = getRuleMatchStats();
        RuleStats.CachePerformanceStats performanceStats = getCachePerformanceStats();
        
        return RuleStats.builder()
                .totalRules(totalRules)
                .enabledRules(enabledRules)
                .disabledRules(disabledRules)
                .rulesByType(rulesByType)
                .rulesByPriority(rulesByPriority)
                .topTables(topTables)
                .matchStats(matchStats)
                .performanceStats(performanceStats)
                .build();
    }
    
    @Override
    public List<CacheRuleInfo> searchRules(String keyword, String ruleType, 
                                         String sortBy, String sortDirection, int limit) {
        return rules.values().stream()
                .filter(rule -> matchesSearchCriteria(rule, keyword, ruleType))
                .sorted(getSortComparator(sortBy, sortDirection))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public CacheRuleInfo copyRule(String ruleId, String newName) {
        CacheRuleInfo sourceRule = rules.get(ruleId);
        if (sourceRule == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }
        
        CacheRuleInfo copiedRule = CacheRuleInfo.builder()
                .name(newName != null ? newName : sourceRule.getName() + " (Copy)")
                .description(sourceRule.getDescription())
                .ttl(sourceRule.getTtl())
                .ruleType(sourceRule.getRuleType())
                .tables(sourceRule.getTables() != null ? new ArrayList<>(sourceRule.getTables()) : null)
                .tablesAny(sourceRule.getTablesAny() != null ? new ArrayList<>(sourceRule.getTablesAny()) : null)
                .tablesAll(sourceRule.getTablesAll() != null ? new ArrayList<>(sourceRule.getTablesAll()) : null)
                .regex(sourceRule.getRegex())
                .queryIds(sourceRule.getQueryIds() != null ? new ArrayList<>(sourceRule.getQueryIds()) : null)
                .priority(sourceRule.getPriority())
                .enabled(false)
                .build();
        
        return createRule(copiedRule);
    }
    
    @Override
    public CacheRuleInfo toggleRule(String ruleId) {
        CacheRuleInfo rule = rules.get(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }
        
        rule.setEnabled(!rule.isEnabled());
        rule.setUpdatedAt(System.currentTimeMillis());
        syncToRuleEngine();
        
        log.info("Toggled rule {} to {}", ruleId, rule.isEnabled() ? "enabled" : "disabled");
        return rule;
    }
    
    @Override
    public List<CacheRuleInfo> reorderRules() {
        List<CacheRuleInfo> allRules = new ArrayList<>(rules.values());
        allRules.sort(Comparator.comparing(CacheRuleInfo::getPriority));
        
        for (int i = 0; i < allRules.size(); i++) {
            allRules.get(i).setPriority(i);
            allRules.get(i).setUpdatedAt(System.currentTimeMillis());
        }
        
        syncToRuleEngine();
        log.info("Reordered {} rules by priority", allRules.size());
        return allRules;
    }
    
    @Override
    public boolean enableRule(String ruleId) {
        CacheRuleInfo rule = rules.get(ruleId);
        if (rule != null && !rule.isEnabled()) {
            rule.setEnabled(true);
            rule.setUpdatedAt(System.currentTimeMillis());
            syncToRuleEngine();
            log.info("Enabled rule: {}", ruleId);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean disableRule(String ruleId) {
        CacheRuleInfo rule = rules.get(ruleId);
        if (rule != null && rule.isEnabled()) {
            rule.setEnabled(false);
            rule.setUpdatedAt(System.currentTimeMillis());
            syncToRuleEngine();
            log.info("Disabled rule: {}", ruleId);
            return true;
        }
        return false;
    }
    
    @Override
    public List<CacheRuleInfo> getEnabledRules() {
        return rules.values().stream()
                .filter(CacheRuleInfo::isEnabled)
                .sorted(Comparator.comparing(CacheRuleInfo::getPriority))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CacheRuleInfo> getDisabledRules() {
        return rules.values().stream()
                .filter(rule -> !rule.isEnabled())
                .sorted(Comparator.comparing(CacheRuleInfo::getPriority))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CacheRuleInfo> getRulesByType(CacheRuleInfo.RuleType ruleType) {
        return rules.values().stream()
                .filter(rule -> rule.getRuleType() == ruleType)
                .sorted(Comparator.comparing(CacheRuleInfo::getPriority))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CacheRuleInfo> getRulesByPriority(int priority) {
        return rules.values().stream()
                .filter(rule -> rule.getPriority() == priority)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isRuleNameExists(String name, String excludeRuleId) {
        return rules.values().stream()
                .anyMatch(rule -> rule.getName().equals(name) && 
                        (excludeRuleId == null || !rule.getId().equals(excludeRuleId)));
    }
    
    @Override
    public String generateRuleId() {
        return "rule_" + idGenerator.getAndIncrement() + "_" + System.currentTimeMillis();
    }
    
    @Override
    public int getRuleCount() {
        return rules.size();
    }
    
    @Override
    public boolean clearAllRules() {
        int count = rules.size();
        rules.clear();
        ruleMatchCounts.clear();
        ruleHitCounts.clear();
        syncToRuleEngine();
        log.info("Cleared all {} rules", count);
        return true;
    }
    
    @Override
    public BatchRuleResult importRules(List<CacheRuleInfo> rulesList) {
        return batchSubmitRules(rulesList);
    }
    
    @Override
    public List<CacheRuleInfo> exportRules() {
        return getAllRules();
    }
    
    @Override
    public byte[] backupRules() {
        try {
            StringBuilder backup = new StringBuilder();
            for (CacheRuleInfo rule : rules.values()) {
                backup.append(rule.getId()).append("|")
                      .append(rule.getName()).append("|")
                      .append(rule.getTtl()).append("|")
                      .append(rule.getRuleType()).append("|")
                      .append(rule.isEnabled()).append("\n");
            }
            return backup.toString().getBytes();
        } catch (Exception e) {
            log.error("Failed to backup rules", e);
            return new byte[0];
        }
    }
    
    @Override
    public boolean restoreRules(byte[] backupData) {
        try {
            String backup = new String(backupData);
            String[] lines = backup.split("\n");
            
            clearAllRules();
            
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    CacheRuleInfo rule = CacheRuleInfo.builder()
                            .name(parts[1])
                            .ttl(parts[2])
                            .ruleType(CacheRuleInfo.RuleType.valueOf(parts[3]))
                            .enabled(Boolean.parseBoolean(parts[4]))
                            .build();
                    createRule(rule);
                }
            }
            
            log.info("Restored {} rules from backup", lines.length);
            return true;
        } catch (Exception e) {
            log.error("Failed to restore rules", e);
            return false;
        }
    }
    
    // 私有辅助方法
    
    private void syncToRuleEngine() {
        if (ruleEngine != null) {
            try {
                log.debug("Syncing {} rules to rule engine", rules.size());
            } catch (Exception e) {
                log.error("Failed to sync rules to rule engine", e);
            }
        }
    }
    
    private boolean matchesSearchCriteria(CacheRuleInfo rule, String keyword, String ruleType) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchTerm = keyword.toLowerCase();
            boolean keywordMatch = (rule.getName() != null && rule.getName().toLowerCase().contains(searchTerm)) ||
                                 (rule.getDescription() != null && rule.getDescription().toLowerCase().contains(searchTerm)) ||
                                 (rule.getMatches() != null && rule.getMatches().toLowerCase().contains(searchTerm));
            if (!keywordMatch) return false;
        }
        
        if (ruleType != null && !ruleType.trim().isEmpty()) {
            try {
                CacheRuleInfo.RuleType type = CacheRuleInfo.RuleType.valueOf(ruleType.toUpperCase());
                if (rule.getRuleType() != type) return false;
            } catch (IllegalArgumentException e) {
                // 无效的规则类型，忽略此条件
            }
        }
        
        return true;
    }
    
    private Comparator<CacheRuleInfo> getSortComparator(String sortBy, String sortDirection) {
        Comparator<CacheRuleInfo> comparator;
        
        switch (sortBy.toLowerCase()) {
            case "name":
                comparator = Comparator.comparing(rule -> rule.getName() != null ? rule.getName() : "");
                break;
            case "created":
                comparator = Comparator.comparing(CacheRuleInfo::getCreatedAt);
                break;
            case "updated":
                comparator = Comparator.comparing(CacheRuleInfo::getUpdatedAt);
                break;
            case "priority":
            default:
                comparator = Comparator.comparing(CacheRuleInfo::getPriority);
                break;
        }
        
        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }
        
        return comparator;
    }
    
    private List<RuleStats.TableUsage> getTopTables() {
        Map<String, Integer> tableUsage = new HashMap<>();
        for (CacheRuleInfo rule : rules.values()) {
            if (rule.getTables() != null) {
                for (String table : rule.getTables()) {
                    tableUsage.merge(table, 1, Integer::sum);
                }
            }
            if (rule.getTablesAny() != null) {
                for (String table : rule.getTablesAny()) {
                    tableUsage.merge(table, 1, Integer::sum);
                }
            }
            if (rule.getTablesAll() != null) {
                for (String table : rule.getTablesAll()) {
                    tableUsage.merge(table, 1, Integer::sum);
                }
            }
        }
        
        return tableUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> RuleStats.TableUsage.builder()
                        .tableName(entry.getKey())
                        .ruleCount(entry.getValue())
                        .matchCount(0)
                        .hitRate(0.0)
                        .build())
                .collect(Collectors.toList());
    }
    
    private RuleStats.RuleMatchStats getRuleMatchStats() {
        long totalMatches = ruleMatchCounts.values().stream().mapToLong(Long::longValue).sum();
        long totalHits = ruleHitCounts.values().stream().mapToLong(Long::longValue).sum();
        
        return RuleStats.RuleMatchStats.builder()
                .totalMatches(totalMatches)
                .successfulMatches(totalHits)
                .failedMatches(totalMatches - totalHits)
                .matchSuccessRate(totalMatches > 0 ? (double) totalHits / totalMatches * 100 : 0.0)
                .matchesByRule(new HashMap<>(ruleMatchCounts))
                .build();
    }
    
    private RuleStats.CachePerformanceStats getCachePerformanceStats() {
        long totalHits = ruleHitCounts.values().stream().mapToLong(Long::longValue).sum();
        long totalMatches = ruleMatchCounts.values().stream().mapToLong(Long::longValue).sum();
        long totalMisses = totalMatches - totalHits;
        
        return RuleStats.CachePerformanceStats.builder()
                .averageHitRate(totalMatches > 0 ? (double) totalHits / totalMatches * 100 : 0.0)
                .totalCacheHits(totalHits)
                .totalCacheMisses(totalMisses)
                .totalCacheRequests(totalMatches)
                .averageResponseTime(0)
                .totalCacheSize(rules.size())
                .expiredEntries(0)
                .build();
    }
    
    // 统计方法（供外部调用）
    public void recordRuleMatch(String ruleId) {
        ruleMatchCounts.merge(ruleId, 1L, Long::sum);
    }
    
    public void recordRuleHit(String ruleId) {
        ruleHitCounts.merge(ruleId, 1L, Long::sum);
    }
}
