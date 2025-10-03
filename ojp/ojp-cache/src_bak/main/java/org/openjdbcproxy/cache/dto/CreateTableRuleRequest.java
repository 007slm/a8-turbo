package org.openjdbcproxy.cache.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建表格缓存规则请求
 *
 * 字段说明:
 * name: 规则名称
 * description: 规则描述
 * connHash: 连接哈希值（JDBC URL），用于区分不同数据库连接
 * ttl: TTL（秒）
 * priority: 优先级
 * enabled: 是否启用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTableRuleRequest {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "连接哈希值不能为空")
    private String connHash;

    @NotNull(message = "TTL不能为空")
    @Min(value = 10, message = "TTL不能小于10秒")
    @Max(value = 86400, message = "TTL不能大于86400秒（24小时）")
    private Integer ttl;

    @Min(value = 1, message = "优先级不能小于1")
    @Max(value = 100, message = "优先级不能大于100")
    private Integer priority = 10;

    private Boolean enabled = true;
}