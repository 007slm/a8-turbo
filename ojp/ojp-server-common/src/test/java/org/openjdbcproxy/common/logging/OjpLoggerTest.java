package org.openjdbcproxy.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OjpLogger 单元测试
 * 
 * 验证日志工具类的各种方法能够正确格式化日志内容，
 * 确保 SessionUUID 包含和中文内容的正确性。
 */
@ExtendWith(MockitoExtension.class)
class OjpLoggerTest {
    
    @Mock
    private Logger mockLogger;
    
    private static final String TEST_SESSION_UUID = "a1b2c3d4-e5f6-7890-1234-567890abcdef";
    private static final String TEST_SQL = "SELECT * FROM users WHERE id = 1 AND name = 'test' AND status = 'active'";
    private static final String LONG_TEST_SQL = "SELECT u.id, u.name, u.email, u.status, p.profile_data, a.address_line1, a.address_line2, a.city, a.state, a.country FROM users u LEFT JOIN profiles p ON u.id = p.user_id LEFT JOIN addresses a ON u.id = a.user_id WHERE u.status = 'active' AND u.created_at > '2023-01-01' AND p.is_verified = true ORDER BY u.created_at DESC LIMIT 100";
    
    @BeforeEach
    void setUp() {
        // 默认不设置任何 stub，只在需要时设置
    }
    
    @Test
    void shouldLogSessionWithCorrectFormat() {
        // Given
        String action = "创建会话";
        String param1 = "客户端:test-client";
        String param2 = "只读:false";
        
        // When
        OjpLogger.logSession(mockLogger, TEST_SESSION_UUID, action, param1, param2);
        
        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> argsCaptor = ArgumentCaptor.forClass(Object.class);
        
        verify(mockLogger).info(messageCaptor.capture(), argsCaptor.capture(), argsCaptor.capture(), argsCaptor.capture());
        
        String logMessage = messageCaptor.getValue();
        Object[] args = argsCaptor.getAllValues().toArray();
        
        assertEquals("会话[{}] {} {}", logMessage);
        assertEquals(TEST_SESSION_UUID, args[0]);
        assertEquals(action, args[1]);
        assertEquals("客户端:test-client 只读:false", args[2]);
    }
    
    @Test
    void shouldLogSessionWithoutParams() {
        // Given
        String action = "终止会话";
        
        // When
        OjpLogger.logSession(mockLogger, TEST_SESSION_UUID, action);
        
        // Then
        verify(mockLogger).info("会话[{}] {}", TEST_SESSION_UUID, action);
    }
    
    @Test
    void shouldLogDatabaseOperationWithTime() {
        // Given
        String operation = "查询";
        String target = "MySQL";
        long timeMs = 150;
        
        // When
        OjpLogger.logDatabase(mockLogger, TEST_SESSION_UUID, operation, target, timeMs);
        
        // Then
        verify(mockLogger).info("会话[{}] 数据库操作 {} 目标:{} 耗时:{}ms", 
                TEST_SESSION_UUID, operation, target, timeMs);
    }
    
    @Test
    void shouldLogDatabaseOperationWithoutTime() {
        // Given
        String operation = "连接";
        String target = "StarRocks";
        
        // When
        OjpLogger.logDatabase(mockLogger, TEST_SESSION_UUID, operation, target);
        
        // Then
        verify(mockLogger).info("会话[{}] 数据库操作 {} 目标:{}", 
                TEST_SESSION_UUID, operation, target);
    }
    
    @Test
    void shouldLogCacheDecisionSingleTable() {
        // Given
        String decision = "命中";
        String table = "users";
        String reason = "表同步状态正常";
        
        // When
        OjpLogger.logCacheDecision(mockLogger, TEST_SESSION_UUID, decision, table, reason);
        
        // Then
        verify(mockLogger).info("会话[{}] 缓存决策 {} 表:{} 原因:{}", 
                TEST_SESSION_UUID, decision, table, reason);
    }
    
    @Test
    void shouldLogCacheDecisionMultipleTables() {
        // Given
        String decision = "未命中";
        Set<String> tables = Set.of("users", "orders", "products");
        String reason = "部分表同步延迟";
        
        // When
        OjpLogger.logCacheDecision(mockLogger, TEST_SESSION_UUID, decision, tables, reason);
        
        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> argsCaptor = ArgumentCaptor.forClass(Object.class);
        
        verify(mockLogger).info(messageCaptor.capture(), 
                argsCaptor.capture(), argsCaptor.capture(), argsCaptor.capture(), argsCaptor.capture());
        
        String logMessage = messageCaptor.getValue();
        Object[] args = argsCaptor.getAllValues().toArray();
        
        assertEquals("会话[{}] 缓存决策 {} 表:{} 原因:{}", logMessage);
        assertEquals(TEST_SESSION_UUID, args[0]);
        assertEquals(decision, args[1]);
        assertTrue(args[2].toString().contains("users"));
        assertTrue(args[2].toString().contains("orders"));
        assertTrue(args[2].toString().contains("products"));
        assertEquals(reason, args[3]);
    }
    
