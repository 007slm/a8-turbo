package org.openjdbcproxy.grpc.server;

import com.google.protobuf.ByteString;
import com.openjdbcproxy.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatementService gRPC服务的集成测试。
 * 测试连接数据库和执行查询的核心功能。
 */
class StatementServiceIntegrationTest {

    private ManagedChannel channel;
    private com.openjdbcproxy.grpc.StatementServiceGrpc.StatementServiceBlockingStub blockingStub;
    private com.openjdbcproxy.grpc.StatementServiceGrpc.StatementServiceStub asyncStub;
    
    private static final String TEST_DB_URL = "jdbc:mysql://localhost:3306/smartcache";
    private static final String TEST_USER = "root";
    private static final String TEST_PASSWORD = "a8";

    @BeforeEach
    void setUp() {
        // 创建客户端通道
        channel = ManagedChannelBuilder.forAddress("localhost", 8010)
                .usePlaintext()
                .build();
                
        blockingStub = com.openjdbcproxy.grpc.StatementServiceGrpc.newBlockingStub(channel);
        asyncStub = com.openjdbcproxy.grpc.StatementServiceGrpc.newStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
        
        System.clearProperty("ojp.server.port");
    }

    @Test
    void testConnectAndExecuteUpdate() throws Exception {
        // 测试连接到数据库
        ConnectionDetails connectionDetails = ConnectionDetails.newBuilder()
                .setUrl(TEST_DB_URL)
                .setUser(TEST_USER)
                .setPassword(TEST_PASSWORD)
                .setClientUUID("test-client")
                .build();
                
        SessionInfo sessionInfo = blockingStub.connect(connectionDetails);
        
        assertNotNull(sessionInfo);
        assertNotNull(sessionInfo.getConnHash());
        assertEquals("test-client", sessionInfo.getClientUUID());
        assertTrue(sessionInfo.getSessionUUID().isEmpty()); // 简单连接没有会话UUID
        
        // 在事务中创建表
        StatementRequest dropRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("DROP TABLE IF EXISTS test_table")
                .build();

        blockingStub.executeUpdate(dropRequest);

        // 测试使用executeUpdate创建表
        StatementRequest createTableRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(255))")
                .build();
            
        OpResult createResult = blockingStub.executeUpdate(createTableRequest);
        
        assertNotNull(createResult);
        assertEquals(ResultType.INTEGER, createResult.getType());
        assertEquals(0, (int) deserializeInteger(createResult.getValue())); // CREATE TABLE返回0更新
        
        // 测试插入数据
        StatementRequest insertRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("INSERT INTO test_table VALUES (1, 'test_name')")
                .build();
                
        OpResult insertResult = blockingStub.executeUpdate(insertRequest);
        
        assertNotNull(insertResult);
        assertEquals(ResultType.INTEGER, insertResult.getType());
        assertEquals(1, (int) deserializeInteger(insertResult.getValue())); // 插入了1行
    }

    @Test
    void testConnectAndExecuteQuery() throws Exception {
        // 首先连接并创建测试数据
        ConnectionDetails connectionDetails = ConnectionDetails.newBuilder()
                .setUrl(TEST_DB_URL)
                .setUser(TEST_USER)
                .setPassword(TEST_PASSWORD)
                .setClientUUID("test-client-query")
                .build();
                
        SessionInfo sessionInfo = blockingStub.connect(connectionDetails);

        // 在事务中创建表
        StatementRequest dropRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("DROP TABLE IF EXISTS query_test")
                .build();

        blockingStub.executeUpdate(dropRequest);

        // 创建表
        StatementRequest createTableRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("CREATE TABLE query_test (id INT PRIMARY KEY, name VARCHAR(255))")
                .build();
                
        blockingStub.executeUpdate(createTableRequest);
        
        // 插入测试数据
        StatementRequest insertRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("INSERT INTO query_test VALUES (1, 'test1'), (2, 'test2')")
                .build();
                
        blockingStub.executeUpdate(insertRequest);
        
        // 测试查询数据
        StatementRequest queryRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("SELECT * FROM query_test ORDER BY id")
                .build();
        
        // 使用CountDownLatch等待流完成
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<OpResult> resultRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        StreamObserver<OpResult> responseObserver = new StreamObserver<OpResult>() {
            @Override
            public void onNext(OpResult value) {
                resultRef.set(value);
            }

            @Override
            public void onError(Throwable t) {
                errorRef.set(new RuntimeException(t));
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };
        
        asyncStub.executeQuery(queryRequest, responseObserver);
        
        // 等待完成或超时
        assertTrue(latch.await(5, TimeUnit.SECONDS), "查询执行超时");
        
        // 检查错误
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        // 验证结果
        OpResult result = resultRef.get();
        assertNotNull(result, "查询结果不应为空");
        assertEquals(ResultType.RESULT_SET_DATA, result.getType());
    }

    @Test
    void testStartAndCommitTransaction() throws Exception {
        // 连接到数据库
        ConnectionDetails connectionDetails = ConnectionDetails.newBuilder()
                .setUrl(TEST_DB_URL)
                .setUser(TEST_USER)
                .setPassword(TEST_PASSWORD)
                .setClientUUID("test-client-tx")
                .build();
                
        SessionInfo sessionInfo = blockingStub.connect(connectionDetails);
        
        // 开始事务
        SessionInfo transactionSession = blockingStub.startTransaction(sessionInfo);
        
        assertNotNull(transactionSession);
        assertNotNull(transactionSession.getTransactionInfo());
        assertEquals(TransactionStatus.TRX_ACTIVE, transactionSession.getTransactionInfo().getTransactionStatus());
        assertFalse(transactionSession.getSessionUUID().isEmpty()); // 事务应创建会话UUID

        // 在事务中创建表
        StatementRequest dropRequest = StatementRequest.newBuilder()
                .setSession(transactionSession)
                .setSql("DROP TABLE IF EXISTS tx_test")
                .build();

        blockingStub.executeUpdate(dropRequest);

        // 在事务中创建表
        StatementRequest createRequest = StatementRequest.newBuilder()
                .setSession(transactionSession)
                .setSql("CREATE TABLE tx_test (id INT PRIMARY KEY, name VARCHAR(255))")
                .build();
                
        OpResult createResult = blockingStub.executeUpdate(createRequest);
        assertEquals(0, (int) deserializeInteger(createResult.getValue()));
        
        // 在事务中插入数据
        StatementRequest insertRequest = StatementRequest.newBuilder()
                .setSession(transactionSession)
                .setSql("INSERT INTO tx_test VALUES (1, 'tx_test')")
                .build();
                
        OpResult insertResult = blockingStub.executeUpdate(insertRequest);
        assertEquals(1, (int) deserializeInteger(insertResult.getValue()));
        
        // 提交事务
        SessionInfo committedSession = blockingStub.commitTransaction(transactionSession);
        
        assertNotNull(committedSession);
        assertNotNull(committedSession.getTransactionInfo());
        assertEquals(TransactionStatus.TRX_COMMITED, committedSession.getTransactionInfo().getTransactionStatus());
    }

    private int deserializeInteger(ByteString byteString) throws Exception {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(byteString.toByteArray());
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Integer) ois.readObject();
        }
    }
    
    private int findAvailablePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            return 0;
        }
    }
}