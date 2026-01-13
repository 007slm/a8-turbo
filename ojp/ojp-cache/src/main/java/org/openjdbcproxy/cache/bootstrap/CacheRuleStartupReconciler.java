package org.openjdbcproxy.cache.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.service.SeatunnelJobService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheRuleStartupReconciler implements ApplicationRunner {

    private final CacheRuleRepository cacheRuleRepository;
    private final SeatunnelJobService seatunnelJobService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<CacheRule> rules = cacheRuleRepository.findAll();
            // Perform one global reconciliation on startup
            var activeJobs = seatunnelJobService.reconcile(rules);
            
            // Update mapping for all rules
            for (CacheRule rule : rules) {
                var jobIds = seatunnelJobService.resolveJobIds(rule, activeJobs);
                rule.setSeatunnelJobIds(jobIds);
                cacheRuleRepository.save(rule);
            }
            log.info("启动时同步已完成。共处理 {} 条规则。", rules.size());
        } catch (Exception ex) {
            log.error("启动时同步失败: {}", ex.getMessage(), ex);
        }
    }
}