package org.openjdbcproxy.grpc.server.chain;

import com.openjdbcproxy.grpc.OpResult;
import com.openjdbcproxy.grpc.StatementRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdbcproxy.grpc.server.chain.processors.*;
import org.openjdbcproxy.grpc.server.smartcache.SmartCacheManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 完整CRUD+事务责任链测试
 */
public class CompleteCrudChainTest {
    
    private SqlProcessorChain completeChain;
    private StatementRequest mockRequest;
    private StreamObserver<OpResult> mockObserver;
    
    @BeforeEach
    void setUp() {
        // 创建模拟对象
        mockRequest = mock(StatementRequest.class);
        mockObserver = mock(StreamObserver.class);
        
        // 创建完整的责任链
        completeChain = SqlProcessorChain.builder()
            .addTransaction()           // 优先级: 110
            .addDataPermission()        // 优先级: 100  
            .addCrudOperation()         // 优先级: 95
            .addSharding()             // 优先级: 80
            .addSqlExecution()         // 优先级: -100
            .build();
    }
    
    @Test
    void testCompleteInsertChain() throws Exception {
        // 测试INSERT操作的完整处理链
        when(mockRequest.getSql()).thenReturn("INSERT INTO users (name, email) VALUES ('John', 'john@example.com')");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 设置用户上下文
        UserContext userContext = UserContext.builder()
            .userId("user001")
            .tenantId("tenant001")
            .roles(Set.of("DATA_WRITER"))
            .dataScope(UserContext.DataScope.DEPARTMENT)
            .departments(Set.of("IT", "ENGINEERING"))
            .build();
        context.setUserContext(userContext);
        
        String originalSql = context.getCurrentSql();
        System.out.println("原始INSERT SQL: " + originalSql);
        
        // 执行责任链（不包括真正的执行器）
        SqlProcessorChain testChain = SqlProcessorChain.builder()
            .addTransaction()
            .addDataPermission()
            .addCrudOperation()
            .addSharding()
            .build();
        
        boolean handled = testChain.process(context);
        
        // 验证处理结果
        assertFalse(handled); // 没有执行器，所以不应该完成
        assertEquals(SqlOperationType.INSERT, context.getOperationType());
        
        // 验证审计记录
        assertTrue(context.hasAttribute("audit_record"));
        CrudOperationProcessor.AuditRecord audit = context.getAttribute("audit_record");
        assertEquals("user001", audit.getUserId());
        assertEquals(SqlOperationType.INSERT, audit.getOperationType());
        
        System.out.println("处理后SQL: " + context.getCurrentSql());
        System.out.println("审计记录: " + audit);
    }
    
    @Test
    void testCompleteUpdateWithPermissions() throws Exception {
        // 测试UPDATE操作的权限控制
        when(mockRequest.getSql()).thenReturn("UPDATE users SET salary = 50000 WHERE department_id = 1");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 设置用户上下文（部门数据权限）
        UserContext userContext = UserContext.builder()
            .userId("manager001")
            .tenantId("tenant001")
            .roles(Set.of("MANAGER", "DATA_WRITER"))
            .dataScope(UserContext.DataScope.DEPARTMENT)
            .departments(Set.of("DEPT1", "DEPT2"))
            .build();
        context.setUserContext(userContext);
        
        String originalSql = context.getCurrentSql();
        System.out.println("原始UPDATE SQL: " + originalSql);
        
        // 执行权限和CRUD处理
        SqlProcessorChain testChain = SqlProcessorChain.builder()
            .addDataPermission()
            .addCrudOperation()
            .build();
        
        boolean handled = testChain.process(context);
        
        // 验证SQL被修改（添加了数据权限条件）
        String finalSql = context.getCurrentSql();
        System.out.println("处理后SQL: " + finalSql);
        
        assertTrue(finalSql.contains("department_id IN"));
        assertTrue(context.hasAttribute("audit_record"));
        
        // 验证事务相关属性
        if (context.hasAttribute("has_write_operation")) {
            assertTrue(context.getAttribute("has_write_operation"));
            assertEquals(SqlOperationType.UPDATE, context.getAttribute("write_operation_type"));
        }
    }
    
    @Test
    void testDangerousDeleteOperation() throws Exception {
        // 测试危险的DELETE操作（没有WHERE条件）
        when(mockRequest.getSql()).thenReturn("DELETE FROM users");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 设置普通用户上下文
        UserContext userContext = UserContext.builder()
            .userId("user002")
            .roles(Set.of("DATA_WRITER"))
            .build();
        context.setUserContext(userContext);
        
        // 执行CRUD处理
        CrudOperationProcessor crudProcessor = new CrudOperationProcessor();
        
        // 应该抛出异常（危险操作）
        assertThrows(SQLException.class, () -> {
            crudProcessor.doProcess(context);
        });
        
        System.out.println("危险操作被正确拦截: " + context.getCurrentSql());
    }
    
