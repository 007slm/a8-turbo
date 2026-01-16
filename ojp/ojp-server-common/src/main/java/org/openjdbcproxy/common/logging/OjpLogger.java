package org.openjdbcproxy.common.logging;

import org.slf4j.Logger;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * OJP 统一日志工具类
 * 
 * 提供规范化的日志记录方法，支持基于 SessionUUID 的链路追踪。
 * 所有日志内容使用中文，便于开发和运维人员理解。
 * 
 * 设计原则：
 * 1. 完全无侵入：基于现有 SessionUUID 机制
 * 2. 格式统一：所有日志都包含 SessionUUID 标识
 * 3. 中文友好：使用中文描述操作和状态
 * 4. 分类清晰：按操作类型分类记录日志
 * 
 * @author OJP Team
 * @since 0.0.8-alpha
 */
public class OjpLogger {
    
    /**
     * 记录会话相关日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param action 操作描述
     * @param params 附加参数
     */
    public static void logSession(Logger logger, String sessionUUID, String action, Object... params) {
        String paramStr = formatParams(params);
        if (paramStr.isEmpty()) {
            logger.info("会话[{}] {}", sessionUUID, action);
        } else {
            logger.info("会话[{}] {} {}", sessionUUID, action, paramStr);
        }
    }
    
    /**
     * 记录数据库操作日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param operation 操作类型（如：查询、更新、连接）
     * @param target 目标数据库（如：MySQL、StarRocks）
     * @param timeMs 执行时间（毫秒）
     */
    public static void logDatabase(Logger logger, String sessionUUID, String operation, String target, long timeMs) {
        logger.info("会话[{}] 数据库操作 {} 目标:{} 耗时:{}ms", sessionUUID, operation, target, timeMs);
    }
    
    /**
     * 记录数据库操作日志（不包含执行时间）
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param operation 操作类型
     * @param target 目标数据库
     */
    public static void logDatabase(Logger logger, String sessionUUID, String operation, String target) {
        logger.info("会话[{}] 数据库操作 {} 目标:{}", sessionUUID, operation, target);
    }
    
    /**
     * 记录缓存决策日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param decision 决策结果（如：命中、未命中、跳过）
     * @param table 相关表名
     * @param reason 决策原因
     */
    public static void logCacheDecision(Logger logger, String sessionUUID, String decision, String table, String reason) {
        logger.info("会话[{}] 缓存决策 {} 表:{} 原因:{}", sessionUUID, decision, table, reason);
    }
    
    /**
     * 记录缓存决策日志（多表场景）
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param decision 决策结果
     * @param tables 相关表名列表
     * @param reason 决策原因
     */
    public static void logCacheDecision(Logger logger, String sessionUUID, String decision, java.util.Set<String> tables, String reason) {
        String tableStr = tables.stream().collect(Collectors.joining(","));
        logger.info("会话[{}] 缓存决策 {} 表:{} 原因:{}", sessionUUID, decision, tableStr, reason);
    }
    
    /**
     * 记录慢查询日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param sql SQL 语句
     * @param timeMs 执行时间（毫秒）
     */
    public static void logSlowQuery(Logger logger, String sessionUUID, String sql, long timeMs) {
        logger.warn("会话[{}] 慢查询检测 SQL:{} 耗时:{}ms", sessionUUID, abbreviate(sql, 100), timeMs);
    }
    
    /**
     * 记录慢查询日志（包含阈值信息）
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param sql SQL 语句
     * @param timeMs 执行时间（毫秒）
     * @param thresholdMs 慢查询阈值（毫秒）
     */
    public static void logSlowQuery(Logger logger, String sessionUUID, String sql, long timeMs, long thresholdMs) {
        logger.warn("会话[{}] 慢查询检测 SQL:{} 耗时:{}ms 阈值:{}ms", sessionUUID, abbreviate(sql, 100), timeMs, thresholdMs);
    }
    
    /**
     * 记录异常日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param operation 操作描述
     * @param e 异常对象
     */
    public static void logError(Logger logger, String sessionUUID, String operation, Exception e) {
        logger.error("会话[{}] 操作异常 {} 错误:{}", sessionUUID, operation, e.getMessage(), e);
    }
    
