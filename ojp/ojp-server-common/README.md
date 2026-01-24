# OJP gRPC Commons

OJP gRPC Commons 是 Open JDBC Proxy 的 gRPC 通信协议定义模块，基于 Protocol Buffers 提供客户端与服务端之间的标准化通信接口。

## 📋 目录

- [功能特性](#功能特性)
- [协议定义](#协议定义)
- [消息格式](#消息格式)
- [快速开始](#快速开始)
- [使用指南](#使用指南)
- [扩展开发](#扩展开发)
- [故障排查](#故障排查)

## ✨ 功能特性

### 核心功能

- **协议标准化**: 统一的 gRPC 通信协议定义
- **跨语言支持**: 支持多种编程语言的客户端实现
- **类型安全**: 强类型的消息定义和验证
- **版本兼容**: 向后兼容的协议演进支持

### 技术特性

- **Protocol Buffers**: Google 协议缓冲区格式
- **gRPC**: 高性能 RPC 框架
- **流式传输**: 支持双向流和服务器流
- **错误处理**: 结构化的错误响应格式

### 通信特性

- **连接管理**: 安全的数据库连接建立和管理
- **查询执行**: 完整的 SQL 查询生命周期支持
- **结果流式**: 大结果集的高效流式传输
- **事务控制**: 分布式事务的协议支持

## 📋 协议定义

### 服务接口

#### StatementService

主要的查询执行服务接口：

```protobuf
service StatementService {
  // 连接管理
  rpc Connect(ConnectionRequest) returns (ConnectionResponse);
  rpc Disconnect(DisconnectRequest) returns (DisconnectResponse);

  // 查询执行
  rpc ExecuteQuery(QueryRequest) returns (stream QueryResult);
  rpc ExecuteUpdate(UpdateRequest) returns (UpdateResponse);

  // 预编译语句
  rpc PrepareStatement(PrepareRequest) returns (PrepareResponse);
  rpc ExecutePrepared(ExecutePreparedRequest) returns (stream QueryResult);

  // 事务管理
  rpc BeginTransaction(TransactionRequest) returns (TransactionResponse);
  rpc CommitTransaction(CommitRequest) returns (CommitResponse);
  rpc RollbackTransaction(RollbackRequest) returns (RollbackResponse);

  // 元数据查询
  rpc GetMetaData(MetaDataRequest) returns (MetaDataResponse);
}
```

#### 流式接口

```protobuf
service StreamingService {
  // 大对象数据流
  rpc UploadLob(stream LobChunk) returns (LobResponse);
  rpc DownloadLob(LobRequest) returns (stream LobChunk);

  // 批量操作流
  rpc ExecuteBatch(stream BatchRequest) returns (stream BatchResponse);
}
```

### 核心消息类型

#### 连接相关

```protobuf
message ConnectionRequest {
  string database_url = 1;
  string username = 2;
  string password = 3;
  map<string, string> properties = 4;
  string client_id = 5;
}

message ConnectionResponse {
  string session_id = 1;
  string server_version = 2;
  repeated string supported_features = 3;
  Status status = 4;
}
```

#### 查询相关

```protobuf
message QueryRequest {
  string session_id = 1;
  string sql = 2;
  repeated Parameter parameters = 3;
  QueryOptions options = 4;
}

message QueryResult {
  repeated ColumnMetaData columns = 1;
  repeated RowData rows = 2;
  bool has_more = 3;
  string cursor_id = 4;
  Status status = 5;
}
```

#### 参数和结果

```protobuf
message Parameter {
  int32 index = 1;
  Value value = 2;
  int32 sql_type = 3;
}

message Value {
  oneof value {
    string string_value = 1;
    int64 int_value = 2;
    double double_value = 3;
    bool bool_value = 4;
    bytes bytes_value = 5;
    google.protobuf.Timestamp timestamp_value = 6;
    LobReference lob_reference = 7;
  }
}
```

## 📦 消息格式

### 数据类型映射

| JDBC 类型 | Protobuf 类型 | 说明 |
|-----------|---------------|------|
| VARCHAR | string | 字符串类型 |
| INTEGER | int64 | 整数类型 |
| DOUBLE | double | 浮点数类型 |
| BOOLEAN | bool | 布尔类型 |
| TIMESTAMP | google.protobuf.Timestamp | 时间戳 |
| BLOB/CLOB | LobReference | 大对象引用 |

### 状态码定义

```protobuf
enum StatusCode {
  SUCCESS = 0;
  INVALID_REQUEST = 1;
  AUTHENTICATION_FAILED = 2;
  CONNECTION_ERROR = 3;
  QUERY_ERROR = 4;
  TRANSACTION_ERROR = 5;
  TIMEOUT = 6;
  RESOURCE_EXHAUSTED = 7;
}
```

### 错误处理

```protobuf
message Status {
  StatusCode code = 1;
  string message = 2;
  map<string, string> details = 3;
}

message ErrorResponse {
  Status status = 1;
  string error_id = 2;
  google.protobuf.Timestamp timestamp = 3;
  repeated string stack_trace = 4;
}
```

## 🚀 快速开始

### 环境要求

- **Protocol Buffers**: 3.21+
- **gRPC**: 1.50+
- **Java**: 11+ (如果使用 Java 客户端)

### 生成代码

#### Java 代码生成

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.openjdbcproxy</groupId>
  <artifactId>ojp-grpc-commons</artifactId>
  <version>${project.version}</version>
</dependency>
```

#### Python 代码生成

```bash
# 安装依赖
pip install grpcio grpcio-tools

# 生成 Python 代码
python -m grpc_tools.protoc \
  --proto_path=. \
  --python_out=. \
  --grpc_python_out=. \
  *.proto
```

#### Go 代码生成

```bash
# 安装 protoc-gen-go
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest

# 生成 Go 代码
protoc --go_out=. --go-grpc_out=. *.proto
```

### 基本使用

#### Java 客户端

```java
// 创建通道
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 8010)
    .usePlaintext()
    .build();

// 创建客户端
StatementServiceGrpc.StatementServiceBlockingStub stub =
    StatementServiceGrpc.newBlockingStub(channel);

// 连接数据库
ConnectionRequest request = ConnectionRequest.newBuilder()
    .setDatabaseUrl("jdbc:mysql://localhost:3306/mydb")
    .setUsername("user")
    .setPassword("password")
    .build();

ConnectionResponse response = stub.connect(request);
```

#### Python 客户端

```python
import grpc
from ojp_grpc import statement_service_pb2, statement_service_pb2_grpc

# 创建通道
channel = grpc.insecure_channel('localhost:8010')

# 创建客户端
stub = statement_service_pb2_grpc.StatementServiceStub(channel)

# 连接数据库
request = statement_service_pb2.ConnectionRequest(
    database_url='jdbc:mysql://localhost:3306/mydb',
    username='user',
    password='password'
)

response = stub.Connect(request)
```

## 📚 使用指南

### 连接管理

#### 建立连接

```java
ConnectionRequest request = ConnectionRequest.newBuilder()
    .setDatabaseUrl("jdbc:mysql://localhost:3306/mydb")
    .setUsername("user")
    .setPassword("password")
    .putProperties("autoReconnect", "true")
    .setClientId("client-001")
    .build();

ConnectionResponse response = stub.connect(request);
String sessionId = response.getSessionId();
```

#### 断开连接

```java
DisconnectRequest request = DisconnectRequest.newBuilder()
    .setSessionId(sessionId)
    .build();

DisconnectResponse response = stub.disconnect(request);
```

### 查询执行

#### 简单查询

```java
QueryRequest request = QueryRequest.newBuilder()
    .setSessionId(sessionId)
    .setSql("SELECT id, name FROM users WHERE age > ?")
    .addParameters(Parameter.newBuilder()
        .setIndex(1)
        .setValue(Value.newBuilder().setIntValue(18)))
    .build();

Iterator<QueryResult> results = stub.executeQuery(request);
while (results.hasNext()) {
    QueryResult result = results.next();
    // 处理结果
}
```

#### 预编译查询

```java
PrepareStatementRequest prepareRequest = PrepareStatementRequest.newBuilder()
    .setSessionId(sessionId)
    .setSql("SELECT * FROM users WHERE id = ?")
    .build();

PrepareStatementResponse prepareResponse = stub.prepareStatement(prepareRequest);

ExecutePreparedRequest executeRequest = ExecutePreparedRequest.newBuilder()
    .setStatementId(prepareResponse.getStatementId())
    .addParameters(Parameter.newBuilder()
        .setIndex(1)
        .setValue(Value.newBuilder().setIntValue(123)))
    .build();

Iterator<QueryResult> results = stub.executePrepared(executeRequest);
```

### 事务管理

```java
// 开始事务
TransactionResponse txResponse = stub.beginTransaction(
    TransactionRequest.newBuilder()
        .setSessionId(sessionId)
        .setIsolationLevel(IsolationLevel.READ_COMMITTED)
        .build()
);

// 执行操作
// ...

// 提交事务
CommitResponse commitResponse = stub.commitTransaction(
    CommitRequest.newBuilder()
        .setSessionId(sessionId)
        .build()
);
```

### 错误处理

```java
try {
    ConnectionResponse response = stub.connect(request);
    if (response.getStatus().getCode() != StatusCode.SUCCESS) {
        log.error("Connection failed: {}", response.getStatus().getMessage());
    }
} catch (StatusRuntimeException e) {
    Status status = Status.fromThrowable(e);
    log.error("gRPC error: {} - {}", status.getCode(), status.getDescription());
}
```

## 🔧 扩展开发

### 添加新消息类型

1. **定义协议**:
```protobuf
// 新增消息类型
message CustomQueryRequest {
  string session_id = 1;
  string custom_param = 2;
  repeated string filters = 3;
}
```

2. **生成代码**:
```bash
# 重新生成代码
mvn clean compile
```

3. **实现服务**:
```java
@Override
public CustomQueryResponse customQuery(CustomQueryRequest request) {
    // 实现自定义逻辑
    return CustomQueryResponse.newBuilder()
        .setResult("custom result")
        .build();
}
```

### 添加新服务

1. **定义服务**:
```protobuf
service CustomService {
  rpc CustomOperation(CustomRequest) returns (CustomResponse);
}
```

2. **实现服务类**:
```java
@GrpcService
public class CustomServiceImpl extends CustomServiceGrpc.CustomServiceImplBase {
    @Override
    public void customOperation(CustomRequest request,
                               StreamObserver<CustomResponse> responseObserver) {
        // 实现逻辑
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

### 版本兼容性

#### 添加字段

```protobuf
message ExampleMessage {
  string existing_field = 1;
  string new_field = 2;  // 添加新字段，使用新编号
}
```

#### 废弃字段

```protobuf
message ExampleMessage {
  string active_field = 1;
  reserved 2;  // 保留编号，避免重用
  // reserved "deprecated_field";  // 可选：保留字段名
}
```

## 🔧 故障排查

### 常见问题

#### 1. 连接失败

**可能原因**:
- 服务端未启动
- 端口配置错误
- 网络连接问题

**解决方法**:
```bash
# 检查服务状态
curl http://localhost:8010/actuator/health

# 检查端口监听
netstat -tlnp | grep 8010

# 测试连接
grpcurl -plaintext localhost:8010 list
```

#### 2. 协议不匹配

**可能原因**:
- 客户端和服务端使用不同版本的协议
- 代码生成不一致

**解决方法**:
```bash
# 检查版本
grep "version" pom.xml

# 重新生成代码
mvn clean compile

# 验证服务列表
grpcurl -plaintext localhost:8010 list
```

#### 3. 流式传输问题

**可能原因**:
- 网络超时设置不当
- 客户端处理速度慢

**解决方法**:
```java
// 设置超时
stub.withDeadlineAfter(30, TimeUnit.SECONDS)

// 增加缓冲区
.withMaxInboundMessageSize(50 * 1024 * 1024)
```

### 调试技巧

#### 启用 gRPC 调试

```java
// Java 客户端
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 8010)
    .usePlaintext()
    .intercept(new LoggingInterceptor())  // 添加日志拦截器
    .build();
```

#### 查看协议详情

```bash
# 查看服务定义
grpcurl -plaintext localhost:8010 describe StatementService

# 测试方法调用
grpcurl -plaintext -d '{"session_id": "test"}' \
  localhost:8010 StatementService/GetMetaData
```

### 性能优化

#### 连接池配置

```java
ManagedChannel channel = NettyChannelBuilder
    .forAddress("localhost", 8010)
    .usePlaintext()
    .maxInboundMessageSize(50 * 1024 * 1024)  // 50MB
    .keepAliveTime(30, TimeUnit.SECONDS)
    .keepAliveTimeout(10, TimeUnit.SECONDS)
    .build();
```

#### 批量操作优化

```java
// 使用流式批量操作
StreamObserver<BatchRequest> requestObserver = stub.executeBatch(responseObserver);

// 发送批量请求
for (BatchItem item : batchItems) {
    requestObserver.onNext(BatchRequest.newBuilder()
        .setItem(item)
        .build());
}
requestObserver.onCompleted();
```

## 📊 监控指标

### gRPC 指标

- **请求数量**: 按服务和方法统计
- **响应时间**: 请求处理的延迟分布
- **错误率**: 按错误类型统计
- **连接状态**: 活跃连接和连接池状态

### 集成 Prometheus

```yaml
# 启用 gRPC 指标
grpc:
  server:
    metrics:
      enabled: true
  client:
    metrics:
      enabled: true
```

```java
@Configuration
public class GrpcMetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> grpcMetrics() {
        return registry -> registry.config()
            .commonTags("component", "grpc");
    }
}
```

## 🤝 开发指南

### 项目结构

```
ojp-grpc-commons/
├── src/main/proto/
│   ├── statement_service.proto    # 主要服务定义
│   ├── streaming_service.proto    # 流式服务定义
│   ├── common.proto               # 通用消息类型
│   └── error.proto                # 错误定义
├── src/main/java/
│   └── org/openjdbcproxy/grpc/
│       ├── StatementServiceGrpc.java
│       ├── StreamingServiceGrpc.java
│       └── model/                 # 生成的消息类
├── src/test/java/
│   └── org/openjdbcproxy/grpc/
│       ├── StatementServiceTest.java
│       └── StreamingServiceTest.java
└── pom.xml                        # Maven 配置
```

### 代码生成配置

```xml
<plugin>
  <groupId>org.xolstice.maven.plugins</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>0.6.1</version>
  <configuration>
    <protocArtifact>com.google.protobuf:protoc:3.21.0</protocArtifact>
    <pluginId>grpc-java</pluginId>
    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.50.0</pluginArtifact>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>compile</goal>
        <goal>compile-custom</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

### 测试编写

```java
@Test
public void testConnection() {
    // 创建测试通道
    ManagedChannel channel = InProcessChannelBuilder
        .forName("test")
        .build();

    // 创建客户端
    StatementServiceGrpc.StatementServiceBlockingStub stub =
        StatementServiceGrpc.newBlockingStub(channel);

    // 执行测试
    ConnectionRequest request = ConnectionRequest.newBuilder()
        .setDatabaseUrl("jdbc:test://localhost")
        .build();

    // 断言结果
    // ...
}
```

## 📄 许可证

本项目采用 MIT 许可证。

---

**OJP gRPC Commons** - 标准化、高性能的数据库代理通信协议！</content>
<parameter name="filePath">E:\a8-turbo\ojp\ojp-grpc-commons\README.md