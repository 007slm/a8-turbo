package org.openjdbcproxy.grpc.server.chain;

import com.openjdbcproxy.grpc.OpResult;
import com.openjdbcproxy.grpc.StatementRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openjdbcproxy.grpc.server.CircuitBreaker;
import org.openjdbcproxy.grpc.server.chain.processors.CircuitBreakerProcessor;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 熔断器处理器测试
 */
public class CircuitBreakerProcessorTest {
    
    @Mock
    private CircuitBreaker mockCircuitBreaker;
    
    @Mock
    private StatementRequest mockRequest;
    
    @Mock
    private StreamObserver<OpResult> mockObserver;
    
    private CircuitBreakerProcessor processor;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new CircuitBreakerProcessor(mockCircuitBreaker, true);
    }
    
    @Test
    void testProcessorInitialization() {
        assertEquals("CircuitBreakerProcessor", processor.getProcessorName());
        assertEquals(120, processor.getPriority());
        assertTrue(processor.isEnabled());
        
        var stats = processor.getStats();
        assertTrue(stats.isEnabled());
    }
    
    @Test
    void testDisabledProcessor() {
        CircuitBreakerProcessor disabledProcessor = new CircuitBreakerProcessor();
        
        assertFalse(disabledProcessor.isEnabled());
        
        var stats = disabledProcessor.getStats();
        assertFalse(stats.isEnabled());
        assertEquals("CircuitBreakerProcessor[enabled=false]", stats.toString());
    }
    
    @Test
    void testSuccessfulProcessing() throws Exception {
        when(mockRequest.getSql()).thenReturn("SELECT * FROM users WHERE id = 1");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 模拟熔断器预检查成功
        doNothing().when(mockCircuitBreaker).preCheck(anyString());
        
        // 处理请求
        boolean handled = processor.doProcess(context);
        
        // 验证结果
        assertFalse(handled, "处理器应该传递给下一个处理器，不应该完成处理");
        
        // 验证熔断器相关属性被设置
        assertTrue(context.hasAttribute("circuit_breaker_hash"));
        assertTrue(context.hasAttribute("circuit_breaker_checked"));
        assertTrue(context.hasAttribute("circuit_breaker_success_callback"));
        assertTrue(context.hasAttribute("circuit_breaker_failure_callback"));
        
        // 验证熔断器被调用
        verify(mockCircuitBreaker, times(1)).preCheck(anyString());
        
        System.out.println("处理的SQL: " + context.getCurrentSql());
        System.out.println("熔断器哈希: " + context.getAttribute("circuit_breaker_hash"));
    }
    
    @Test
    void testCircuitBreakerBlocked() throws Exception {
        when(mockRequest.getSql()).thenReturn("SELECT * FROM problematic_table");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 模拟熔断器阻止执行
        doThrow(new SQLException("Circuit breaker is open"))
            .when(mockCircuitBreaker).preCheck(anyString());
        
        // 验证抛出异常
        SQLException exception = assertThrows(SQLException.class, () -> {
            processor.doProcess(context);
        });
        
        assertEquals("Circuit breaker is open", exception.getMessage());
        
        // 验证阻止状态被记录
        assertTrue(context.hasAttribute("circuit_breaker_blocked"));
        assertTrue(context.hasAttribute("circuit_breaker_block_reason"));
        
        System.out.println("熔断器阻止原因: " + context.getAttribute("circuit_breaker_block_reason"));
    }
    
    @Test
    void testPostProcessingSuccess() throws Exception {
        when(mockRequest.getSql()).thenReturn("UPDATE users SET last_login = NOW()");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 模拟预检查成功
        doNothing().when(mockCircuitBreaker).preCheck(anyString());
        
        // 处理请求
        processor.doProcess(context);
        
        // 模拟成功完成（没有错误）
        // context.getError() 为 null
        
        // 执行后处理
        processor.postProcess(context);
        
        // 验证成功回调被调用
        verify(mockCircuitBreaker, times(1)).onSuccess(anyString());
        
        System.out.println("后处理成功，熔断器记录成功状态");
    }
    
    @Test
    void testPostProcessingFailure() throws Exception {
        when(mockRequest.getSql()).thenReturn("INSERT INTO invalid_table VALUES (1)");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 模拟预检查成功
        doNothing().when(mockCircuitBreaker).preCheck(anyString());
        
        // 处理请求
        processor.doProcess(context);
        
        // 模拟执行失败
        SQLException executionError = new SQLException("Table not found");
        context.setError(executionError);
        
        // 执行后处理
        processor.postProcess(context);
        
        // 验证失败回调被调用
        verify(mockCircuitBreaker, times(1)).onFailure(anyString(), eq(executionError));
        
        System.out.println("后处理失败，熔断器记录失败状态");
    }
    
    @Test
    void testSupportedOperations() {
        var supportedOps = processor.getSupportedOperations();
        
        // 应该支持所有SQL操作类型
        assertTrue(supportedOps.contains(SqlOperationType.SELECT));
        assertTrue(supportedOps.contains(SqlOperationType.INSERT));
        assertTrue(supportedOps.contains(SqlOperationType.UPDATE));
        assertTrue(supportedOps.contains(SqlOperationType.DELETE));
        
        System.out.println("支持的操作类型: " + supportedOps);
    }
    
    @Test
    void testStatsReporting() {
        var stats = processor.getStats();
        
        assertNotNull(stats);
        assertTrue(stats.isEnabled());
        assertEquals(0, stats.getTotalCircuits()); // 简化实现返回0
        assertEquals(0, stats.getOpenCircuits());  // 简化实现返回0
        
        System.out.println("处理器统计: " + stats.toString());
    }
    
    @Test
    void testHighestPriority() {
        // 熔断器应该有最高优先级，在所有其他处理器之前执行
        assertEquals(120, processor.getPriority());
        
        System.out.println("熔断器优先级: " + processor.getPriority());
    }
}