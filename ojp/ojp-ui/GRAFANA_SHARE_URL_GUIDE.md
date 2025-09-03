# Grafana 分享URL功能使用指南

## 概述

本系统已集成Grafana分享URL功能，允许用户生成可公开访问的监控面板链接，无需登录即可查看监控数据。

## 功能特点

### 1. 匿名访问支持
- Grafana已配置匿名访问（`GF_AUTH_ANONYMOUS_ENABLED=true`）
- 生成的分享URL可直接访问，无需登录
- 适合团队协作和外部分享

### 2. 优化的嵌入体验
- 使用`kiosk=tv`参数提供全屏显示
- 隐藏Grafana导航栏，专注于监控数据
- 适合嵌入到其他系统或大屏显示

### 3. 灵活的时间范围
- 默认显示最近1小时数据（`from=now-1h&to=now`）
- 30秒自动刷新（`refresh=30s`）
- 可根据需要调整时间参数

## 使用方法

### 在监控总览页面
1. 访问 `/monitoring` 页面
2. 点击右上角的"获取分享链接"按钮
3. 在弹出的模态框中点击"生成分享URL"
4. 复制生成的URL或直接打开预览

### 在单个服务监控页面
1. 访问任意服务监控页面（如 `/monitoring/mysql`）
2. 点击右上角的"分享"按钮
3. 在弹出的模态框中生成和使用分享URL

## URL参数说明

生成的分享URL包含以下参数：

```
http://localhost:3000/d/{dashboard-uid}-overview?orgId=1&from=now-1h&to=now&timezone=browser&refresh=30s&theme=light&kiosk=tv
```

- `orgId=1` - Grafana组织ID
- `from=now-1h&to=now` - 时间范围（最近1小时）
- `timezone=browser` - 使用浏览器时区
- `refresh=30s` - 30秒自动刷新
- `theme=light` - 浅色主题
- `kiosk=tv` - 全屏模式，隐藏导航栏

## 技术实现

### 1. Docker配置
在 `docker-compose.yml` 中已配置：
```yaml
services:
  grafana:
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_USERS_ALLOW_ORG_CREATE=false
```

### 2. 前端组件
- `GrafanaShareUrl.jsx` - 分享URL生成和管理组件
- `MonitoringOverview.jsx` - 监控总览页面，包含分享功能
- `ServiceMonitoring.jsx` - 单个服务监控页面，包含分享功能

### 3. URL构建逻辑
```javascript
const grafanaBaseUrl = '/grafana';
const timeRange = 'from=now-1h&to=now';
const dashboardUid = service?.key || 'a8-turbo';
const publicUrl = `${grafanaBaseUrl}/d/${dashboardUid}-overview?orgId=1&${timeRange}&timezone=browser&refresh=30s&theme=light&kiosk=tv`;
```

## 使用场景

### 1. 团队协作
- 将监控链接分享给团队成员
- 无需为每个成员创建Grafana账户
- 快速共享特定时间段的监控数据

### 2. 系统集成
- 嵌入到其他管理系统
- 集成到运维平台
- 添加到文档或Wiki页面

### 3. 大屏显示
- 运维大屏展示
- 会议室监控显示
- 24/7监控中心

## 安全考虑

### 1. 网络访问
- 当前配置仅限本地访问（localhost:3000）
- 生产环境建议配置HTTPS
- 考虑网络防火墙和访问控制

### 2. 数据敏感性
- 匿名访问意味着任何人都可以查看监控数据
- 确保不包含敏感信息
- 考虑使用Grafana的权限管理功能

### 3. 生产环境建议
- 配置HTTPS加密传输
- 设置适当的网络访问策略
- 定期审查匿名访问权限
- 考虑使用Grafana的组织和用户管理

## 故障排除

### 1. 无法访问分享URL
- 检查Grafana服务是否运行（localhost:3000）
- 确认匿名访问已启用
- 检查防火墙设置

### 2. 面板显示异常
- 确认dashboard UID正确
- 检查数据源配置
- 验证时间范围参数

### 3. 嵌入显示问题
- 检查iframe安全策略
- 确认kiosk参数生效
- 验证浏览器兼容性

## 扩展功能

### 1. 自定义时间范围
可以修改URL中的时间参数：
- `from=now-6h&to=now` - 最近6小时
- `from=now-1d&to=now` - 最近1天
- `from=now-7d&to=now` - 最近7天

### 2. 特定面板
如需分享特定面板，可添加panelId参数：
```
&panelId=2
```

### 3. 主题切换
支持切换主题：
- `theme=light` - 浅色主题
- `theme=dark` - 深色主题

## 更新日志

- **v1.0** - 初始版本，支持基本分享URL功能
- **v1.1** - 添加kiosk模式，优化嵌入体验
- **v1.2** - 集成到监控总览和服务监控页面