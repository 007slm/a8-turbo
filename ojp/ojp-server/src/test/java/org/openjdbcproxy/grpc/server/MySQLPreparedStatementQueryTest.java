package org.openjdbcproxy.grpc.server;

import com.google.protobuf.ByteString;
import org.openjdbcproxy.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdbcproxy.grpc.dto.Parameter;
import org.openjdbcproxy.grpc.dto.ParameterType;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL PreparedStatement查询功能的集成测试。
 * 与StatementServiceIntegrationTest使用相同的数据库配置进行测试。
 */
class MySQLPreparedStatementQueryTest {

    private ManagedChannel channel;
    private StatementServiceGrpc.StatementServiceBlockingStub blockingStub;
    private StatementServiceGrpc.StatementServiceStub asyncStub;

    // 使用与StatementServiceIntegrationTest相同的数据库配置
    private static final String TEST_DB_URL = "jdbc:mysql://localhost:3306/smartcache";
    private static final String TEST_USER = "root";
    private static final String TEST_PASSWORD = "a8";

    @BeforeEach
    void setUp() {
        // 创建客户端通道，使用与StatementServiceIntegrationTest相同的端口
        channel = ManagedChannelBuilder.forAddress("localhost", 8010)
                .usePlaintext()
                .build();

        blockingStub = StatementServiceGrpc.newBlockingStub(channel);
        asyncStub = StatementServiceGrpc.newStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testPreparedStatementQueryWithParameters() throws Exception {
        // 连接到数据库
        ConnectionDetails connectionDetails = ConnectionDetails.newBuilder()
                .setUrl(TEST_DB_URL)
                .setUser(TEST_USER)
                .setPassword(TEST_PASSWORD)
                .setClientUUID("test-client-prepared-stmt")
                .build();

        SessionInfo sessionInfo = blockingStub.connect(connectionDetails);

        // 创建测试表
        StatementRequest dropRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("DROP TABLE IF EXISTS prepared_stmt_test")
                .build();
        try {
            blockingStub.executeUpdate(dropRequest);
        } catch (StatusRuntimeException e) {
            // 如果表不存在，忽略错误
            if (e.getStatus().getCode() != Status.Code.UNKNOWN) {
                throw e;
            }
        }

        StatementRequest createTableRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("CREATE TABLE prepared_stmt_test (id INT PRIMARY KEY, name VARCHAR(255), age INT)")
                .build();
        blockingStub.executeUpdate(createTableRequest);

        // 插入测试数据
        StatementRequest insertRequest1 = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("INSERT INTO prepared_stmt_test VALUES (1, 'Alice', 25)")
                .build();
        blockingStub.executeUpdate(insertRequest1);

        StatementRequest insertRequest2 = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("INSERT INTO prepared_stmt_test VALUES (2, 'Bob', 30)")
                .build();
        blockingStub.executeUpdate(insertRequest2);

        StatementRequest insertRequest3 = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("INSERT INTO prepared_stmt_test VALUES (3, 'Charlie', 35)")
                .build();
        blockingStub.executeUpdate(insertRequest3);

        // 使用PreparedStatement查询特定ID的记录
        // 创建参数列表
        List<Parameter> params = new ArrayList<>();
        Parameter param = Parameter.builder()
                .index(1)
                .type(ParameterType.INT)
                .values(Arrays.asList(2))
                .build();
        params.add(param);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(params);
        oos.close();

        StatementRequest queryRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("SELECT * FROM prepared_stmt_test WHERE id = ?")
                .setParameters(ByteString.copyFrom(baos.toByteArray()))
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
    void testPreparedStatementQueryWithStringParameter() throws Exception {
        // 连接到数据库
        ConnectionDetails connectionDetails = ConnectionDetails.newBuilder()
                .setUrl(TEST_DB_URL)
                .setUser(TEST_USER)
                .setPassword(TEST_PASSWORD)
                .setClientUUID("test-client-prepared-stmt-str")
                .build();

        SessionInfo sessionInfo = blockingStub.connect(connectionDetails);

        // 使用PreparedStatement按名称查询
        // 创建参数列表
        List<Parameter> params = new ArrayList<>();
        Parameter param = Parameter.builder()
                .index(1)
                .type(ParameterType.STRING)
                .values(Arrays.asList("Alice"))
                .build();
        params.add(param);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(params);
        oos.close();

        StatementRequest queryRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("SELECT * FROM prepared_stmt_test WHERE name = ?")
                .setParameters(ByteString.copyFrom(baos.toByteArray()))
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
    void testPreparedStatementQueryWithMultipleParameters() throws Exception {
        // 连接到数据库
        ConnectionDetails connectionDetails = ConnectionDetails.newBuilder()
                .setUrl(TEST_DB_URL)
                .setUser(TEST_USER)
                .setPassword(TEST_PASSWORD)
                .setClientUUID("test-client-prepared-stmt-multi")
                .build();

        SessionInfo sessionInfo = blockingStub.connect(connectionDetails);

        // 使用PreparedStatement查询年龄范围
        // 创建参数列表
        List<Parameter> params = new ArrayList<>();
        Parameter param1 = Parameter.builder()
                .index(1)
                .type(ParameterType.INT)
                .values(Arrays.asList(26))
                .build();
        params.add(param1);
        
        Parameter param2 = Parameter.builder()
                .index(2)
                .type(ParameterType.INT)
                .values(Arrays.asList(34))
                .build();
        params.add(param2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(params);
        oos.close();

        StatementRequest queryRequest = StatementRequest.newBuilder()
                .setSession(sessionInfo)
                .setSql("SELECT * FROM prepared_stmt_test WHERE age BETWEEN ? AND ?")
                .setParameters(ByteString.copyFrom(baos.toByteArray()))
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

    
    // 删除serialize方法，因为已经不再需要了
}