    /**
     * 记录异常日志（不包含堆栈跟踪）
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param operation 操作描述
     * @param errorMessage 错误信息
     */
    public static void logError(Logger logger, String sessionUUID, String operation, String errorMessage) {
        logger.error("会话[{}] 操作异常 {} 错误:{}", sessionUUID, operation, errorMessage);
    }
    
    /**
     * 记录连接池相关日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param action 连接池操作（如：获取连接、释放连接、连接池状态）
     * @param details 详细信息
     */
    public static void logConnectionPool(Logger logger, String sessionUUID, String action, String details) {
        logger.info("会话[{}] 连接池操作 {} 详情:{}", sessionUUID, action, details);
    }
    
    /**
     * 记录事务相关日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param action 事务操作（如：开始事务、提交事务、回滚事务）
     * @param status 事务状态
     */
    public static void logTransaction(Logger logger, String sessionUUID, String action, String status) {
        logger.info("会话[{}] 事务操作 {} 状态:{}", sessionUUID, action, status);
    }
    
    /**
     * 记录 gRPC 通信日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param method gRPC 方法名
     * @param status 调用状态（如：成功、失败）
     * @param timeMs 调用耗时（毫秒）
     */
    public static void logGrpcCall(Logger logger, String sessionUUID, String method, String status, long timeMs) {
        logger.info("会话[{}] gRPC调用 方法:{} 状态:{} 耗时:{}ms", sessionUUID, method, status, timeMs);
    }
    
    /**
     * 记录 CDC 同步相关日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符（可为空，CDC 操作可能不关联特定会话）
     * @param action CDC 操作（如：创建作业、作业状态变更、同步完成）
     * @param jobId SeaTunnel 作业 ID
     * @param details 详细信息
     */
    public static void logCdcSync(Logger logger, String sessionUUID, String action, String jobId, String details) {
        if (sessionUUID != null && !sessionUUID.isEmpty()) {
            logger.info("会话[{}] CDC同步 {} 作业:{} 详情:{}", sessionUUID, action, jobId, details);
        } else {
            logger.info("CDC同步 {} 作业:{} 详情:{}", action, jobId, details);
        }
    }
    
    /**
     * 记录性能监控日志
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param metric 性能指标名称
     * @param value 指标值
     * @param unit 单位
     */
    public static void logPerformance(Logger logger, String sessionUUID, String metric, double value, String unit) {
        logger.info("会话[{}] 性能监控 指标:{} 值:{} 单位:{}", sessionUUID, metric, value, unit);
    }
    
    /**
     * 记录调试日志（仅在 DEBUG 级别启用时输出）
     * 
     * @param logger 日志记录器
     * @param sessionUUID 会话唯一标识符
     * @param operation 操作描述
     * @param details 调试详情
     */
    public static void logDebug(Logger logger, String sessionUUID, String operation, String details) {
        if (logger.isDebugEnabled()) {
            logger.debug("会话[{}] 调试信息 {} 详情:{}", sessionUUID, operation, details);
        }
    }
    
    /**
     * 格式化参数列表
     * 
     * @param params 参数数组
     * @return 格式化后的参数字符串
     */
    private static String formatParams(Object... params) {
        if (params == null || params.length == 0) {
            return "";
        }
        return Arrays.stream(params)
                .map(param -> param == null ? "null" : param.toString())
                .collect(Collectors.joining(" "));
    }
    
    /**
     * 缩略字符串（用于 SQL 等长文本）
     * 
     * @param str 原始字符串
     * @param maxLength 最大长度
     * @return 缩略后的字符串
     */
    private static String abbreviate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
    
    /**
     * 安全获取 SessionUUID（处理空值情况）
     * 
     * @param sessionUUID 会话 UUID
     * @return 安全的 SessionUUID 字符串
     */
    public static String safeSessionUUID(String sessionUUID) {
        return sessionUUID != null ? sessionUUID : "unknown";
    }
    
    /**
     * 记录系统启动日志
     * 
     * @param logger 日志记录器
     * @param component 组件名称
     * @param version 版本信息
     */
    public static void logSystemStart(Logger logger, String component, String version) {
        logger.info("系统启动 组件:{} 版本:{}", component, version);
    }
    
    /**
     * 记录系统关闭日志
     * 
     * @param logger 日志记录器
     * @param component 组件名称
     * @param reason 关闭原因
     */
    public static void logSystemShutdown(Logger logger, String component, String reason) {
        logger.info("系统关闭 组件:{} 原因:{}", component, reason);
    }
}