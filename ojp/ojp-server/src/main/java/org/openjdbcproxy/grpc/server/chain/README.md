# SQL处理责任链架构设计说明

## 概述

新的SQL处理责任链架构支持对**所有SQL操作**（SELECT、INSERT、UPDATE、DELETE、DDL、事务等）进行统一的拦截和处理，实现了：

1. **事务状态跟踪** - 跟踪事务状态，确保缓存安全性
2. **数据权限控制** - 动态修改WHERE条件，实现行级权限
3. **CRUD操作监控** - 统一的CRUD操作审计和安全检查
4. **分库分表** - SQL路由和表名重写  
5. **智能缓存** - 查询缓存和结果存储
6. **SQL执行** - 最终的SQL执行逻辑

## 架构组件

### 1. SqlProcessor接口
定义统一的SQL处理器接口，所有处理器都实现此接口。

### 2. AbstractSqlProcessor抽象类
提供责任链的基础实现，包括：
- 责任链传递逻辑
- 操作类型过滤  
- 性能监控
- 异常处理

### 3. 具体处理器

#### TransactionProcessor（优先级：110）
- **功能**：事务状态跟踪和事务权限控制
- **支持操作**：所有类型
- **特性**：
  - 跟踪事务状态，影响缓存决策
  - 记录写操作，确保ACID特性
  - 事务权限控制
  - 长时间运行事务监控

#### CrudOperationProcessor（优先级：95）
- **功能**：统一的CRUD操作处理和监控
- **支持操作**：所有类型
- **特性**：
  - SQL注入检测和防护
  - 危险操作拦截（如无WHERE的DELETE）
  - 批量操作检测和优化
  - 操作审计和监控
  - 操作级别权限控制
- **示例**：
  ```sql
  -- 危险操作被拦截
  DELETE FROM users  -- 抛出异常：需要管理员权限
  
  -- 批量操作被检测
  INSERT INTO users VALUES ('A'),('B'),('C')  -- 标记为batch_insert=true
  ```

#### DataPermissionProcessor（优先级：100）
- **功能**：根据用户权限动态修改SQL的WHERE条件
- **支持操作**：SELECT、INSERT、UPDATE、DELETE
- **示例**：
  ```sql
  -- 原始SQL
  SELECT * FROM users WHERE status = 'active'
  
  -- 经过数据权限处理后（个人数据权限）
  SELECT * FROM users WHERE status = 'active' AND (created_by = 'user123')
  
  -- 经过数据权限处理后（部门数据权限）  
  SELECT * FROM users WHERE status = 'active' AND (department_id IN ('dept1','dept2'))
  ```

#### ShardingProcessor（优先级：80）
- **功能**：根据分片规则进行SQL路由和表名重写
- **支持操作**：SELECT、INSERT、UPDATE、DELETE
- **示例**：
  ```sql
  -- 原始SQL
  SELECT * FROM users WHERE user_id = 12345
  
  -- 经过分片处理后
  SELECT * FROM users_1 WHERE user_id = 12345
  ```

#### SmartCacheProcessor（优先级：50）
- **功能**：查询缓存处理
- **支持操作**：SELECT
- **特性**：
  - 事务感知，确保ACID特性
  - 规则引擎驱动
  - 支持StarRocks缓存存储

#### SqlExecutionProcessor（优先级：-100）
- **功能**：实际执行SQL语句
- **支持操作**：所有类型
- **特性**：作为责任链的最后一环

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
// 创建SQL处理上下文
SqlProcessContext context = SqlProcessContext.create(request, responseObserver);
context.setConnectionSession(connectionSession);

// 执行责任链处理
boolean handled = chain.process(context);

if (context.isCompleted()) {
    // 处理完成，返回结果
    responseObserver.onNext(context.getResult());
    responseObserver.onCompleted();
}
```

## 扩展能力

### 1. 事务状态监控示例

```java
public class TransactionAuditProcessor extends AbstractSqlProcessor {
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        // 检查是否在事务中
        if (context.hasAttribute("in_transaction")) {
            // 记录事务中的操作
            logTransactionOperation(context);
            
            // 检查长时间运行事务
            if (context.hasAttribute("long_running_transaction")) {
                sendAlert("Long running transaction detected");
            }
        }
        
        return false;
    }
}
```

### 2. 危险操作监控示例

```java
public class SecurityAuditProcessor extends AbstractSqlProcessor {
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        // 检查危险操作
        if (context.hasAttribute("dangerous_operation")) {
            String pattern = context.getAttribute("danger_pattern");
            
            // 发送安全报警
            securityAlert(context.getUserContext().getUserId(), pattern, context.getCurrentSql());
            
            // 记录安全日志
            auditLogger.warn("Dangerous SQL executed: {} by user {}", 
                           context.getCurrentSql(), context.getUserContext().getUserId());
        }
        
