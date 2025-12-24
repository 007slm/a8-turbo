package org.openjdbcproxy.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleRecommendation {
    private String sqlTemplate;
    private String tableNames;
    private long frequency;
    private double avgDuration;
    private String recommendedRuleName; 
    private String reason;
}
