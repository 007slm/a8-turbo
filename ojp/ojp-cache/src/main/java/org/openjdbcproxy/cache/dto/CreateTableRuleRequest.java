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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTableRuleRequest {

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 数据源名称
     */
    @NotBlank(message = "数据源名称不能为空")
    private String datasourceName;

    /**
     * TTL（秒）
     */
    @NotNull(message = "TTL不能为空")
    @Min(value = 10, message = "TTL不能小于10秒")
    @Max(value = 86400, message = "TTL不能大于86400秒（24小时）")
    private Integer ttl;

    /**
     * 优先级
     */
    @Min(value = 1, message = "优先级不能小于1")
    @Max(value = 100, message = "优先级不能大于100")
    private Integer priority = 10;

    /**
     * 是否启用
     */
    private Boolean enabled = true;
}