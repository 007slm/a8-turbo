package org.openjdbcproxy.cache.entity;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openjdbcproxy.cache.model.SeatunnelJobView;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Builder.Default
    private List<String> slowQueryIds = new ArrayList<>();
    private boolean enabled;
    private String connHash;
    // 添加表匹配字段，只保留tablesAny
    @Builder.Default
    private List<String> tables = new ArrayList<>();
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    @Builder.Default
    private Map<String, String> seatunnelJobIds = new HashMap<>();
    @Transient
    @Builder.Default
    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<SeatunnelJobView> seatunnelJobs = new ArrayList<>();


    // 获取匹配的Job ID列表
    public List<String> getMatchingJobIds(SlowQuery query) {
        List<String> jobIds = new ArrayList<>();
        if (tables != null && !tables.isEmpty() && seatunnelJobIds != null) {
            String tableNames = query.getTableNames();
            if (tableNames != null && !tableNames.isEmpty()) {
                String[] queryTables = tableNames.split(",");
                for (String table : tables) {
                    for (String queryTable : queryTables) {
                        if (queryTable.trim().equals(table.trim())) {
                           String jId = seatunnelJobIds.get(table);
                           if (jId != null) jobIds.add(jId);
                        }
                    }
                }
            }
        }
        return jobIds;
    }

    // 检查查询是否匹配此规则
    public boolean matches(SlowQuery query) {
        if (!enabled) {
            return false;
        }

        // 优先检查connHash，如果存在则使用connHash进行匹配
        if (!connHash.equals(query.getConnHash())) {
            return false;
        }

        // 检查表匹配 - 只要查询包含任意一个指定的表即可匹配
        if (!tables.isEmpty()) {
            String tableNames = query.getTableNames();
            if (tableNames != null && !tableNames.isEmpty()) {
                String[] queryTables = tableNames.split(",");
                
                // 检查TABLES匹配（包含任意一个表即可）
                for (String table : tables) {
                    for (String queryTable : queryTables) {
                        if (queryTable.trim().equals(table.trim())) {
                            return true; // 找到匹配的表
                        }
                    }
                }
                // 没有找到匹配的表
                return false;
            }
            // 如果规则要求表匹配但查询没有表信息，则不匹配
            return false;
        }

        // 检查slowQueryIds匹配
        if (!slowQueryIds.isEmpty()) {
            String id = query.getId();
            return slowQueryIds.contains(id);
        }
        
        // 如果既没有表匹配也没有slowQueryIds匹配，则不匹配
        return false;
    }
}
