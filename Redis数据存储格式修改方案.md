# Redis数据存储格式修改方案

## 问题概述

通过对 `ojp-server` 和 `ojp-cache` 代码以及Redis实际存储数据的分析，发现存储格式与使用代码存在不匹配问题。本文档详细分析了问题根源并提供了修改方案。

## 问题分析

### 1. 文档设计 vs 实际实现差异

#### 文档设计（docs/Redis数据结构设计文档.md）
- **查询统计键格式**: `ojp:query:stats:{datasource}:{date}`
- **统计信息数据结构**: 使用JSON格式存储复杂统计信息
- **缓存键命名**: 严格按照 `ojp:{type}:{datasource}:{identifier}` 格式

#### 实际代码实现（AsyncStatsService.java）
- **日级统计键**: `ojp:stats:{datasource}:{date}` (缺少query层级)
- **小时级统计键**: `ojp:hourly:stats:{datasource}:{date-hour}`
- **表级统计键**: `ojp:table:stats:{datasource}:{tableName}`
- **缓存操作统计键**: `ojp:cache:stats:operation:{datasource}:{operation}`
- **性能统计键**: `ojp:performance:{datasource}:{queryId}`

### 2. 实际Redis存储格式分析

#### 当前Redis中的键模式
```
# 缓存操作统计 (Hash类型)
ojp:cache:stats:operation:07147f5bf6de9bd386ca601679eb64066124b3e00478fc8f988a71151c21186e:DECISION_SKIP
字段: totalCount=7, totalResponseTime=411, totalSqlLength=551, lastOperation=1758340777577

# 每日统计 (Hash类型)  
ojp:stats:07147f5bf6de9bd386ca601679eb64066124b3e00478fc8f988a71151c21186e:2025-09-20
字段: totalQueries=7, cacheMisses=7, totalResponseTime=102, maxResponseTime=73, minResponseTime=2

# 表格统计 (Hash类型)
ojp:table:stats:07147f5bf6de9bd386ca601679eb64066124b3e00478fc8f988a71151c21186e:order_items
字段: accessCount=2, totalResponseTime=18, cacheMisses=2, maxResponseTime=9, minResponseTime=9, lastAccessTime=1758340777557

# 慢查询 (Hash类型)
slow_query:1758340781152:40860c64-1faa-4904-81da-a92966a5f14c
字段: tableNames=products, parameters=[...], executionTime=10, timestamp=1758340781151, ...
```

### 3. 主要不匹配问题

#### 3.1 键命名不一致
- **文档**: `ojp:query:stats:{datasource}:{date}`
- **实际**: `ojp:stats:{datasource}:{date}`
- **影响**: 查询统计数据无法按文档预期的键格式访问

#### 3.2 数据结构不匹配
- **文档期望**: JSON格式的复杂统计对象
- **实际存储**: Redis Hash结构，字段分散存储
- **影响**: 数据读取和聚合逻辑需要适配Hash结构

#### 3.3 慢查询键格式差异
- **文档**: 未明确定义慢查询键格式
- **实际**: `slow_query:{timestamp}:{clientUUID}` 
- **影响**: 慢查询数据检索可能存在问题

## 修改方案：统一键命名规范

### 1. 修改AsyncStatsService.java中的键前缀常量

```java
// 当前实现
private static final String STATS_PREFIX = "ojp:stats:";
private static final String QUERY_STATS_PREFIX = "ojp:query:stats:";

// 修改为
private static final String STATS_PREFIX = "ojp:query:stats:";  // 统一使用query:stats
private static final String QUERY_STATS_PREFIX = "ojp:query:stats:";
```

### 2. 统一慢查询键格式

根据文档规范，慢查询应该使用统一的键格式：

```java
// 当前实现
private void recordSlowQuery(String datasource, Query query, long responseTime) {
    String slowQueryKey = "ojp:slow:queries:" + datasource;
    // ...
}

// 修改为
private void recordSlowQuery(String datasource, Query query, long responseTime) {
    String slowQueryKey = "ojp:slow:query:" + System.currentTimeMillis() + ":" + query.getClientUUID();
    // ...
}
```

### 3. 统一所有统计键前缀

需要修改的键前缀常量：

```java
// AsyncStatsService.java 中需要修改的常量
private static final String STATS_PREFIX = "ojp:query:stats:";           // 原: "ojp:stats:"
private static final String QUERY_STATS_PREFIX = "ojp:query:stats:";     // 保持不变
private static final String CACHE_STATS_PREFIX = "ojp:cache:stats:";     // 保持不变
private static final String PERFORMANCE_PREFIX = "ojp:performance:";     // 保持不变
private static final String TABLE_STATS_PREFIX = "ojp:table:stats:";     // 保持不变
private static final String HOURLY_STATS_PREFIX = "ojp:hourly:stats:";   // 保持不变
```

### 4. 保持Hash数据结构

考虑到Redis Hash结构在性能和内存使用上的优势，保持当前的Hash存储方式：

- 统一字段命名规范
- 确保所有统计字段完整性
- 添加必要的元数据字段

### 5. 具体修改内容

#### 5.1 日级统计键修改
```java
// updateDailyStats方法中
String dayKey = STATS_PREFIX + datasource + ":" + now.format(DAY_FORMATTER);
// 修改后的键格式: ojp:query:stats:{datasource}:{date}
```

#### 5.2 慢查询存储修改
```java
// recordSlowQuery方法中
String slowQueryKey = "ojp:slow:query:" + System.currentTimeMillis() + ":" + query.getClientUUID();
Map<String, Object> slowQueryData = new HashMap<>();
// 使用Hash结构存储慢查询详情
redisTemplate.opsForHash().putAll(slowQueryKey, slowQueryData);
```
```

## 实施建议

### 阶段一：代码修改（1-2天）
1. 修改AsyncStatsService.java中的键前缀常量
2. 统一慢查询键格式
3. 完善Hash字段命名规范
4. 添加单元测试验证新格式

### 阶段二：数据迁移（1天）
1. 创建数据迁移脚本
2. 在测试环境验证迁移效果
3. 生产环境执行迁移（建议在低峰期）

### 阶段三：清理工作（1天）
1. 清理旧格式的Redis键
2. 更新相关文档
3. 移除兼容性代码

## 风险评估

### 低风险
- Hash数据结构保持不变，不影响现有读取逻辑
- 键名修改对应用逻辑影响较小

### 中等风险
- 数据迁移期间可能出现数据不一致
- 需要协调多个服务的部署时间

### 缓解措施
- 使用双写策略确保过渡期数据完整性
- 提供回滚方案
- 充分的测试验证

## 总结

本方案通过统一键命名规范、保持Hash数据结构、采用渐进式迁移策略，可以有效解决当前Redis数据存储格式不匹配的问题。建议优先采用方案一（统一键命名规范），配合方案三（兼容性处理）来确保平滑过渡。