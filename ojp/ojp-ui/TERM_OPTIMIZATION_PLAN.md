# 术语优化和产品名称去技术化方案

## 📋 优化目标

1. **去除技术化产品名称**：将 Prometheus、Redis、StarRocks 等技术产品名称替换为业务友好的名称
2. **添加术语解释**：为所有专业术语添加浮动 Tooltip 解释，提升用户体验

## ✅ 已完成的工作

### 1. 创建统一术语工具 (`src/utils/termDefinitions.jsx`)

**功能：**
- `TERM_DEFINITIONS`: 术语解释字典，包含 30+ 个常用术语
- `TermWithTooltip`: React 组件，自动为术语添加 Tooltip
- `SERVICE_NAME_MAP`: 技术名称到业务名称的映射
- `getBusinessName()`: 获取业务友好名称的工具函数

**使用示例：**
```jsx
import { TermWithTooltip } from '../utils/termDefinitions';

// 自动添加解释
<TermWithTooltip term="qps">每秒请求数</TermWithTooltip>

// 不显示图标
<TermWithTooltip term="cpu" showIcon={false}>CPU 使用率</TermWithTooltip>
```

### 2. 更新菜单名称 (`App.jsx`)

| 原名称 | 新名称 |
|--------|--------|
| OJP Cache | OJP 缓存 |
| Redis | 缓存服务 |
| StarRocks | 数据仓库 |
| Prometheus | 监控服务 |

### 3. 首页优化 (`HomePage.jsx`)

- ✅ 所有核心指标都添加了 Tooltip 解释
- ✅ 移除了 "Prometheus 实时监控" 标题中的技术名称
- ✅ 改为 "实时性能监控"

## 📝 待优化的页面

### 需要更新的监控页面：

1. **OJP 缓存监控** (`/monitor/cache/index.jsx`)
   - 标题：OJP Cache 监控 → OJP 缓存监控
   - 副标题：添加术语解释

2. **缓存服务监控** (`/monitor/redis/index.jsx`)
   - 标题：Redis 监控 → 缓存服务监控
   - 副标题：添加术语解释
   - 指标名称优化

3. **数据仓库监控** (`/monitor/starrocks/index.jsx`)
   - 标题：StarRocks 监控 → 数据仓库监控
   - 副标题：添加术语解释
   - 指标名称优化

4. **监控服务监控** (`/monitor/prometheus/index.jsx`)
   - 标题：Prometheus 监控 → 监控服务监控
   - 副标题：添加术语解释
   - 指标名称优化

5. **监控导航仪表板** (`/monitor/index.jsx`)
   - 更新所有服务卡片的名称和描述
   - 添加术语解释

## 🔧 实施步骤

### 步骤 1: 更新监控页面标题

为每个监控页面添加术语解释：

```jsx
import { TermWithTooltip } from '../../../utils/termDefinitions';

<MonitorLayout
    title="缓存服务监控"
    subtitle={
        <>
            <TermWithTooltip term="cache">缓存</TermWithTooltip>
            服务的核心性能指标和运行状态
        </>
    }
>
```

### 步骤 2: 优化指标卡片

为所有指标添加解释：

```jsx
<MetricStatCard
    title={
        <TermWithTooltip term="hitRate">
            命中率
        </TermWithTooltip>
    }
    query='...'
    unit="percent"
/>
```

### 步骤 3: 优化图表标题

```jsx
<PrometheusChart
    title={
        <>
            <TermWithTooltip term="qps" showIcon={false}>
                请求量
            </TermWithTooltip>
            趋势
        </>
    }
    query='...'
/>
```

## 📊 术语分类

### 系统资源类
- CPU、内存、堆内存、非堆内存、磁盘、运行时长

### 性能指标类
- QPS、TPS、响应时间、延迟、吞吐量

### Java/JVM 类
- JVM、GC、线程、新生代GC、完全GC

### 数据库类
- 连接池、活跃连接、空闲连接

### 缓存类
- 缓存、命中率、未命中率、淘汰

### 数据存储类
- 数据仓库、OLAP、OLTP

### 监控类
- 指标、时间序列、采集、目标

### CDC 类
- CDC、数据流、事件

## 🎯 优化效果

### 用户体验提升：
1. **降低学习成本**：用户无需了解技术产品名称
2. **提高可读性**：业务友好的名称更容易理解
3. **即时帮助**：鼠标悬停即可查看术语解释
4. **统一体验**：全站使用一致的术语和解释

### 示例对比：

**优化前：**
```
Prometheus 监控
Redis 内存使用
QPS: 1234
```

**优化后：**
```
监控服务监控
缓存服务内存使用
每秒请求数 ⓘ: 1234
（鼠标悬停显示：每秒请求数表示系统每秒处理的请求数量...）
```

## 📌 注意事项

1. **保持技术准确性**：业务名称要准确反映技术含义
2. **避免过度简化**：不要丢失重要的技术信息
3. **统一术语**：全站使用相同的术语和解释
4. **适度使用**：不是所有文字都需要添加 Tooltip，避免过度
5. **性能考虑**：Tooltip 组件较轻量，但大量使用时注意性能

## 🚀 下一步行动

1. ✅ 创建术语工具 (`termDefinitions.jsx`)
2. ✅ 更新 App.jsx 菜单
3. ✅ 优化首页 (`HomePage.jsx`)
4. ⏳ 更新 OJP 缓存监控页面
5. ⏳ 更新缓存服务监控页面
6. ⏳ 更新数据仓库监控页面
7. ⏳ 更新监控服务监控页面
8. ⏳ 更新监控导航仪表板
9. ⏳ 全局搜索并替换剩余的技术名称
10. ⏳ 测试所有页面的 Tooltip 功能

---

**创建时间**: 2025-12-19 23:47
**状态**: 进行中
**优先级**: 高
