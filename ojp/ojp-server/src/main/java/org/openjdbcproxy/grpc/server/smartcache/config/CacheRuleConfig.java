package org.openjdbcproxy.grpc.server.smartcache.config;

import lombok.Data;
import java.time.Duration;
import java.util.List;

/**
 * 缓存规则配置类
 */
@Data
public class CacheRuleConfig {
    
    /**
     * 配置名称
     */
    private String name;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 是否启用
     */
    private boolean enabled = true;
    
    /**
     * 缓存规则列表
     */
    private List<CacheRule> rules;
    
    /**
     * 创建时间
     */
    private long createTime = System.currentTimeMillis();
    
    /**
     * 更新时间
     */
    private long updateTime = System.currentTimeMillis();
    
    /**
     * 版本号
     */
    private String version = "1.0.0";
    
    /**
     * 单个缓存规则
     */
    @Data
    public static class CacheRule {
        
        /**
         * 规则名称
         */
        private String name;
        
        /**
         * 规则描述
         */
        private String description;
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * 优先级（数字越大优先级越高）
         */
        private int priority = 0;
        
        /**
         * 表名列表（精确匹配）
         */
        private List<String> tables;
        
        /**
         * 表名列表（包含任意一个）
         */
        private List<String> tablesAny;
        
        /**
         * 表名列表（包含所有）
         */
        private List<String> tablesAll;
        
        /**
         * SQL正则表达式
         */
        private String regex;
        
        /**
         * 查询ID列表
         */
        private List<String> queryIds;
        
        /**
         * 查询类型（SELECT, INSERT, UPDATE, DELETE等）
         */
        private String queryType;
        
        /**
         * 缓存TTL
         */
        private Duration ttl = Duration.ZERO;
        
        /**
         * 控制行为（STOP, CONTINUE）
         */
        private Control control = Control.STOP;
        
        /**
         * 规则条件（JSON格式的复杂条件）
         */
        private String condition;
        
        /**
         * 控制枚举
         */
        public enum Control {
            STOP, CONTINUE
        }
    }
}
