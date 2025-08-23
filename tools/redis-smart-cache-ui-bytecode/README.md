# Redis Smart Cache UI

Redis Smart Cache 的现代化 Web 管理界面，基于 React + Vite + Ant Design 构建。

## ✨ 功能特性

### 🎯 核心功能
- **仪表板概览** - 系统状态、性能指标、热门查询/表格一览
- **查询管理** - 查看、搜索、排序SQL查询，一键创建缓存规则
- **表格管理** - 数据表访问统计，智能缓存建议
- **规则管理** - 完整的CRUD操作，支持多种匹配规则类型
- **Redis连接** - 可视化连接配置与状态监控

### 🚀 技术特色
- **响应式设计** - 完美适配桌面和移动设备
- **实时数据** - 使用 React Query 实现数据缓存和自动刷新
- **智能推荐** - 基于访问频率和查询耗时的缓存策略建议
- **友好交互** - 丰富的操作反馈和状态提示
- **类型安全** - 完整的 TypeScript 类型定义

## 📦 技术栈

- **框架**: React 18 + Vite 4
- **UI库**: Ant Design 5
- **状态管理**: React Query 3
- **HTTP客户端**: Axios
- **样式**: CSS + Ant Design
- **开发工具**: ESLint + Vite

## 🛠️ 开发环境

### 前提条件
- Node.js 16+ 
- npm 或 yarn 或 pnpm

### 安装依赖
```bash
npm install
# 或
yarn install
# 或
pnpm install
```

### 启动开发服务器
```bash
npm run dev
# 或
yarn dev
# 或
pnpm dev
```

访问 http://localhost:3000 查看应用

### 构建生产版本
```bash
npm run build
# 或
yarn build
# 或
pnpm build
```

### 预览生产构建
```bash
npm run preview
# 或
yarn preview
# 或
pnpm preview
```

## 🏗️ 项目结构

```
src/
├── components/          # React 组件
│   ├── Dashboard.jsx    # 仪表板
│   ├── QueryManagement.jsx    # 查询管理
│   ├── TableManagement.jsx    # 表格管理
│   ├── RuleManagement.jsx     # 规则管理
│   └── RedisConnection.jsx    # Redis连接配置
├── hooks/              # 自定义 Hooks
│   └── useData.js      # 数据管理 Hooks
├── api/                # API 服务层
│   ├── client.js       # HTTP 客户端配置
│   └── index.js        # API 服务定义
├── types/              # 类型定义
│   └── index.js        # 数据类型
├── App.jsx             # 主应用组件
├── main.jsx            # 应用入口
└── index.css           # 全局样式
```

## 🔧 配置说明

### API 端点配置
默认情况下，应用会代理 API 请求到 `/api` 路径。在生产环境中，你需要配置反向代理或更新 `src/api/client.js` 中的 `baseURL`。

### 模拟数据
开发阶段使用模拟数据，在 `src/hooks/useData.js` 中设置：
```javascript
const USE_MOCK_DATA = true; // 设为 false 使用真实 API
```

## 📱 界面预览

### 仪表板
- 系统概览统计
- 缓存性能指标
- 热门查询和表格

### 查询管理
- 查询列表展示
- SQL 语句预览
- 一键创建缓存规则
- 详细信息查看

### 表格管理
- 表格访问统计
- 性能分析
- 智能缓存建议
- 批量规则创建

### 规则管理
- 规则 CRUD 操作
- 多种匹配类型支持
- 批量提交功能
- 规则优先级说明

### Redis 连接
- 连接状态监控
- 可视化配置界面
- 连接测试功能
- 配置保存/加载

## 🔌 与后端集成

该 UI 需要配合 Redis Smart Cache 后端 API 使用。API 接口包括：

- `GET /api/redis/status` - 获取 Redis 连接状态
- `GET /api/queries` - 获取查询列表
- `GET /api/tables` - 获取表格列表
- `GET /api/rules` - 获取规则列表
- `POST /api/rules` - 创建规则
- `PUT /api/rules/:id` - 更新规则
- `DELETE /api/rules/:id` - 删除规则
- `POST /api/rules/commit` - 提交规则更改

详细的 API 文档请参考后端项目说明。

## 🎨 自定义主题

可以通过修改 `src/main.jsx` 中的 ConfigProvider 来自定义 Ant Design 主题：

```jsx
<ConfigProvider
  locale={zhCN}
  theme={{
    token: {
      colorPrimary: '#1890ff', // 主色调
      // 其他主题配置...
    },
  }}
>
  <App />
</ConfigProvider>
```

## 🤝 贡献指南

1. Fork 该项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

该项目使用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 🙏 致谢

- [Ant Design](https://ant.design/) - 优秀的 React UI 组件库
- [React Query](https://react-query.tanstack.com/) - 强大的数据获取库
- [Vite](https://vitejs.dev/) - 极速的前端构建工具