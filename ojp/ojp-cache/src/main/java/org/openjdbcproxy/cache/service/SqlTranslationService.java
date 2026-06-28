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

    @Value("${ojp.sql-translator.url:http://ojp-sql-translator:8000}")
    private String processorUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 获取翻译后的 SQL。如果 Redis 缓存中存在则直接返回，否则实时调用 Sidecar 进行翻译并缓存到 Redis。
     *
     * @param sql      原始 SQL 语句
     * @param connHash 连接标识
     * @return 翻译后的 SQL，若翻译失败或未发生转换则返回 null
     */
    public String getOrTranslateSql(String sql, String connHash) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        // 生成 Query ID 保持与慢查询日志一致
        String queryId = org.openjdbcproxy.cache.util.JSqlParserUtil.generateSlowQueryId(connHash, sql);
        String cacheKey = "ojp:cache:sql:translated:" + queryId;

        // 1. 尝试从 Redis 缓存中获取
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                String cachedSql = cached.toString();
                if (!cachedSql.isEmpty()) {
                    log.debug("从 Redis 缓存中命中翻译后的 SQL: queryId={}", queryId);
                    return cachedSql;
                }
            }
        } catch (Exception e) {
            log.warn("从 Redis 读取翻译 SQL 失败: queryId={}", queryId, e);
        }

        // 2. 缓存未命中，进行实时翻译
        String source = "oracle"; // 默认源库为 oracle
        if (connHash != null) {
            String lowerConnHash = connHash.toLowerCase();
            if (lowerConnHash.contains("mysql") || lowerConnHash.contains("jdbc:mysql")) {
                source = "mysql";
            } else if (lowerConnHash.contains("oracle") || lowerConnHash.contains("jdbc:oracle")) {
                source = "oracle";
            }
        }

        try {
            log.info("翻译缓存未命中，开始实时调用 Sidecar 翻译 SQL: queryId={}", queryId);
            String translatedSql = callTranslationApi(sql, source, "starrocks");
            if (translatedSql != null && !translatedSql.trim().isEmpty()) {
                // 3. 写入 Redis 缓存
                redisTemplate.opsForValue().set(cacheKey, translatedSql);
                log.info("实时翻译成功并已写入 Redis 缓存: queryId={}", queryId);
                return translatedSql;
            }
        } catch (Exception e) {
            log.error("实时翻译 SQL 失败: queryId={}, sql={}", queryId, sql, e);
        }

        return null;
    }

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
                log.error("查询 {} 的转换过程失败", queryId, e);
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
        // Logic to determine source type might need to be refined based on available
        // metadata.
        // For now, assuming Oracle if not specified, or inferring from connection info
        // if available.
        // Since the requirement specifically mentions Oracle to StarRocks, we'll
        // default source to 'oracle'.
        // TODO: In the future, this should be dynamic based on the actual source DB
        // type of the connection.
        String source = "oracle";

        String sql = query.getSql();
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }

        try {
            String translatedSql = callTranslationApi(sql, source, "starrocks");

            if (translatedSql != null) {
                // 以结构化 JSON 格式存储翻译 SQL 缓存
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> cacheValue = new HashMap<>();
                cacheValue.put("queryId", queryId);
                cacheValue.put("originalSql", sql);
                cacheValue.put("translatedSql", translatedSql);
                cacheValue.put("connHash", query.getConnHash());
                cacheValue.put("updateTime", System.currentTimeMillis());

                String jsonVal = mapper.writeValueAsString(cacheValue);
                redisTemplate.opsForValue().set(cacheKey, jsonVal);
                log.info("已完成查询 {} 的 SQL 转换并存入 Redis (JSON 格式)", queryId);
            }
        } catch (Exception e) {
            log.error("转换查询 {} 的 SQL 时发生错误", queryId, e);
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
            log.error("调用翻译服务失败", e);
            throw e;
        }
        return null;
    }
}
