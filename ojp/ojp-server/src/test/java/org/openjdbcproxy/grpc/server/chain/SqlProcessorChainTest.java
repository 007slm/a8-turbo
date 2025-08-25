package org.openjdbcproxy.grpc.server.chain;

import com.openjdbcproxy.grpc.OpResult;
import com.openjdbcproxy.grpc.StatementRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdbcproxy.grpc.server.chain.processors.DataPermissionProcessor;
import org.openjdbcproxy.grpc.server.chain.processors.ShardingProcessor;
import org.openjdbcproxy.grpc.server.chain.processors.SqlExecutionProcessor;
import org.openjdbcproxy.grpc.server.smartcache.SmartCacheManager;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SQL处理责任链测试用例
 */
public class SqlProcessorChainTest {
    
    private SqlProcessorChain chain;
    private StatementRequest mockRequest;
    private StreamObserver<OpResult> mockObserver;
    
    @BeforeEach
    void setUp() {
        // 创建模拟对象
        mockRequest = mock(StatementRequest.class);
        mockObserver = mock(StreamObserver.class);
        
        // 创建责任链
        chain = new SqlProcessorChain()
            .addProcessor(new DataPermissionProcessor())
            .addProcessor(new ShardingProcessor())
            .addProcessor(new SqlExecutionProcessor());
        
        chain.buildChain();
    }
    
    @Test
    void testDataPermissionProcessing() throws Exception {
        // 准备测试数据
        when(mockRequest.getSql()).thenReturn("SELECT * FROM users WHERE status = 'active'");
        
        // 创建处理上下文
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 设置用户上下文（个人数据权限）
        UserContext userContext = UserContext.builder()
            .userId("user123")
            .dataScope(UserContext.DataScope.PERSONAL)
            .build();
        context.setUserContext(userContext);
        
        // 执行责任链（只测试数据权限处理器）
        SqlProcessorChain testChain = new SqlProcessorChain()
            .addProcessor(new DataPermissionProcessor());
        testChain.buildChain();
        
        boolean handled = testChain.process(context);
        
        // 验证SQL被修改
        assertFalse(handled); // 数据权限处理器不应该完成处理
        assertTrue(context.getCurrentSql().contains("created_by = 'user123'"));
        
        System.out.println("原始SQL: " + mockRequest.getSql());
        System.out.println("修改后SQL: " + context.getCurrentSql());
    }
    
    @Test
    void testShardingProcessing() throws Exception {
        // 准备测试数据
        when(mockRequest.getSql()).thenReturn("SELECT * FROM users WHERE user_id = 12345");
        
        // 创建处理上下文
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 执行责任链（只测试分片处理器）
        SqlProcessorChain testChain = new SqlProcessorChain()
            .addProcessor(new ShardingProcessor());
        testChain.buildChain();
        
        boolean handled = testChain.process(context);
        
        // 验证分片处理
        assertFalse(handled); // 分片处理器不应该完成处理
        
        // 检查是否有分片相关的属性设置
        if (context.hasAttribute("shard_table_users")) {
            String shardedTable = context.getAttribute("shard_table_users");
            assertNotNull(shardedTable);
            assertTrue(shardedTable.startsWith("users_"));
            
            System.out.println("原始表名: users");
            System.out.println("分片表名: " + shardedTable);
        }
    }
    
    @Test
    void testCompleteChainProcessing() throws Exception {
        // 准备测试数据
        when(mockRequest.getSql()).thenReturn("UPDATE users SET name = 'John' WHERE user_id = 123");
        
        // 创建处理上下文
        SqlProcessContext context = SqlProcessContext.create(mockRequest);
        
        // 设置用户上下文（部门数据权限）
        UserContext userContext = UserContext.builder()
            .userId("user456")
            .dataScope(UserContext.DataScope.DEPARTMENT)
            .departments(Set.of("dept1", "dept2"))
            .build();
        context.setUserContext(userContext);
        
        // 模拟连接会话
        // ConnectionSessionDTO mockSession = mock(ConnectionSessionDTO.class);
        // context.setConnectionSession(mockSession);
        
        String originalSql = context.getCurrentSql();
        System.out.println("处理前SQL: " + originalSql);
        
        // 只测试权限和分片处理器，不执行实际SQL
        SqlProcessorChain testChain = new SqlProcessorChain()
            .addProcessor(new DataPermissionProcessor())
            .addProcessor(new ShardingProcessor());
        testChain.buildChain();
        
        boolean handled = testChain.process(context);
        
        // 验证处理结果
        assertFalse(handled); // 没有执行处理器，所以不应该被标记为已处理
        
        String finalSql = context.getCurrentSql();
        System.out.println("处理后SQL: " + finalSql);
        
        // 验证数据权限条件被添加
        assertTrue(finalSql.contains("department_id IN"));
        
        // 验证处理耗时
        assertTrue(context.getElapsedTime() >= 0);
    }
    
