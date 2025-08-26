package org.openjdbcproxy.grpc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 日志响应信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponse {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 数据
     */
    private Object data;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 日志条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntry {
        /**
         * 日志ID
         */
        private String id;
        
        /**
         * 时间戳
         */
        private Long timestamp;
        
        /**
         * 日志级别
         */
        private String level;
        
        /**
         * 日志器名称
         */
        private String logger;
        
        /**
         * 消息
         */
        private String message;
        
        /**
         * 线程名
         */
        private String thread;
        
        /**
         * 异常信息
         */
        private String exception;
        
        /**
         * 堆栈跟踪
         */
        private String stackTrace;
    }
    
    /**
     * 日志统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogStatistics {
        /**
         * 总日志数
         */
        private Long totalLogs;
        
        /**
         * 错误日志数
         */
        private Long errorLogs;
        
        /**
         * 警告日志数
         */
        private Long warningLogs;
        
        /**
         * 信息日志数
         */
        private Long infoLogs;
        
        /**
         * 调试日志数
         */
        private Long debugLogs;
        
        /**
         * 按级别分组的统计
         */
        private List<LevelCount> levelCounts;
    }
    
    /**
     * 级别统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelCount {
        /**
         * 日志级别
         */
        private String level;
        
        /**
         * 数量
         */
        private Long count;
        
        /**
         * 百分比
         */
        private Double percentage;
    }
}