    @Test
    void shouldLogSlowQueryWithAbbreviation() {
        // Given
        long timeMs = 5000;
        
        // When
        OjpLogger.logSlowQuery(mockLogger, TEST_SESSION_UUID, LONG_TEST_SQL, timeMs);
        
        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> argsCaptor = ArgumentCaptor.forClass(Object.class);
        
        verify(mockLogger).warn(messageCaptor.capture(), 
                argsCaptor.capture(), argsCaptor.capture(), argsCaptor.capture());
        
        String logMessage = messageCaptor.getValue();
        Object[] args = argsCaptor.getAllValues().toArray();
        
        assertEquals("会话[{}] 慢查询检测 SQL:{} 耗时:{}ms", logMessage);
        assertEquals(TEST_SESSION_UUID, args[0]);
        String abbreviatedSql = args[1].toString();
        assertTrue(abbreviatedSql.endsWith("..."), "SQL should be abbreviated with '...'");
        assertTrue(abbreviatedSql.length() <= 103, "Abbreviated SQL should not exceed 103 characters (100 + '...')"); // 100 + "..."
        assertEquals(timeMs, args[2]);
    }
    
    @Test
    void shouldLogSlowQueryWithThreshold() {
        // Given
        String shortSql = "SELECT 1";
        long timeMs = 3000;
        long thresholdMs = 2000;
        
        // When
        OjpLogger.logSlowQuery(mockLogger, TEST_SESSION_UUID, shortSql, timeMs, thresholdMs);
        
        // Then
        verify(mockLogger).warn("会话[{}] 慢查询检测 SQL:{} 耗时:{}ms 阈值:{}ms", 
                TEST_SESSION_UUID, shortSql, timeMs, thresholdMs);
    }
    
    @Test
    void shouldLogErrorWithException() {
        // Given
        String operation = "执行查询";
        Exception exception = new RuntimeException("连接超时");
        
        // When
        OjpLogger.logError(mockLogger, TEST_SESSION_UUID, operation, exception);
        
        // Then
        verify(mockLogger).error("会话[{}] 操作异常 {} 错误:{}", 
                TEST_SESSION_UUID, operation, exception.getMessage(), exception);
    }
    
    @Test
    void shouldLogErrorWithMessage() {
        // Given
        String operation = "获取连接";
        String errorMessage = "连接池已满";
        
        // When
        OjpLogger.logError(mockLogger, TEST_SESSION_UUID, operation, errorMessage);
        
        // Then
        verify(mockLogger).error("会话[{}] 操作异常 {} 错误:{}", 
                TEST_SESSION_UUID, operation, errorMessage);
    }
    
    @Test
    void shouldLogConnectionPool() {
        // Given
        String action = "获取连接";
        String details = "活跃连接:5 空闲连接:3";
        
        // When
        OjpLogger.logConnectionPool(mockLogger, TEST_SESSION_UUID, action, details);
        
        // Then
        verify(mockLogger).info("会话[{}] 连接池操作 {} 详情:{}", 
                TEST_SESSION_UUID, action, details);
    }
    
    @Test
    void shouldLogTransaction() {
        // Given
        String action = "提交事务";
        String status = "成功";
        
        // When
        OjpLogger.logTransaction(mockLogger, TEST_SESSION_UUID, action, status);
        
        // Then
        verify(mockLogger).info("会话[{}] 事务操作 {} 状态:{}", 
                TEST_SESSION_UUID, action, status);
    }
    
    @Test
    void shouldLogGrpcCall() {
        // Given
        String method = "executeQuery";
        String status = "成功";
        long timeMs = 25;
        
        // When
        OjpLogger.logGrpcCall(mockLogger, TEST_SESSION_UUID, method, status, timeMs);
        
        // Then
        verify(mockLogger).info("会话[{}] gRPC调用 方法:{} 状态:{} 耗时:{}ms", 
                TEST_SESSION_UUID, method, status, timeMs);
    }
    
    @Test
    void shouldLogCdcSyncWithSession() {
        // Given
        String action = "创建作业";
        String jobId = "job-12345";
        String details = "表:users 目标:StarRocks";
        
        // When
        OjpLogger.logCdcSync(mockLogger, TEST_SESSION_UUID, action, jobId, details);
        
        // Then
        verify(mockLogger).info("会话[{}] CDC同步 {} 作业:{} 详情:{}", 
                TEST_SESSION_UUID, action, jobId, details);
    }
    
    @Test
    void shouldLogCdcSyncWithoutSession() {
        // Given
        String action = "作业状态变更";
        String jobId = "job-67890";
        String details = "状态:RUNNING";
        
        // When
        OjpLogger.logCdcSync(mockLogger, null, action, jobId, details);
        
        // Then
        verify(mockLogger).info("CDC同步 {} 作业:{} 详情:{}", action, jobId, details);
    }
    