    @Test
    void testTransactionStateTracking() throws Exception {
        // 测试事务状态跟踪
        String sessionId = "session_123";
        
        TransactionProcessor transactionProcessor = new TransactionProcessor();
        
        // 开始事务
        transactionProcessor.handleTransactionStart(sessionId);
        
        // 执行写操作
        when(mockRequest.getSql()).thenReturn("UPDATE users SET name = 'Updated' WHERE id = 1");
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        context.setSessionId(sessionId);
        
        transactionProcessor.doProcess(context);
        
        // 验证事务状态
        assertTrue(context.hasAttribute("has_write_operation"));
        assertTrue(context.hasAttribute("in_transaction"));
        
        // 提交事务
        transactionProcessor.handleTransactionCommit(sessionId);
        
        // 获取事务统计
        TransactionProcessor.TransactionStats stats = transactionProcessor.getTransactionStats();
        System.out.println("事务统计: " + stats);
        
        // 验证事务已结束
        assertEquals(0, stats.getActiveTransactionCount());
    }
    
    @Test
    void testCompleteSelectWithCaching() throws Exception {
        // 测试SELECT查询的完整处理（包括缓存）
        when(mockRequest.getSql()).thenReturn("SELECT * FROM users WHERE status = 'active'");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 设置用户上下文
        UserContext userContext = UserContext.builder()
            .userId("analyst001")
            .roles(Set.of("DATA_READER"))
            .dataScope(UserContext.DataScope.PERSONAL)
            .build();
        context.setUserContext(userContext);
        
        String originalSql = context.getCurrentSql();
        System.out.println("原始SELECT SQL: " + originalSql);
        
        // 执行完整的处理链（除了真正的执行和缓存）
        SqlProcessorChain testChain = SqlProcessorChain.builder()
            .addTransaction()
            .addDataPermission()
            .addCrudOperation()
            .addSharding()
            .build();
        
        boolean handled = testChain.process(context);
        
        // 验证权限过滤被添加
        String finalSql = context.getCurrentSql();
        System.out.println("处理后SQL: " + finalSql);
        
        assertTrue(finalSql.contains("created_by = 'analyst001'"));
        
        // 验证审计记录
        assertTrue(context.hasAttribute("audit_record"));
        CrudOperationProcessor.AuditRecord audit = context.getAttribute("audit_record");
        assertEquals(SqlOperationType.SELECT, audit.getOperationType());
        assertFalse(audit.isDangerous());
    }
    
    @Test
    void testBatchInsertDetection() throws Exception {
        // 测试批量插入检测
        when(mockRequest.getSql()).thenReturn(
            "INSERT INTO users (name, email) VALUES ('User1', 'user1@example.com'), ('User2', 'user2@example.com'), ('User3', 'user3@example.com')"
        );
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        CrudOperationProcessor crudProcessor = new CrudOperationProcessor();
        crudProcessor.doProcess(context);
        
        // 验证批量操作被检测
        assertTrue(context.hasAttribute("batch_insert"));
        assertEquals(3, (Integer) context.getAttribute("batch_size"));
        
        // 验证审计记录标记为批量操作
        CrudOperationProcessor.AuditRecord audit = context.getAttribute("audit_record");
        assertTrue(audit.isBatch());
        
        System.out.println("检测到批量插入: " + context.getAttribute("batch_size") + " 行");
    }
    
    @Test
    void testShardingWithPermissions() throws Exception {
        // 测试分片 + 权限的组合处理
        when(mockRequest.getSql()).thenReturn("SELECT * FROM users WHERE user_id = 12345");
        
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 设置用户上下文
        UserContext userContext = UserContext.builder()
            .userId("user003")
            .dataScope(UserContext.DataScope.PERSONAL)
            .build();
        context.setUserContext(userContext);
        
        String originalSql = context.getCurrentSql();
        System.out.println("原始SQL: " + originalSql);
        
        // 执行权限和分片处理
        SqlProcessorChain testChain = SqlProcessorChain.builder()
            .addDataPermission()
            .addSharding()
            .build();
        
        boolean handled = testChain.process(context);
        
        String finalSql = context.getCurrentSql();
        System.out.println("处理后SQL: " + finalSql);
        
        // 验证权限条件被添加
        assertTrue(finalSql.contains("created_by = 'user003'"));
        
        // 检查分片信息
        if (context.hasAttribute("shard_table_users")) {
            String shardedTable = context.getAttribute("shard_table_users");
            System.out.println("分片表: " + shardedTable);
        }
    }
    
    @Test
    void testChainStatistics() {
        // 测试责任链统计功能
        SqlProcessorChain.ChainStatistics stats = completeChain.getStatistics();
        
        assertEquals(5, stats.totalProcessors);
        assertEquals(5, stats.enabledProcessors);
        
        // 验证所有处理器都存在
        assertNotNull(completeChain.findProcessor("TransactionProcessor"));
        assertNotNull(completeChain.findProcessor("DataPermissionProcessor"));
        assertNotNull(completeChain.findProcessor("CrudOperationProcessor"));
        assertNotNull(completeChain.findProcessor("ShardingProcessor"));
        assertNotNull(completeChain.findProcessor("SqlExecutionProcessor"));
        
        System.out.println("责任链统计: " + stats);
    }
    
    @Test
    void testProcessorPriorities() {
        // 测试处理器优先级排序
        var processors = completeChain.getProcessors();
        
        // 验证优先级顺序
        assertEquals("TransactionProcessor", processors.get(0).getProcessorName());
        assertEquals("DataPermissionProcessor", processors.get(1).getProcessorName());
        assertEquals("CrudOperationProcessor", processors.get(2).getProcessorName());
        assertEquals("ShardingProcessor", processors.get(3).getProcessorName());
        assertEquals("SqlExecutionProcessor", processors.get(4).getProcessorName());
        
        System.out.println("处理器优先级顺序验证通过");
    }
}