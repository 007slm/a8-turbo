# Redis Smart Cache UI 完整系统

基于Spring Boot + React的Redis Smart Cache管理系统，提供完整的Web UI界面来管理缓存规则、监控查询性能。

## 系统架构

```
┌─────────────────────┐    HTTP API    ┌──────────────────────┐    Redis Protocol    ┌─────────────┐
│                     │ <------------- │                      │ <------------------- │             │
│   React Frontend    │                │  Spring Boot Backend │                      │    Redis    │
│   (Port: 5173)      │ -------------> │    (Port: 8080)      │ -------------------> │  (Port: 6379) │
│                     │                │                      │                      │             │
└─────────────────────┘                └──────────────────────┘                      └─────────────┘
```

## 快速开始

### 前置条件

1. **JDK 17+** - 运行Spring Boot后端
2. **Node.js 16+** - 运行React前端  
3. **Redis Server** - 数据存储和缓存

### 一键启动

```bash
# 在 e:\redis-smart-cache\tools\ 目录下运行
start-full-stack.bat
```

这将自动启动：
- 后端服务器 (http://localhost:8080)
- 前端界面 (http://localhost:5173)

### 手动启动

#### 1. 启动Redis (如果未启动)
```bash
redis-server
```

#### 2. 启动后端API服务
```bash
cd e:\redis-smart-cache\tools\redis-smart-cache-client
gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

#### 3. 启动前端界面
```bash
cd e:\redis-smart-cache\tools\redis-smart-cache-ui-bytecode
npm run dev
```

#### 4. 访问系统
打开浏览器访问: http://localhost:5173

## 系统功能

### 1. 仪表板 (Dashboard)
- 查看整体缓存统计
- 监控查询性能指标
- 查看缓存命中率
- 显示热门表格和慢查询

### 2. 查询管理 (Query Management)
- 浏览所有数据库查询
- 查看查询执行统计
- 为特定查询创建缓存规则
- 搜索和过滤查询

### 3. 表格管理 (Table Management)  
- 查看数据表访问统计
- 监控表格查询性能
- 为表格创建缓存规则
- 查看缓存覆盖率

### 4. 规则管理 (Rule Management)
- 创建和编辑缓存规则
- 支持多种规则类型：
  - 表格匹配 (Tables)
  - 表格任意匹配 (Tables Any)
  - 表格全部匹配 (Tables All)
  - 查询ID匹配 (Query IDs)
  - 正则表达式 (Regex)
- 批量提交规则更改

### 5. Redis连接管理
- 监控Redis连接状态
- 测试连接配置
- 查看连接信息

## API接口

### 后端API (Spring Boot - Port 8080)

#### 统计接口
```
GET /api/stats/overview        # 获取统计概览
GET /api/stats                 # 获取详细统计
```

#### 查询管理
```
GET /api/queries              # 获取查询列表
GET /api/queries/{id}         # 获取查询详情
```

#### 表格管理
```
GET /api/tables               # 获取表格列表
GET /api/tables/{name}        # 获取表格详情
```

#### 规则管理
```
GET /api/rules                # 获取规则列表
POST /api/rules               # 创建规则
POST /api/rules/commit        # 提交规则更改
POST /api/rules/query/{id}    # 为查询创建规则
POST /api/rules/table/{name}  # 为表格创建规则
```

#### 连接管理
```
GET /api/connection/status    # 获取连接状态
POST /api/connection/test     # 测试连接
```

## 配置

### 后端配置 (application.yml)
```yaml
smartcache:
  redis:
    host: localhost
    port: 6379
    application-name: smartcache
    username: 
    password: 
```

### 前端配置
- API地址在 `src/api/client.js` 中配置
- 默认指向 `http://localhost:8080/api`

## 开发指南

### 后端开发 (Spring Boot)
```bash
cd redis-smart-cache-client
./gradlew build          # 构建
./gradlew test           # 测试
./gradlew bootRun        # 运行
```

### 前端开发 (React + Vite)
```bash
cd redis-smart-cache-ui-bytecode
npm install              # 安装依赖
npm run dev              # 开发服务器
npm run build            # 构建生产版本
npm run preview          # 预览生产版本
```

## 部署

### Docker部署 (推荐)
```bash
# 构建后端镜像
cd redis-smart-cache-client
docker build -t redis-smart-cache-client .

# 构建前端镜像
cd redis-smart-cache-ui-bytecode
docker build -t redis-smart-cache-ui .

# 运行完整系统
docker-compose up
```

### 生产环境配置
```yaml
# docker-compose.yml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  backend:
    image: redis-smart-cache-client
    ports:
      - "8080:8080"
    environment:
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    depends_on:
      - redis
  
  frontend:
    image: redis-smart-cache-ui
    ports:
      - "80:80"
    depends_on:
      - backend
```

## 故障排除

### 常见问题

1. **后端启动失败**
   - 检查JDK版本 (需要17+)
   - 确认Redis服务已启动
   - 检查端口8080是否被占用

2. **前端连接后端失败**
   - 确认后端服务在8080端口运行
   - 检查API地址配置
   - 查看浏览器控制台错误信息

3. **Redis连接失败**
   - 确认Redis服务启动
   - 检查Redis配置参数
   - 验证网络连接

### 日志查看

#### 后端日志
```bash
# 查看Spring Boot应用日志
tail -f logs/application.log
```

#### 前端日志
- 打开浏览器开发者工具
- 查看Console标签页
- 查看Network标签页的API请求

## 性能优化

### 前端优化
- 启用React Query缓存
- 使用虚拟滚动处理大数据量
- 优化组件渲染性能

### 后端优化
- 配置连接池
- 启用Redis集群
- 优化查询逻辑

## 安全建议

1. **生产环境配置**
   - 配置Redis认证
   - 启用HTTPS
   - 设置防火墙规则

2. **访问控制**
   - 限制API访问IP
   - 添加认证中间件
   - 启用审计日志

## 技术支持

如有问题或建议，请联系：
- 项目文档：查看项目README
- 问题报告：提交GitHub Issue
- 技术讨论：加入项目讨论组

---

**Redis Smart Cache UI** - 让缓存管理更简单！