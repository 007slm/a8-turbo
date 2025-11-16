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
        List<CacheRule> rules = cacheRuleRepository.findAll();
        for (CacheRule rule : rules) {
            try {
                if (rule.isEnabled()) {
                    rule.setSeatunnelJobIds(
                            seatunnelJobService.synchroniseRule(rule, null)
                    );
                    cacheRuleRepository.save(rule);
                    log.info("Startup reconciled rule {}", rule.getId());
                }
            } catch (Exception ex) {
                log.warn("Startup reconcile failed for rule {}: {}", rule.getId(), ex.getMessage());
            }
        }
    }
}