package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.repository.SlowQueryRepository;
import org.openjdbcproxy.cache.util.JSqlParserUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 缓存决策服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheDecisionService {

    private final CacheRuleRepository cacheRuleRepository;

    private final SlowQueryRepository queryRepository;
    @SneakyThrows
    public boolean makeDecision(String connHash, String sql) {
        long startTime = System.currentTimeMillis();

        String slowQueryId = JSqlParserUtil.generateSlowQueryId(connHash, sql);
        SlowQuery query = queryRepository.findById(slowQueryId).orElse(null);
        if (query == null){
            return false;
        }
        boolean match = cacheRuleRepository.findAll().stream().anyMatch(rule -> {
            return rule.matches( query);
        });


        // 记录决策统计
        long decisionTime = System.currentTimeMillis() - startTime;

        log.info("缓存决策完成: sql={}, shouldCache={}, decisionTime={}ms",
                sql, match, decisionTime);
        return match;
    }

}