# OJP Server 责任链模式架构设计文档

## 概述

OJP Server 采用责任链模式（Chain of Responsibility Pattern）来处理SQL语句的执行流程。这种架构设计实现了高度的模块化、可扩展性和可维护性，支持对SQL操作进行统一的拦截、处理和监控。

## 核心设计理念

### 1. 单一职责原则
每个处理器只负责特定的功能领域，如事务管理、权限控制、缓存处理等。

### 2. 开闭原则
通过SPI机制实现扩展，新增处理器无需修改现有代码。

### 3. 优先级驱动
处理器按优先级排序执行，确保处理顺序的正确性。

### 4. 条件化启用
支持基于配置和运行时条件动态启用/禁用处理器。

## 架构组件

### 1. 核心接口和抽象类

#### SqlProcessor 接口
```java
public interface SqlProcessor {
    boolean process(SqlProcessContext context) throws SQLException;
    void setNext(SqlProcessor next);
    String getProcessorName();
    int getPriority();
    boolean isEnabled();
    Set<SqlProcessContext.SqlOperationType> getSupportedOperations();
}
```

#### AbstractSqlProcessor 抽象类
提供责任链的基础实现：
- 责任链传递逻辑
- 操作类型过滤
- 性能监控
- 异常处理

### 2. 处理器实现

#### CircuitBreakerProcessor（优先级：120）
**功能**：熔断保护，防止系统过载
- 执行前检查熔断状态
- 记录执行成功/失败状态
- 自动熔断频繁失败的SQL语句
- 支持熔断器状态监控

**示例**：
```java
// 熔断器阻止执行
CircuitBreaker circuitBreaker = new CircuitBreaker(5000, 3);
CircuitBreakerProcessor processor = new CircuitBreakerProcessor(circuitBreaker, true);

// 当SQL失败次数超过阈值时，熔断器会阻止执行
```

#### TransactionProcessor（优先级：110）
**功能**：事务状态跟踪和事务权限控制
- 跟踪事务状态，影响缓存决策
- 记录写操作，确保ACID特性
- 事务权限控制
- 长时间运行事务监控

#### DataPermissionProcessor（优先级：100）
**功能**：根据用户权限动态修改SQL的WHERE条件
- 支持行级权限控制
- 动态SQL重写
- 多租户数据隔离

**示例**：
```sql
-- 原始SQL
SELECT * FROM users WHERE status = 'active'

-- 经过数据权限处理后（个人数据权限）
SELECT * FROM users WHERE status = 'active' AND (created_by = 'user123')

-- 经过数据权限处理后（部门数据权限）  
SELECT * FROM users WHERE status = 'active' AND (department_id IN ('dept1','dept2'))
```

#### CrudOperationProcessor（优先级：95）
**功能**：统一的CRUD操作处理和监控
- SQL注入检测和防护
- 危险操作拦截（如无WHERE的DELETE）
- 批量操作检测和优化
- 操作审计和监控

**示例**：
```sql
-- 危险操作被拦截
DELETE FROM users  -- 抛出异常：需要管理员权限

-- 批量操作被检测
INSERT INTO users VALUES ('A'),('B'),('C')  -- 标记为batch_insert=true
```

#### ShardingProcessor（优先级：80）
**功能**：根据分片规则进行SQL路由和表名重写
- 支持水平分片
- 动态表名重写
- 分片键路由

**示例**：
```sql
-- 原始SQL
SELECT * FROM users WHERE user_id = 12345

-- 经过分片处理后
SELECT * FROM users_1 WHERE user_id = 12345
```

#### SlowQuerySegregationProcessor（优先级：70）
**功能**：慢查询隔离处理
- 将慢查询路由到专用线程池
- 防止慢查询影响正常查询性能
- 支持动态调整隔离策略

#### SmartCacheProcessor（优先级：50）
**功能**：查询缓存处理
- 事务感知，确保ACID特性
- 规则引擎驱动
- 支持StarRocks缓存存储
- 异步缓存存储

#### SqlExecutionProcessor（优先级：-100）
**功能**：实际执行SQL语句
- 作为责任链的最后一环
- 执行实际的数据库操作
- 返回查询结果

## SPI扩展机制

### 1. SqlProcessorProvider 接口
```java
public interface SqlProcessorProvider {
    String getProcessorClassName();
    int getPriority();
    String getName();
    boolean isEnabled(ProcessorRegistrationContext context);
    ProcessorConfig getConfiguration(ProcessorRegistrationContext context);
    SqlProcessor createProcessor(ProcessorRegistrationContext context) throws Exception;
}
```

### 2. 处理器提供者实现

