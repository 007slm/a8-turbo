package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.openjdbcproxy.cache.model.RuleRecommendation;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.repository.SlowQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final SlowQueryRepository slowQueryRepository;
    private final CacheRuleRepository cacheRuleRepository;

    public List<RuleRecommendation> generateRecommendations() {
        List<SlowQuery> queries = slowQueryRepository.findAll();
        List<CacheRule> existingRules = cacheRuleRepository.findAll();

        // 1. Group by Normalized SQL
        Map<String, List<SlowQuery>> grouped = queries.stream()
                .filter(q -> StringUtils.hasText(q.getNormalizedSql()))
                .collect(Collectors.groupingBy(SlowQuery::getNormalizedSql));

        List<RuleRecommendation> recommendations = new ArrayList<>();

        // 2. Analyze each group
        for (Map.Entry<String, List<SlowQuery>> entry : grouped.entrySet()) {
            String sqlTemplate = entry.getKey();
            List<SlowQuery> group = entry.getValue();

            if (group.isEmpty()) continue;

            // Metrics
            long frequency = group.size();
            double avgDuration = group.stream().mapToLong(SlowQuery::getExecutionTime).average().orElse(0);

            // Heuristics: Frequency > 5 AND Duration > 500ms
            if (frequency < 5 || avgDuration < 500) {
                continue;
            }

            // Representative query (latest)
            SlowQuery representative = group.get(group.size() - 1);

            // 3. Exclude if already matched by existing rules
            boolean covered = existingRules.stream().anyMatch(rule -> rule.matches(representative));
            if (covered) {
                continue;
            }

            // 4. Create Recommendation
            String tableNames = representative.getTableNames();
            String recommendedName = "Auto-Cache: " + (tableNames != null ? tableNames : "Complex Query");
            String reason = String.format("High Frequency (%d times), Avg Latency (%.0f ms)", frequency, avgDuration);

            recommendations.add(RuleRecommendation.builder()
                    .sqlTemplate(sqlTemplate)
                    .tableNames(tableNames)
                    .frequency(frequency)
                    .avgDuration(avgDuration)
                    .recommendedRuleName(recommendedName)
                    .reason(reason)
                    .build());
        }
        
        // Sort by impact (freq * duration) desc
        recommendations.sort((a, b) -> Double.compare(b.getFrequency() * b.getAvgDuration(), a.getFrequency() * a.getAvgDuration()));

        return recommendations;
    }
}
