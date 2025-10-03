package org.openjdbcproxy.cache.entity;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();


    // 检查查询是否匹配此规则
    public boolean matches(SlowQuery query) {
        if (!enabled) {
            return false;
        }

        // 优先检查connHash，如果存在则使用connHash进行匹配
        if (!connHash.equals(query.getConnHash())) {
            return false;
        }

        if (slowQueryIds.isEmpty()) {
            return false;
        }
        String id = query.getId();
        return slowQueryIds.contains(id);
    }
}