#### CircuitBreakerProcessorProvider
```java
public class CircuitBreakerProcessorProvider implements SqlProcessorProvider {
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.CircuitBreakerProcessor";
    }
    
    @Override
    public int getPriority() {
        return 120; // 最高优先级
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        Object circuitBreaker = context.getCircuitBreaker();
        return circuitBreaker != null &&
               (context.getServerConfiguration() == null || 
                context.getServerConfiguration().getCircuitBreakerTimeout() > 0);
    }
}
```

### 3. SPI注册配置
在 `META-INF/services/org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider` 中注册：
```
org.openjdbcproxy.grpc.server.chain.spi.providers.CircuitBreakerProcessorProvider
org.openjdbcproxy.grpc.server.chain.spi.providers.CrudOperationProcessorProvider
org.openjdbcproxy.grpc.server.chain.spi.providers.DataPermissionProcessorProvider
org.openjdbcproxy.grpc.server.chain.spi.providers.ShardingProcessorProvider
org.openjdbcproxy.grpc.server.chain.spi.providers.SlowQuerySegregationProcessorProvider
org.openjdbcproxy.grpc.server.chain.spi.providers.SmartCacheProcessorProvider
org.openjdbcproxy.grpc.server.chain.spi.providers.SqlExecutionProcessorProvider
org.openjdbcproxy.grpc.server.chain.spi.providers.TransactionProcessorProvider
```

## 责任链管理器

### SqlProcessorChain 类
负责组装和管理SQL处理责任链：

```java
public class SqlProcessorChain {
    private final List<SqlProcessor> processors = new ArrayList<>();
    private SqlProcessor chainHead;
    
    public SqlProcessorChain addProcessor(SqlProcessor processor) {
        if (processor != null && processor.isEnabled()) {
            processors.add(processor);
        }
        return this;
    }
    
    public void buildChain() {
        // 按优先级降序排序
        processors.sort(Comparator.comparingInt(SqlProcessor::getPriority).reversed());
        
        // 构建责任链
        for (int i = 0; i < processors.size() - 1; i++) {
            processors.get(i).setNext(processors.get(i + 1));
        }
        
        chainHead = processors.get(0);
    }
    
    public boolean process(SqlProcessContext context) throws SQLException {
        if (chainHead == null) {
            return false;
        }
        return chainHead.process(context);
    }
}
```

### 责任链构建器
提供流式API构建责任链：

```java
SqlProcessorChain chain = SqlProcessorChain.builder()
    .addCircuitBreaker(circuitBreaker)     // 优先级: 120
    .addTransaction()                       // 优先级: 110
    .addDataPermission()                    // 优先级: 100
    .addCrudOperation()                     // 优先级: 95
    .addSharding()                         // 优先级: 80
    .addSlowQuerySegregation()             // 优先级: 70
    .addSmartCache(cacheManager)           // 优先级: 50
    .addSqlExecution()                     // 优先级: -100
    .build();
```

## 处理上下文

### SqlProcessContext 类
封装SQL处理过程中的所有信息：

```java
public class SqlProcessContext {
    private final StatementRequest request;
    private final StreamObserver<OpResult> responseObserver;
    private String currentSql;
    private SqlOperationType operationType;
    private OpResult result;
    private SQLException error;
    private boolean completed;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    
    // 支持操作类型
    public enum SqlOperationType {
        SELECT, INSERT, UPDATE, DELETE, DDL, TRANSACTION, OTHER
    }
}
```

## 使用示例

### 1. 创建默认责任链
```java
// 创建智能缓存管理器
SmartCacheManager cacheManager = new SmartCacheManager(cacheConfig);

// 创建默认责任链（包含所有处理器）
SqlProcessorChain chain = SqlProcessorChain.createDefaultChain(cacheManager);
```

### 2. 自定义责任链
```java
SqlProcessorChain chain = SqlProcessorChain.builder()
    .addTransaction()              // 添加事务处理
    .addDataPermission()           // 添加数据权限处理
    .addCrudOperation()            // 添加CRUD操作处理
    .addSharding()                 // 添加分库分表处理
    .addSmartCache(cacheManager)   // 添加智能缓存处理
    .addSqlExecution()             // 添加SQL执行处理
    .addCustomProcessor(customProcessor) // 添加自定义处理器
    .build();
```

### 3. 处理SQL请求
```java
// 创建处理上下文
SqlProcessContext context = SqlProcessContext.create(request);

// 执行责任链处理
boolean handled = chain.process(context);

if (context.isCompleted() && context.getResult() != null) {
    // 处理完成，返回结果
    responseObserver.onNext(context.getResult());
    responseObserver.onCompleted();
} else if (!handled) {
    // 没有处理器处理请求
    throw new SQLException("No processor handled the SQL request");
}
```

## 扩展指南

### 1. 创建自定义处理器

