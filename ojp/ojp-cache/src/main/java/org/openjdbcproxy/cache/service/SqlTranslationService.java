package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqlTranslationService {

    private final SlowQueryService slowQueryService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${ojp.sql-translator.url:http://a8-sql-translator:8000}")
    private String processorUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();

    public void processRule(CacheRule rule) {
        if (rule == null || !rule.isEnabled()) {
            return;
        }

        // Only process if we have slow queries to translate
        List<String> queryIds = rule.getSlowQueryIds();
        if (queryIds == null || queryIds.isEmpty()) {
            return;
        }

        for (String queryId : queryIds) {
            try {
                processQuery(queryId, rule);
            } catch (Exception e) {
                log.error("Failed to process translation for query {}", queryId, e);
            }
        }
    }

    private void processQuery(String queryId, CacheRule rule) {
        String cacheKey = "ojp:cache:sql:translated:" + queryId;
        
        // Check if already translated
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            return;
        }

        Optional<SlowQuery> queryOpt = slowQueryService.findById(queryId);
        if (!queryOpt.isPresent()) {
            return;
        }
        SlowQuery query = queryOpt.get();
        
        // Determine source database type. 
        // Logic to determine source type might need to be refined based on available metadata.
        // For now, assuming Oracle if not specified, or inferring from connection info if available.
        // Since the requirement specifically mentions Oracle to StarRocks, we'll default source to 'oracle'.
        // TODO: In the future, this should be dynamic based on the actual source DB type of the connection.
        String source = "oracle"; 
        
        String sql = query.getSql();
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }

        try {
            String translatedSql = callTranslationApi(sql, source, "starrocks");
            
            if (translatedSql != null) {
                // Store in Redis with expiration same as rule or persistent?
                // The requirement says "store to redis... behind use when needed".
                // We'll store it without expiry for now, or maybe long expiry.
                redisTemplate.opsForValue().set(cacheKey, translatedSql);
                log.info("Translated SQL for query {} and stored in Redis", queryId);
            }
        } catch (Exception e) {
            log.error("Error translating SQL for query {}", queryId, e);
        }
    }

    private String callTranslationApi(String sql, String source, String target) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("sql", sql);
        requestBody.put("source", source);
        requestBody.put("target", target);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map response = restTemplate.postForObject(processorUrl + "/translate", request, Map.class);
            if (response != null && response.containsKey("translated_sql")) {
                return (String) response.get("translated_sql");
            }
        } catch (Exception e) {
            log.error("Failed to call translation service", e);
            throw e;
        }
        return null;
    }
}
