package org.openjdbcproxy.grpc.server.chain;

import com.openjdbcproxy.grpc.OpResult;
import com.openjdbcproxy.grpc.StatementRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdbcproxy.grpc.server.chain.processors.SlowQuerySegregationProcessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 慢查询隔离处理器测试
 */
public class SlowQuerySegregationProcessorTest {
    
    private SlowQuerySegregationProcessor processor;
    private StatementRequest mockRequest;
    private StreamObserver<OpResult> mockObserver;
    
    @BeforeEach
    void setUp() {
        // 创建启用的慢查询隔离处理器（10个槽位，20%用于慢查询）
        processor = new SlowQuerySegregationProcessor(
            10,  // totalSlots
            20,  // slowSlotPercentage
            5000, // idleTimeoutMs
            30000, // slowSlotTimeoutMs
            10000, // fastSlotTimeoutMs
            true  // enabled
        );
        
        mockRequest = mock(StatementRequest.class);
        mockObserver = mock(StreamObserver.class);
    }
    
    @Test
    void testProcessorInitialization() {
        assertEquals("SlowQuerySegregationProcessor", processor.getProcessorName());
        assertEquals(70, processor.getPriority());
        assertTrue(processor.isEnabled());
        
        var stats = processor.getStats();
        assertTrue(stats.isEnabled());
        assertEquals(0, stats.getTrackedOperationCount());
        assertEquals(0, stats.getTotalExecutionCount());
    }
    
    @Test
    void testDisabledProcessor() {
        SlowQuerySegregationProcessor disabledProcessor = new SlowQuerySegregationProcessor();
        
        assertFalse(disabledProcessor.isEnabled());
        
        var stats = disabledProcessor.getStats();
        assertFalse(stats.isEnabled());
        assertEquals("SlowQuerySegregationProcessor[enabled=false]", stats.toString());
    }
    
    @Test
    void testProcessSqlRequest() throws Exception {
        when(mockRequest.getSql()).thenReturn("SELECT * FROM users WHERE id = 1");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 处理SQL请求（不应该完成处理，应该传递给下一个处理器）
        boolean handled = processor.doProcess(context);
        
        assertFalse(handled, "处理器应该传递给下一个处理器，不应该完成处理");
        
        // 验证槽位相关属性被设置
        assertTrue(context.hasAttribute("slow_query_slot_acquired"));
        assertTrue(context.hasAttribute("slow_query_operation_hash"));
        assertTrue(context.hasAttribute("slow_query_is_slow"));
        assertTrue(context.hasAttribute("slow_query_slot_type"));
        
        // 验证性能监控属性
        assertTrue(context.hasAttribute("performance_operation_hash"));
        assertTrue(context.hasAttribute("performance_monitor_start_time"));
        
        System.out.println("处理的SQL: " + context.getCurrentSql());
        System.out.println("槽位类型: " + context.getAttribute("slow_query_slot_type"));
        System.out.println("操作哈希: " + context.getAttribute("slow_query_operation_hash"));
    }
    
    @Test
    void testSlowQueryClassification() throws Exception {
        when(mockRequest.getSql()).thenReturn("SELECT COUNT(*) FROM large_table WHERE complex_condition = 'value'");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 第一次执行
        processor.doProcess(context);
        
        String operationHash = context.getAttribute("slow_query_operation_hash");
        assertNotNull(operationHash);
        
        // 检查是否为慢查询（首次执行可能不是）
        Boolean isSlowOperation = context.getAttribute("slow_query_is_slow");
        assertNotNull(isSlowOperation);
        
        System.out.println("操作哈希: " + operationHash);
        System.out.println("是否为慢查询: " + isSlowOperation);
        System.out.println("历史平均时间: " + context.getAttribute("historical_avg_time"));
    }
    
    @Test
    void testPostProcessing() throws Exception {
        when(mockRequest.getSql()).thenReturn("UPDATE users SET last_login = NOW() WHERE id = 1");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 处理SQL请求
        processor.doProcess(context);
        
        // 验证清理回调被注册
        assertTrue(context.hasAttribute("slow_query_cleanup_callback"));
        assertTrue(context.hasAttribute("performance_cleanup_callback"));
        
        // 模拟执行完成，调用后处理
        context.markElapsed(); // 设置执行时间
        processor.postProcess(context);
        
        System.out.println("后处理完成，执行时间: " + context.getElapsedTime() + "ms");
    }
    
    @Test
    void testOperationHashGeneration() throws Exception {
        // 测试相同SQL生成相同哈希
        when(mockRequest.getSql()).thenReturn("SELECT * FROM users WHERE id = ?");
        
        SqlProcessContext context1 = SqlProcessContext.create(mockRequest);
        processor.doProcess(context1);
        String hash1 = context1.getAttribute("slow_query_operation_hash");
        
        SqlProcessContext context2 = SqlProcessContext.create(mockRequest);
        processor.doProcess(context2);
        String hash2 = context2.getAttribute("slow_query_operation_hash");
        
        assertEquals(hash1, hash2, "相同的SQL应该生成相同的操作哈希");
        
        // 测试不同SQL生成不同哈希
        when(mockRequest.getSql()).thenReturn("SELECT * FROM orders WHERE status = ?");
        SqlProcessContext context3 = SqlProcessContext.create(mockRequest);
        processor.doProcess(context3);
        String hash3 = context3.getAttribute("slow_query_operation_hash");
        
        assertNotEquals(hash1, hash3, "不同的SQL应该生成不同的操作哈希");
        
        System.out.println("SQL1哈希: " + hash1);
        System.out.println("SQL2哈希: " + hash2);
        System.out.println("SQL3哈希: " + hash3);
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
        assertEquals(0, stats.getTrackedOperationCount());
        assertEquals(0, stats.getTotalExecutionCount());
        assertEquals(0.0, stats.getOverallAverageTime(), 0.001);
        assertNotNull(stats.getSlotManagerStatus());
        
        System.out.println("处理器统计: " + stats.toString());
    }
    
    @Test
    void testQueryTypeDetection() throws Exception {
        String[] testSqls = {
            "SELECT * FROM users",
            "INSERT INTO users VALUES (1, 'test')",
            "UPDATE users SET name = 'updated'",
            "DELETE FROM users WHERE id = 1",
            "CREATE TABLE test (id INT)",
            "SELECT COUNT(*) FROM large_table GROUP BY category"
        };
        
        for (String sql : testSqls) {
            when(mockRequest.getSql()).thenReturn(sql);
            
            SqlProcessContext context = SqlProcessContext.create(mockRequest);
            processor.doProcess(context);
            
            String operationHash = context.getAttribute("slow_query_operation_hash");
            String slotType = context.getAttribute("slow_query_slot_type");
            
            assertNotNull(operationHash, "操作哈希不应为空: " + sql);
            assertNotNull(slotType, "槽位类型不应为空: " + sql);
            
            System.out.println("SQL: " + sql + " -> 哈希: " + operationHash + ", 槽位类型: " + slotType);
        }
    }
}