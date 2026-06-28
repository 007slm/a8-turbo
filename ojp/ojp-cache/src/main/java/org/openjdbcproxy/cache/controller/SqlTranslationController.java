package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * SQL 翻译缓存管理 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/translations")
@Tag(name = "SQL Translation Management", description = "SQL 翻译缓存管理 API")
@RequiredArgsConstructor
public class SqlTranslationController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/list")
    @Operation(summary = "获取已缓存的 SQL 翻译列表", description = "查询当前 Redis 缓存中所有的 SQL 翻译记录")
    public List<Map<String, Object>> getTranslationList(
            @Parameter(description = "根据 SQL 模糊搜索") @RequestParam(value = "sql", required = false) String sqlSearch,
            @Parameter(description = "根据连接标识筛选") @RequestParam(value = "connHash", required = false) String connHashFilter) {
        
        log.info("获取已缓存 of SQL 翻译列表，过滤条件：sql={}, connHash={}", sqlSearch, connHashFilter);
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            // 匹配所有翻译缓存 Key
            Set<String> keys = redisTemplate.keys("ojp:cache:sql:translated:*");
            if (keys == null || keys.isEmpty()) {
                return result;
            }

            for (String key : keys) {
                try {
                    Object cachedVal = redisTemplate.opsForValue().get(key);
                    if (cachedVal == null) {
                        continue;
                    }

                    String queryId = key.substring("ojp:cache:sql:translated:".length());
                    String rawVal = cachedVal.toString().trim();
                    Map<String, Object> item = new HashMap<>();

                    if (rawVal.startsWith("{")) {
                        // 结构化 JSON 格式
                        Map<?, ?> parsed = objectMapper.readValue(rawVal, Map.class);
                        item.put("queryId", parsed.get("queryId"));
                        item.put("originalSql", parsed.get("originalSql"));
                        item.put("translatedSql", parsed.get("translatedSql"));
                        item.put("connHash", parsed.get("connHash"));
                        item.put("updateTime", parsed.get("updateTime"));
                    } else {
                        // 兼容旧的普通字符串格式
                        item.put("queryId", queryId);
                        item.put("originalSql", ""); // 旧格式无原始 SQL
                        item.put("translatedSql", rawVal);
                        item.put("connHash", "");
                        item.put("updateTime", null);
                    }

                    // 过滤逻辑
                    boolean match = true;
                    if (sqlSearch != null && !sqlSearch.trim().isEmpty()) {
                        String searchLower = sqlSearch.toLowerCase();
                        String orig = (String) item.get("originalSql");
                        String trans = (String) item.get("translatedSql");
                        boolean origMatch = orig != null && orig.toLowerCase().contains(searchLower);
                        boolean transMatch = trans != null && trans.toLowerCase().contains(searchLower);
                        if (!origMatch && !transMatch) {
                            match = false;
                        }
                    }
                    if (connHashFilter != null && !connHashFilter.trim().isEmpty()) {
                        String conn = (String) item.get("connHash");
                        if (conn == null || !conn.toLowerCase().contains(connHashFilter.toLowerCase())) {
                            match = false;
                        }
                    }

                    if (match) {
                        result.add(item);
                    }
                } catch (Exception e) {
                    log.warn("解析 SQL 翻译缓存失败, key={}", key, e);
                }
            }

            // 按照更新时间排序（最近更新的排在前面）
            result.sort((o1, o2) -> {
                Long t1 = (Long) o1.get("updateTime");
                Long t2 = (Long) o2.get("updateTime");
                if (t1 == null && t2 == null) return 0;
                if (t1 == null) return 1;
                if (t2 == null) return -1;
                return t2.compareTo(t1);
            });

        } catch (Exception e) {
            log.error("获取已缓存的 SQL 翻译列表失败", e);
        }

        return result;
    }

    @DeleteMapping("/{queryId}")
    @Operation(summary = "删除指定的 SQL 翻译缓存", description = "根据 queryId 清理指定的 SQL 翻译记录")
    public void deleteTranslation(
            @Parameter(description = "翻译记录的 ID") @PathVariable("queryId") String queryId) {
        String key = "ojp:cache:sql:translated:" + queryId;
        Boolean deleted = redisTemplate.delete(key);
        log.info("删除 SQL 翻译缓存: key={}, 结果={}", key, deleted);
    }

    @PostMapping("/clear")
    @Operation(summary = "清空所有 SQL 翻译缓存", description = "一键清空 Redis 中所有的 SQL 翻译记录")
    public Map<String, Object> clearAllTranslations() {
        Map<String, Object> result = new HashMap<>();
        try {
            Set<String> keys = redisTemplate.keys("ojp:cache:sql:translated:*");
            if (keys != null && !keys.isEmpty()) {
                Long count = redisTemplate.delete(keys);
                log.info("一键清空 SQL 翻译缓存成功，共删除 {} 条记录", count);
                result.put("success", true);
                result.put("deletedCount", count);
                result.put("message", "清空成功，共删除 " + count + " 条翻译缓存");
            } else {
                result.put("success", true);
                result.put("deletedCount", 0);
                result.put("message", "没有可删除的翻译缓存记录");
            }
        } catch (Exception e) {
            log.error("清空所有 SQL 翻译缓存失败", e);
            result.put("success", false);
            result.put("message", "清空失败: " + e.getMessage());
        }
        return result;
    }
}
