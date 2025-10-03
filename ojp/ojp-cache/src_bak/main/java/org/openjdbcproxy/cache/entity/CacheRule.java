package org.openjdbcproxy.cache.entity;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.openjdbcproxy.cache.util.TtlUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 缓存规则实体类
 * <p>
 * 字段说明:
 * id: 规则ID
 * name: 规则名称
 * description: 规则描述
 * ttl: TTL（生存时间，秒）
 * ttlString: TTL字符串（支持格式如"30m", "1h", "3600s"）
 * tables: 涉及的表名列表（AND关系：查询必须包含所有这些表）
 * tablesAny: 涉及的表名列表（OR关系：查询包含任意一个表即可）
 * priority: 规则优先级（数字越小优先级越高）
 * enabled: 是否启用
 * connHash: 连接哈希值（JDBC URL），用于区分不同数据库连接
 * createdAt: 创建时间
 * updatedAt: 更新时间
 * ruleType: 规则类型
 * condition: 规则条件（可选，用于更复杂的匹配逻辑）
 * <p>
 * RuleType枚举说明:
 * TABLES: 表匹配规则（基于tables字段，查询必须包含所有指定表）
 * TABLES_ANY: 表任意匹配规则（基于tablesAny字段，查询包含任意一个表即可）
 * TABLES_ALL: 表全匹配规则（基于tables字段，查询必须包含所有指定表）
 * QUERY_IDS: 查询ID匹配规则（基于condition字段存储的查询ID列表）
 * REGEX: 正则表达式匹配规则（基于condition字段的正则表达式）
 * ANY: 匹配所有查询的默认规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("ojp:cache:rule")
public class CacheRule {
    @Id
    private String id;
    private String name;
    private String description;
    private int ttl;
    private RuleType ruleType;
    private List<String> tablesAll = new ArrayList<>();
    private List<String> tablesAny = new ArrayList<>();
    private List<String> queryIds = new ArrayList<>();
    private String queryReg;
    private int priority;
    private boolean enabled;
    private String connHash;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @JsonSetter
    public void setTtl(String ttl) {
        this.ttl = TtlUtils.parseTtl(ttl);
    }
    @JsonIgnore
    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    // 检查查询是否匹配此规则
    public boolean matches(Query query) {
        if (!enabled) {
            return false;
        }

        // 优先检查connHash，如果存在则使用connHash进行匹配
        if (connHash != null && !connHash.equals(query.getConnHash())) {
            return false;
        }

        // 如果connHash为空，则不匹配
        if (connHash == null || !connHash.equals(query.getConnHash())) {
            return false;
        }

        // 只处理SELECT查询
        if (!query.isSelectQuery()) {
            return false;
        }

        switch (ruleType) {
            case TABLES_ANY:
                return matchesTablesAny(query);
            case TABLES_ALL:
                return matchesTablesAll(query);
            case QUERY_IDS:
                return matchesQueryIds(query);
            case REGEX:
                return matchesRegex(query);
            default:
                return false;
        }
    }

    // 检查是否匹配tablesAny（OR关系）
    public boolean matchesTablesAny(Query query) {
        if (tablesAny == null || tablesAny.isEmpty()) {
            return false;
        }

        List<String> queryTables = query.getTables();
        if (queryTables == null || queryTables.isEmpty()) {
            return false;
        }

        // 查询包含任意一个指定的表即可
        return queryTables.stream().anyMatch(tablesAny::contains);
    }

    // 检查是否匹配tables（AND关系）
    public boolean matchesTablesAll(Query query) {
        if (tablesAll.isEmpty()) {
            return false;
        }

        List<String> queryTables = query.getTables();
        // 查询必须包含所有指定的表
        return queryTables.containsAll(tablesAll);
    }

    // 检查是否匹配queryIds（基于查询ID列表）
    public boolean matchesQueryIds(Query query) {
        // 这里需要根据具体的Query类实现来判断
        // 假设Query类有getQueryId()方法
        String queryId = query.getQueryId();
        if (queryIds.isEmpty()) {
            return false;
        }

        // condition字段存储逗号分隔的查询ID列表
        return queryIds.contains(queryId);
    }

    // 检查是否匹配regex（正则表达式匹配）
    public boolean matchesRegex(Query query) {
        if (StringUtils.isEmpty(queryReg)) {
            return false;
        }
        String sql = query.getSql();
        return sql.matches(queryReg);
    }

}