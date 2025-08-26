# SQL处理器链架构

## 概述

SQL处理器链采用责任链模式，支持前处理和后处理机制，实现了灵活的SQL处理流程。

## 架构设计

### 核心接口

1. **SqlProcessor**: 基础处理器接口
2. **PreProcessor**: 前处理器接口，在SQL执行前进行预处理
3. **PostProcessor**: 后处理器接口，在SQL执行后进行后处理

### 抽象基类

**AbstractSqlProcessor**: 提供责任链的基础实现
- 自动检测并执行前处理（如果实现了PreProcessor接口）
- 提供后处理方法供责任链调用
- 处理异常和性能监控

### 处理流程

```
SQL请求 → 责任链启动 → 前处理 → 主处理 → 后处理 → 返回结果
```

1. **前处理阶段**: 每个处理器检查是否实现PreProcessor接口，如果是则执行preProcess方法
2. **主处理阶段**: 按优先级顺序执行处理器的doProcess方法
3. **后处理阶段**: 责任链完成后，调用所有实现PostProcessor接口的处理器的postProcess方法

## 处理器类型

### 前处理器 (PreProcessor)
- **CrudOperationProcessor**: SQL验证、危险操作检测、批量操作检测
- 在SQL执行前进行验证和准备

### 后处理器 (PostProcessor)
- **SqlStatisticsProcessor**: 记录SQL执行统计信息
- 在SQL执行完成后进行统计和监控

### 基础处理器
- **TransactionProcessor**: 事务管理
- **ShardingProcessor**: 分库分表
- **SmartCacheProcessor**: 智能缓存
- **SqlExecutionProcessor**: SQL执行

## 使用示例

### 创建处理器链

```java
SqlProcessorChain chain = new SqlProcessorChain();

// 添加处理器
chain.addProcessor(new CrudOperationProcessor())     // 前处理
     .addProcessor(new SqlStatisticsProcessor())     // 后处理
     .addProcessor(new TransactionProcessor())       // 基础处理
     .addProcessor(new SqlExecutionProcessor());     // 执行

chain.buildChain();
```

### 执行处理

```java
SqlProcessContext context = new SqlProcessContext();
context.setCurrentSql("SELECT * FROM users");
context.setOperationType(SqlProcessContext.SqlOperationType.SELECT);

boolean result = chain.process(context);
```

## 实现自定义处理器

### 前处理器

```java
@Component
public class CustomPreProcessor extends AbstractSqlProcessor implements PreProcessor {
    
    @Override
    public boolean doProcess(SqlProcessContext context) throws SQLException {
        // 主处理逻辑
        return false; // 继续传递
    }
    
    @Override
    public void preProcess(SqlProcessContext context) throws SQLException {
        // 前处理逻辑
        log.info("Pre-processing SQL: {}", context.getCurrentSql());
    }
    
    @Override
    public int getPriority() {
        return 90;
    }
}
```

### 后处理器

```java
@Component
public class CustomPostProcessor extends AbstractSqlProcessor implements PostProcessor {
    
    @Override
    public boolean doProcess(SqlProcessContext context) throws SQLException {
        // 主处理逻辑
        return false; // 继续传递
    }
    
    @Override
    public void postProcess(SqlProcessContext context) throws SQLException {
        // 后处理逻辑
        log.info("Post-processing completed for SQL: {}", context.getCurrentSql());
    }
    
    @Override
    public int getPriority() {
        return 80;
    }
}
```

## 优先级说明

处理器按优先级降序执行（优先级高的先执行）：

- 110: TransactionProcessor (事务管理)
- 100: SqlStatisticsProcessor (统计)
- 95: CrudOperationProcessor (CRUD验证)
- 80: ShardingProcessor (分库分表)
- 70: SlowQuerySegregationProcessor (慢查询隔离)
- 50: SmartCacheProcessor (智能缓存)
- -100: SqlExecutionProcessor (SQL执行)

## 优势

1. **职责分离**: 前处理、主处理、后处理职责明确
2. **灵活扩展**: 可以轻松添加新的处理器类型
3. **自动检测**: 自动检测处理器实现的接口类型
4. **异常隔离**: 前处理和后处理的异常不会影响主流程
5. **性能优化**: 异步执行统计等非关键操作

## 注意事项

1. 前处理异常不会中断主流程，只会记录警告日志
2. 后处理异常不会影响返回结果，只会记录警告日志
3. 处理器优先级决定了执行顺序，需要合理设置
4. 建议将耗时操作放在后处理中异步执行