        return false;
    }
}
```

### 3. 批量操作优化示例

```java
public class BatchOptimizationProcessor extends AbstractSqlProcessor {
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        // 检查批量操作
        if (context.hasAttribute("batch_insert")) {
            int batchSize = context.getAttribute("batch_size");
            
            if (batchSize > 1000) {
                // 大批量操作，建议分批处理
                context.setAttribute("suggest_split_batch", true);
                log.warn("Large batch operation detected: {} rows", batchSize);
            }
            
            // 设置批量处理参数
            context.setAttribute("batch_processing", true);
        }
        
        return false;
    }
}
```

### 4. 数据权限扩展示例

```java
public class CustomDataPermissionProcessor extends AbstractSqlProcessor {
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        UserContext userContext = getUserContext(context);
        
        switch (context.getOperationType()) {
            case UPDATE:
                // 动态修改UPDATE的WHERE条件
                String originalSql = context.getCurrentSql();
                String permissionCondition = "tenant_id = '" + userContext.getTenantId() + "'";
                String modifiedSql = addWhereCondition(originalSql, permissionCondition);
                context.updateSql(modifiedSql);
                break;
                
            case DELETE:
                // 限制DELETE操作的权限范围
                // 类似处理...
                break;
        }
        
        return false; // 继续传递给下一个处理器
    }
}
```

### 5. 分库分表扩展示例

```java
public class TimeBasedShardingProcessor extends AbstractSqlProcessor {
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        if (matchesTablePattern(context, "log_.*")) {
            // 对日志表按时间分片
            String originalSql = context.getCurrentSql();
            String timeBasedTable = determineTimeBasedTable(context);
            String modifiedSql = replaceTableName(originalSql, "log_table", timeBasedTable);
            context.updateSql(modifiedSql);
        }
        
        return false;
    }
    
    private String determineTimeBasedTable(SqlProcessContext context) {
        // 根据当前时间确定分片表名
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return "log_table_" + month;
    }
}
```

## SQL操作类型支持

### 基本操作类型

| SQL类型 | 事务跟踪 | 数据权限 | CRUD监控 | 分库分表 | 智能缓存 | 执行 |
|---------|----------|----------|----------|----------|----------|------|
| SELECT  | ✅       | ✅       | ✅       | ✅       | ✅       | ✅   |
| INSERT  | ✅       | ✅       | ✅       | ✅       | ❌       | ✅   |
| UPDATE  | ✅       | ✅       | ✅       | ✅       | ❌       | ✅   |
| DELETE  | ✅       | ✅       | ✅       | ✅       | ❌       | ✅   |
| CREATE  | ✅       | ✅       | ✅       | ✅       | ❌       | ✅   |
| DROP    | ✅       | ✅       | ✅       | ✅       | ❌       | ✅   |
| ALTER   | ✅       | ✅       | ✅       | ✅       | ❌       | ✅   |
| TRUNCATE| ✅       | ✅       | ✅       | ✅       | ❌       | ✅   |
| CALL    | ✅       | ✅       | ✅       | ✅       | ❌       | ✅   |

### 事务操作支持

| 事务操作 | 支持情况 | 功能说明 |
|------------|----------|----------|
| START TRANSACTION | ✅ | 事务开始通知，状态跟踪 |
| COMMIT | ✅ | 事务提交通知，清理状态 |
| ROLLBACK | ✅ | 事务回滚通知，清理状态 |
| SAVEPOINT | ✅ | 保存点管理 |
| AUTO COMMIT | ✅ | 自动提交模式切换 |

## 监控和调试

### 1. 责任链统计
```java
ChainStatistics stats = chain.getStatistics();
log.info("Chain statistics: {}", stats);
```

### 2. 处理器查找
```java
SqlProcessor processor = chain.findProcessor("DataPermissionProcessor");
if (processor != null) {
    log.info("Found processor: {}", processor.getProcessorName());
}
```

### 3. 性能监控
每个处理器的执行时间都会被记录，便于性能分析和调优。

## 配置示例

### application.yml
```yaml
ojp:
  sql-processing:
    chain:
      data-permission:
        enabled: true
        priority: 100
      sharding:
        enabled: true
        priority: 80
      smart-cache:
        enabled: true
        priority: 50
        
  smart-cache:
    starRocks:
      jdbcUrl: jdbc:mysql://localhost:9030/smart_cache
      username: root
      password: ""
      maximumPoolSize: 10
```

## 优势

1. **正交设计**：各功能模块独立，互不干扰
2. **易于扩展**：新功能只需添加新的处理器
3. **支持所有SQL类型**：不仅限于查询，支持完整的SQL操作
4. **性能监控**：内置性能监控和调试功能
5. **灵活配置**：支持动态启用/禁用处理器
6. **事务安全**：确保不破坏ACID特性

这个架构为后续的权限控制、分库分表、审计日志等功能预留了完整的扩展接口。