    @Test
    void testChainStatistics() {
        // 测试责任链统计功能
        SqlProcessorChain.ChainStatistics stats = chain.getStatistics();
        
        assertEquals(3, stats.totalProcessors);
        assertEquals(3, stats.enabledProcessors);
        
        System.out.println("责任链统计: " + stats.toString());
    }
    
    @Test
    void testProcessorLookup() {
        // 测试处理器查找功能
        SqlProcessor dataPermissionProcessor = chain.findProcessor("DataPermissionProcessor");
        assertNotNull(dataPermissionProcessor);
        assertEquals("DataPermissionProcessor", dataPermissionProcessor.getProcessorName());
        
        SqlProcessor nonExistentProcessor = chain.findProcessor("NonExistentProcessor");
        assertNull(nonExistentProcessor);
        
        // 测试处理器类型检查
        assertTrue(chain.hasProcessor(DataPermissionProcessor.class));
        assertFalse(chain.hasProcessor(String.class)); // 不是处理器类型
    }
    
    @Test
    void testBuilderPattern() {
        // 测试构建器模式
        SmartCacheManager mockCacheManager = mock(SmartCacheManager.class);
        
        SqlProcessorChain builderChain = SqlProcessorChain.builder()
            .addDataPermission()
            .addSharding()
            .addSqlExecution()
            .build();
        
        assertNotNull(builderChain);
        assertEquals(3, builderChain.getProcessors().size());
    }
    
    @Test
    void testSqlOperationTypes() {
        // 测试不同SQL操作类型的支持
        String[] sqls = {
            "SELECT * FROM users",           // SELECT
            "INSERT INTO users VALUES (1)", // INSERT  
            "UPDATE users SET name = 'x'",   // UPDATE
            "DELETE FROM users WHERE id=1",  // DELETE
            "CREATE TABLE test (id INT)",    // CREATE
            "DROP TABLE test",               // DROP
            "ALTER TABLE users ADD COLUMN x" // ALTER
        };
        
        for (String sql : sqls) {
            SqlProcessContext context = createContextForSql(sql);
            assertNotNull(context.getOperationType());
            assertNotEquals(SqlOperationType.UNKNOWN, context.getOperationType());
            
            System.out.println("SQL: " + sql + " -> 操作类型: " + context.getOperationType());
        }
    }
    
    private SqlProcessContext createContextForSql(String sql) {
        StatementRequest request = mock(StatementRequest.class);
        when(request.getSql()).thenReturn(sql);
        
        return SqlProcessContext.create(request);
    }
    
    /**
     * 演示自定义处理器的使用
     */
    static class CustomAuditProcessor extends AbstractSqlProcessor {
        
        @Override
        protected boolean doProcess(SqlProcessContext context) throws SQLException {
            // 审计日志处理器示例
            log.info("审计日志: 用户 {} 执行SQL: {}", 
                    context.getSessionId(), context.getCurrentSql());
            
            // 记录审计信息到数据库
            context.setAttribute("audit_logged", true);
            
            return false; // 继续传递给下一个处理器
        }
        
        @Override
        public String getProcessorName() {
            return "CustomAuditProcessor";
        }
        
        @Override
        public int getPriority() {
            return 90; // 在数据权限之后，分片之前执行
        }
    }
    
    @Test
    void testCustomProcessor() throws Exception {
        // 测试自定义处理器
        SqlProcessorChain customChain = new SqlProcessorChain()
            .addProcessor(new DataPermissionProcessor())
            .addProcessor(new CustomAuditProcessor())
            .addProcessor(new ShardingProcessor());
        
        customChain.buildChain();
        
        when(mockRequest.getSql()).thenReturn("SELECT * FROM sensitive_table");
        SqlProcessContext context = SqlProcessContext.create(mockRequest, mockObserver);
        
        boolean handled = customChain.process(context);
        
        // 验证审计处理器被执行
        assertTrue(context.hasAttribute("audit_logged"));
        assertEquals(true, context.getAttribute("audit_logged"));
        
        System.out.println("自定义处理器测试完成");
    }
}