    @Test
    void shouldLogCdcSyncWithEmptySession() {
        // Given
        String action = "同步完成";
        String jobId = "job-11111";
        String details = "同步记录数:1000";
        
        // When
        OjpLogger.logCdcSync(mockLogger, "", action, jobId, details);
        
        // Then
        verify(mockLogger).info("CDC同步 {} 作业:{} 详情:{}", action, jobId, details);
    }
    
    @Test
    void shouldLogPerformance() {
        // Given
        String metric = "查询响应时间";
        double value = 125.5;
        String unit = "ms";
        
        // When
        OjpLogger.logPerformance(mockLogger, TEST_SESSION_UUID, metric, value, unit);
        
        // Then
        verify(mockLogger).info("会话[{}] 性能监控 指标:{} 值:{} 单位:{}", 
                TEST_SESSION_UUID, metric, value, unit);
    }
    
    @Test
    void shouldLogDebugWhenEnabled() {
        // Given
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        String operation = "解析SQL";
        String details = "表名:users 字段:id,name";
        
        // When
        OjpLogger.logDebug(mockLogger, TEST_SESSION_UUID, operation, details);
        
        // Then
        verify(mockLogger).isDebugEnabled();
        verify(mockLogger).debug("会话[{}] 调试信息 {} 详情:{}", 
                TEST_SESSION_UUID, operation, details);
    }
    
    @Test
    void shouldNotLogDebugWhenDisabled() {
        // Given
        when(mockLogger.isDebugEnabled()).thenReturn(false);
        String operation = "解析SQL";
        String details = "表名:users";
        
        // When
        OjpLogger.logDebug(mockLogger, TEST_SESSION_UUID, operation, details);
        
        // Then
        verify(mockLogger).isDebugEnabled();
        verify(mockLogger, never()).debug(anyString(), any(), any(), any());
    }
    
    @Test
    void shouldLogSystemStart() {
        // Given
        String component = "OJP-Server";
        String version = "0.0.8-alpha";
        
        // When
        OjpLogger.logSystemStart(mockLogger, component, version);
        
        // Then
        verify(mockLogger).info("系统启动 组件:{} 版本:{}", component, version);
    }
    
    @Test
    void shouldLogSystemShutdown() {
        // Given
        String component = "OJP-Server";
        String reason = "正常关闭";
        
        // When
        OjpLogger.logSystemShutdown(mockLogger, component, reason);
        
        // Then
        verify(mockLogger).info("系统关闭 组件:{} 原因:{}", component, reason);
    }
    
    @Test
    void shouldHandleNullSessionUUID() {
        // Given
        String nullSessionUUID = null;
        
        // When
        String safeUUID = OjpLogger.safeSessionUUID(nullSessionUUID);
        
        // Then
        assertEquals("unknown", safeUUID);
    }
    
    @Test
    void shouldReturnValidSessionUUID() {
        // When
        String safeUUID = OjpLogger.safeSessionUUID(TEST_SESSION_UUID);
        
        // Then
        assertEquals(TEST_SESSION_UUID, safeUUID);
    }
    
    @Test
    void shouldHandleNullParamsInFormatParams() {
        // Given
        String action = "测试操作";
        
        // When
        OjpLogger.logSession(mockLogger, TEST_SESSION_UUID, action, (Object[]) null);
        
        // Then
        verify(mockLogger).info("会话[{}] {}", TEST_SESSION_UUID, action);
    }
    
    @Test
    void shouldHandleEmptyParamsInFormatParams() {
        // Given
        String action = "测试操作";
        
        // When
        OjpLogger.logSession(mockLogger, TEST_SESSION_UUID, action);
        
        // Then
        verify(mockLogger).info("会话[{}] {}", TEST_SESSION_UUID, action);
    }
    
    @Test
    void shouldHandleNullParamInFormatParams() {
        // Given
        String action = "测试操作";
        Object nullParam = null;
        String validParam = "有效参数";
        
        // When
        OjpLogger.logSession(mockLogger, TEST_SESSION_UUID, action, nullParam, validParam);
        
        // Then
        verify(mockLogger).info("会话[{}] {} {}", TEST_SESSION_UUID, action, "null 有效参数");
    }
    
    @Test
    void shouldAbbreviateNullString() {
        // Given
        String nullString = null;
        
        // When
        OjpLogger.logSlowQuery(mockLogger, TEST_SESSION_UUID, nullString, 1000);
        
        // Then
        ArgumentCaptor<Object> argsCaptor = ArgumentCaptor.forClass(Object.class);
        verify(mockLogger).warn(eq("会话[{}] 慢查询检测 SQL:{} 耗时:{}ms"), 
                eq(TEST_SESSION_UUID), argsCaptor.capture(), eq(1000L));
        
        assertEquals("null", argsCaptor.getValue());
    }
    
    @Test
    void shouldNotAbbreviateShortString() {
        // Given
        String shortSql = "SELECT 1";
        
        // When
        OjpLogger.logSlowQuery(mockLogger, TEST_SESSION_UUID, shortSql, 1000);
        
        // Then
        verify(mockLogger).warn("会话[{}] 慢查询检测 SQL:{} 耗时:{}ms", 
                TEST_SESSION_UUID, shortSql, 1000L);
    }
}