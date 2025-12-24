# Grafana 页面迁移总结

## 📋 迁移概述

本次迁移将所有 Grafana iframe 监控页面替换为原生 React 组件，实现了更好的性能和用户体验。

## ✅ 已完成的工作

### 1. 创建原生监控页面

所有监控页面都位于 `src/pages/monitor/` 目录下：

- **OJP Server 监控** (`/monitor/server`)
  - CPU 使用率、JVM 堆内存、QPS、响应时间
  - QPS 趋势、响应延迟、JVM 内存使用、线程状态、GC 暂停时间

- **OJP Cache 监控** (`/monitor/cache`)
  - 缓存命中率、决策总数、决策延迟、CDC 事件
  - 命中率趋势、延迟分布、查询对比、Stream 事件吞吐

- **Redis 监控** (`/monitor/redis`)
  - 内存使用、连接客户端、命令 QPS、Key 数量
  - 内存趋势、QPS 曲线、连接数、命中率

- **StarRocks 监控** (`/monitor/starrocks`)
  - FE/BE 节点数、查询 QPS、活跃查询
  - QPS 趋势、查询延迟 P99、CPU 使用率、数据导入速率

- **Prometheus 监控** (`/monitor/prometheus`)
  - 时间序列数、样本总数、Scrape 目标数、间隔
  - 时间序列趋势、样本摄入速率、Scrape 耗时、存储块数量

### 2. 创建统一监控导航页面

- **监控仪表板** (`/monitor`)
  - 提供所有监控页面的可视化入口
  - 卡片式设计，每个服务一个卡片
  - 显示关键指标预览
  - 渐变色背景，动画效果
  - 保留 Grafana 访问入口作为备选

### 3. 更新应用路由和菜单

**新的菜单结构：**
```
├── 系统监控 (/)
├── 原生监控 (/monitor)
│   ├── OJP Server
│   ├── OJP Cache
│   ├── Redis
│   ├── StarRocks
│   └── Prometheus
├── Grafana 监控 (/monitoring)
│   ├── 监控总览
│   └── 各服务 Grafana 仪表板
├── 缓存管理
└── ShopService
```

**新的路由配置：**
- `/monitor` - 监控导航仪表板
- `/monitor/server` - OJP Server 原生监控
- `/monitor/cache` - OJP Cache 原生监控
- `/monitor/redis` - Redis 原生监控
- `/monitor/starrocks` - StarRocks 原生监控
- `/monitor/prometheus` - Prometheus 原生监控
- `/monitoring` - Grafana 监控总览（保留）

## 🎨 技术特性

### 1. 组件架构
- **MonitorLayout**: 统一的监控页面布局组件
  - 时间范围选择器（1h, 6h, 12h, 24h, 7d）
  - 自动刷新功能（30秒间隔）
  - 响应式设计

- **PrometheusChart**: Prometheus 数据可视化组件
  - 支持多种图表类型（line, area）
  - 实时数据查询
  - 自动单位转换（bytes, ms, percent 等）
  - 自定义颜色方案

- **MetricStatCard**: 指标统计卡片
  - 实时数值显示
  - 图标和颜色自定义
  - 加载状态

### 2. 数据查询
- 使用 `react-query` 进行数据管理
- 自动缓存和重新验证
- 错误处理和加载状态
- 支持自定义刷新间隔

### 3. 用户体验
- 🎨 现代化 UI 设计
- 📱 完全响应式布局
- ⚡ 快速加载，无 iframe 延迟
- 🔄 实时数据更新
- 🎯 直观的导航体验

## 📦 文件结构

```
src/pages/monitor/
├── index.jsx                    # 监控导航仪表板
├── MonitorDashboard.css         # 仪表板样式
├── MonitorLayout.jsx            # 统一布局组件
├── components/
│   └── MetricStatCard.jsx       # 指标卡片组件
├── server/
│   └── index.jsx                # OJP Server 监控
├── cache/
│   └── index.jsx                # OJP Cache 监控
├── redis/
│   └── index.jsx                # Redis 监控
├── starrocks/
│   └── index.jsx                # StarRocks 监控
└── prometheus/
    └── index.jsx                # Prometheus 监控
```

## 🔧 配置说明

### Prometheus 查询示例

**CPU 使用率：**
```promql
system_cpu_usage
```

**缓存命中率：**
```promql
sum(rate(ojp_cache_decision_total{result="hit"}[5m])) / sum(rate(ojp_cache_decision_total[5m]))
```

**JVM 堆内存：**
```promql
sum(jvm_memory_used_bytes{area="heap"})
```

### 时间范围配置

MonitorLayout 支持以下时间范围：
- 1h (1小时)
- 6h (6小时)
- 12h (12小时)
- 24h (24小时)
- 7d (7天)

## 🚀 使用指南

### 访问原生监控

1. 点击左侧菜单 "原生监控"
2. 选择要查看的服务
3. 使用时间范围选择器调整查看时间
4. 点击刷新按钮手动更新数据

### 访问 Grafana 监控（备选）

1. 点击左侧菜单 "Grafana 监控"
2. 选择 "监控总览" 或具体服务
3. 或在监控仪表板底部点击 "访问 Grafana"

## 🎯 优势对比

### 原生监控 vs Grafana iframe

| 特性 | 原生监控 | Grafana iframe |
|------|---------|---------------|
| 加载速度 | ⚡ 快速 | 🐌 较慢 |
| 响应式设计 | ✅ 完美支持 | ⚠️ 有限支持 |
| 自定义样式 | ✅ 完全可控 | ❌ 受限 |
| 交互体验 | ✅ 流畅 | ⚠️ 一般 |
| 维护成本 | ✅ 低 | ⚠️ 中等 |
| 数据实时性 | ✅ 实时 | ✅ 实时 |

## 🔄 后续优化建议

1. **增强功能**
   - [ ] 添加告警阈值配置
   - [ ] 支持自定义时间范围
   - [ ] 添加数据导出功能
   - [ ] 支持多指标对比

2. **性能优化**
   - [ ] 实现虚拟滚动（大量数据点）
   - [ ] 添加数据采样策略
   - [ ] 优化图表渲染性能

3. **用户体验**
   - [ ] 添加快捷键支持
   - [ ] 支持自定义仪表板布局
   - [ ] 添加深色模式
   - [ ] 支持全屏模式

## 📝 注意事项

1. **Grafana 保留**：Grafana 监控仍然保留，可作为备选方案或用于高级分析
2. **Prometheus 依赖**：所有原生监控页面依赖 Prometheus 服务正常运行
3. **浏览器兼容性**：建议使用现代浏览器（Chrome, Firefox, Edge）

## 🐛 已知问题

目前没有已知问题。

## 📞 支持

如有问题或建议，请联系开发团队。

---

**迁移完成日期**: 2025-12-19  
**版本**: v1.0.0