#### 步骤1：实现SqlProcessor接口
```java
public class CustomProcessor extends AbstractSqlProcessor {
    private static final String PROCESSOR_NAME = "CustomProcessor";
    
    @Override
    public boolean doProcess(SqlProcessContext context) throws SQLException {
        // 实现自定义处理逻辑
        String sql = context.getCurrentSql();
        
        // 检查是否需要处理
        if (!shouldProcess(sql)) {
            return false; // 传递给下一个处理器
        }
        
        // 执行自定义处理
        processCustomLogic(context);
        
        return false; // 继续传递给下一个处理器
    }
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public int getPriority() {
        return 60; // 设置合适的优先级
    }
    
    @Override
    public Set<SqlProcessContext.SqlOperationType> getSupportedOperations() {
        return Set.of(SqlProcessContext.SqlOperationType.SELECT); // 只处理SELECT
    }
}
```

#### 步骤2：创建处理器提供者
```java
public class CustomProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public String getProcessorClassName() {
        return "org.openjdbcproxy.grpc.server.chain.processors.CustomProcessor";
    }
    
    @Override
    public int getPriority() {
        return 60;
    }
    
    @Override
    public String getName() {
        return "CustomProcessor";
    }
    
    @Override
    public boolean isEnabled(ProcessorRegistrationContext context) {
        // 根据配置决定是否启用
        return context.getServerConfiguration() != null &&
               context.getServerConfiguration().isCustomProcessorEnabled();
    }
}
```

#### 步骤3：注册SPI提供者
在 `META-INF/services/org.openjdbcproxy.grpc.server.chain.spi.SqlProcessorProvider` 中添加：
```
org.openjdbcproxy.grpc.server.chain.spi.providers.CustomProcessorProvider
```

### 2. 处理器配置注入

#### 通过构造函数注入
```java
public class ConfigurableProcessor extends AbstractSqlProcessor {
    private final String configValue;
    
    public ConfigurableProcessor(String configValue) {
        this.configValue = configValue;
    }
    
    // 实现处理逻辑...
}
```

#### 通过SPI提供者配置
```java
public class ConfigurableProcessorProvider implements SqlProcessorProvider {
    
    @Override
    public SqlProcessor createProcessor(ProcessorRegistrationContext context) throws Exception {
        String configValue = context.getServerConfiguration().getCustomConfig();
        return new ConfigurableProcessor(configValue);
    }
}
```

## 监控和调试

### 1. 责任链统计信息
```java
ChainStatistics stats = chain.getStatistics();
System.out.println("Total processors: " + stats.totalProcessors);
System.out.println("Enabled processors: " + stats.enabledProcessors);
```

### 2. 处理器查找
```java
SqlProcessor processor = chain.findProcessor("TransactionProcessor");
if (processor != null) {
    System.out.println("Found processor: " + processor.getProcessorName());
}
```

### 3. 处理器类型检查
```java
boolean hasCacheProcessor = chain.hasProcessor(SmartCacheProcessor.class);
System.out.println("Has cache processor: " + hasCacheProcessor);
```

## 性能优化

### 1. 处理器排序优化
- 按优先级预排序，避免运行时排序
- 使用缓存减少重复计算

### 2. 条件检查优化
- 在处理器级别进行快速过滤
- 避免不必要的处理器执行

### 3. 上下文复用
- 重用SqlProcessContext对象
- 减少对象创建开销

## 最佳实践

### 1. 处理器设计原则
- **单一职责**：每个处理器只负责一个特定功能
- **无状态**：处理器应该是无状态的，便于复用
- **快速失败**：在处理器中快速检查条件，避免不必要的处理
- **异常处理**：妥善处理异常，不影响责任链的正常执行

### 2. 优先级设计
- **熔断器**：最高优先级（120），在所有处理器之前执行
- **事务处理**：高优先级（110），确保事务状态正确
- **权限控制**：高优先级（100），在数据访问前进行权限检查
- **CRUD操作**：中高优先级（95），进行安全检查
- **分库分表**：中优先级（80），进行路由处理
- **慢查询隔离**：中优先级（70），进行性能优化
- **缓存处理**：中优先级（50），进行缓存操作
- **SQL执行**：最低优先级（-100），最后执行

### 3. 配置管理
- 使用SPI机制实现配置驱动的处理器启用
- 支持运行时动态配置
- 提供合理的默认配置

### 4. 测试策略
- 单元测试：测试每个处理器的独立功能
- 集成测试：测试责任链的完整流程
- 性能测试：测试责任链的性能影响

## 总结

OJP Server的责任链模式架构提供了：

1. **高度模块化**：每个处理器独立开发、测试和部署
2. **易于扩展**：通过SPI机制轻松添加新的处理器
3. **灵活配置**：支持条件化启用和优先级调整
4. **性能优化**：通过优先级排序和条件检查优化性能
5. **可维护性**：清晰的架构和良好的文档支持

这种架构设计使得OJP Server能够灵活地处理各种SQL操作需求，同时保持代码的简洁性和可维护性。
