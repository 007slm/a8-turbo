# OJP Server Management Console

这是一个基于 React + Ant Design 的 OJP Server 管理控制台前端应用。

## 功能特性

- 🚀 **现代化 UI**: 基于 Ant Design 5.x 的现代化用户界面
- 📊 **实时监控**: 实时显示系统状态、资源使用情况和性能指标
- 💾 **智能缓存管理**: 
  - 缓存规则管理（表格规则、查询规则、正则规则）
  - 查询缓存监控和优化
  - 表格缓存策略配置
  - 实时性能统计和命中率分析
  - 慢查询识别和优化建议
- 📈 **系统监控**: 基于 Spring Boot Actuator 的实时系统监控
- 📚 **API 文档**: 查看和测试系统 API 接口
- 📝 **系统日志**: 查看和管理系统运行日志
- 🧪 **系统测试**: 通过 UI 界面测试服务器功能和连接状态

## 技术栈

- **前端框架**: React 18
- **UI 组件库**: Ant Design 5.x
- **状态管理**: React Query (TanStack Query)
- **构建工具**: Vite 5
- **样式**: CSS3 + Ant Design 主题系统
- **图标**: Ant Design Icons

## 快速开始

### 环境要求

- Node.js >= 16.0.0
- npm >= 8.0.0 或 yarn >= 1.22.0

### 安装依赖

```bash
cd ojp/ojp-ui
npm install
```

### 启动开发服务器

```bash
npm run dev
```

应用将在 `http://localhost:5173` 启动。

### 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist` 目录。

## 项目结构

```
src/
├── components/          # 页面组件
│   ├── Dashboard.jsx   # 仪表盘
│   ├── CacheManagement.jsx   # 缓存管理主组件
│   ├── cache/          # 缓存管理子组件
│   │   ├── CacheRules.jsx    # 缓存规则管理
│   │   ├── QueryCache.jsx    # 查询缓存管理
│   │   ├── TableCache.jsx    # 表格缓存管理
│   │   └── CacheStats.jsx    # 缓存性能统计
│   ├── Monitoring.jsx        # 系统监控
│   ├── Testing.jsx           # 系统测试
│   ├── ApiDocs.jsx           # API 文档
│   ├── Logs.jsx              # 系统日志
│   └── SqlStatistics.jsx     # SQL统计

├── services/           # API 服务
│   └── api.js         # API 接口定义
├── App.jsx            # 主应用组件
├── App.css            # 应用样式
├── main.jsx           # 应用入口
└── index.css          # 全局样式
```

## 配置说明

### 后端 API 配置

在 `src/services/api.js` 中配置后端 API 地址：

```javascript
const API_BASE_URL = '/api'  // 开发环境代理到后端
```

在 `vite.config.js` 中配置开发环境代理：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8010',  // 后端服务地址
      changeOrigin: true,
    },
    '/actuator': {
      target: 'http://localhost:8010',  // Spring Boot Actuator 端点
      changeOrigin: true,
    }
  }
}
```

### 环境变量

创建 `.env` 文件来配置环境变量：

```env
VITE_API_BASE_URL=http://localhost:8010
VITE_APP_TITLE=OJP Server Management Console
```

## 开发指南

### 添加新页面

1. 在 `src/components/` 目录下创建新的组件文件
2. 在 `src/App.jsx` 中添加路由配置
3. 在侧边栏菜单中添加对应的菜单项

### 添加新的 API 接口

1. 在 `src/services/api.js` 中添加新的 API 函数
2. 在组件中使用 `useQuery` 或 `useMutation` 调用 API

### 样式定制

- 全局样式在 `src/index.css` 中定义
- 组件专用样式在 `src/App.css` 中定义
- 使用 Ant Design 的主题系统进行样式定制

## 部署说明

### 开发环境

```bash
npm run dev
```

### 生产环境

```bash
npm run build
npm run preview
```

### Docker 部署

```dockerfile
FROM node:18-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 浏览器支持

- Chrome >= 88
- Firefox >= 85
- Safari >= 14
- Edge >= 88

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件
- 项目讨论区

## 更新日志

### v0.1.0 (2024-01-XX)
- 新增系统测试功能
  - gRPC 连接测试
  - gRPC 健康检查
  - OpenTelemetry 连接测试
  - 数据库连接测试
  - 缓存功能测试
  - 性能测试
  - 测试历史管理和统计
  - 可配置的测试参数
- 添加测试相关的 API 接口
- 提供完整的测试 UI 界面

### v0.0.0 (2024-01-XX)
- 初始版本发布
- 基础框架搭建
- 仪表盘和服务器管理功能
- 智能缓存管理系统
  - 缓存规则管理（支持多种规则类型）
  - 查询缓存监控和优化
  - 表格缓存策略配置
  - 实时性能统计和命中率分析
  - 慢查询识别和优化建议
- 响应式设计支持
- 完整的后端 API 接口规范文档
