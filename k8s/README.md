# A8-Turbo Kubernetes 部署指南

## 概述

本项目将 A8-Turbo 系统从 Docker Compose 迁移到 Kubernetes，提供了完整的微服务架构部署方案。

## 架构组件

### 基础设施层
- **Redis**: 缓存服务
- **Prometheus**: 监控数据收集
- **Grafana**: 监控仪表盘
- **Exporters**: MySQL、Redis、NATS 监控导出器

### 数据层
- **MySQL**: 主数据库
- **StarRocks**: 分析数据库

### 消息层
- **NATS**: 消息队列和流处理
- **NATS Dashboard**: 消息系统监控

### 网关层
- **Kong**: API 网关和路由

### 流处理层
- **Flink**: 实时数据处理
  - JobManager: 作业管理
  - TaskManager: 任务执行
  - CDC Init Job: 数据变更捕获初始化

### 应用层
- **ojp-server**: 后端核心服务
- **shopservice**: 商店服务
- **ojp-ui**: 前端界面

## 目录结构

```
k8s/
├── namespace.yaml              # 命名空间定义
├── deploy-all.yaml            # 一键部署脚本
├── README.md                  # 部署说明
├── storage/
│   └── persistent-volumes.yaml # 持久化存储
├── configmaps/                # 配置映射
│   ├── mysql-config.yaml
│   ├── prometheus-config.yaml
│   ├── grafana-config.yaml
│   ├── grafana-dashboards.yaml
│   ├── kong-config.yaml
│   ├── nats-config.yaml
│   ├── flink-config.yaml
│   └── ojp-config.yaml
├── infrastructure/            # 基础设施
│   ├── redis.yaml
│   ├── prometheus.yaml
│   ├── grafana.yaml
│   └── exporters.yaml
├── data/                      # 数据组件
│   ├── mysql.yaml
│   └── starrocks.yaml
├── messaging/                 # 消息组件
│   ├── nats.yaml
│   └── nats-dashboard.yaml
├── gateway/                   # 网关
│   └── kong.yaml
├── streaming/                 # 流处理
│   └── flink.yaml
└── applications/              # 业务应用
    ├── ojp-server.yaml
    ├── shopservice.yaml
    └── ojp-ui.yaml
```

## 部署步骤

### 1. 前置条件
- Kubernetes 集群 (v1.20+)
- kubectl 命令行工具
- 足够的集群资源 (建议 8GB+ 内存)

### 2. 一键部署
```bash
cd k8s
kubectl apply -f deploy-all.yaml
```

### 3. 分步部署
如需分步部署，按以下顺序执行：

```bash
# 1. 创建命名空间
kubectl apply -f namespace.yaml

# 2. 创建存储
kubectl apply -f storage/

# 3. 创建配置
kubectl apply -f configmaps/

# 4. 部署基础设施
kubectl apply -f infrastructure/

# 5. 部署数据组件
kubectl apply -f data/

# 6. 部署消息组件
kubectl apply -f messaging/

# 7. 部署网关
kubectl apply -f gateway/

# 8. 部署流处理
kubectl apply -f streaming/

# 9. 部署应用
kubectl apply -f applications/
```

## 验证部署

### 检查 Pod 状态
```bash
kubectl get pods -n a8-turbo
```

### 检查服务状态
```bash
kubectl get services -n a8-turbo
```

### 查看日志
```bash
kubectl logs -f deployment/ojp-server -n a8-turbo
```

## 访问地址

### 外部访问 (NodePort)
- Kong 网关: `http://localhost:30800`
- Kong 管理: `http://localhost:30801`

### 集群内访问
- Grafana: `http://grafana.a8-turbo:3000`
- Prometheus: `http://prometheus.a8-turbo:9090`
- Flink WebUI: `http://flink-jobmanager.a8-turbo:8081`
- OJP Server: `http://ojp-server.a8-turbo:8080`
- Shop Service: `http://shopservice.a8-turbo:8081`
- OJP UI: `http://ojp-ui.a8-turbo:80`

## 配置说明

### 资源限制
所有组件都配置了合理的资源请求和限制：
- 请求资源确保基本运行
- 限制资源防止资源争抢

### 健康检查
所有服务都配置了：
- Liveness Probe: 检测服务是否存活
- Readiness Probe: 检测服务是否就绪

### 持久化存储
关键数据组件使用 PVC 持久化：
- MySQL 数据
- NATS JetStream 数据
- Grafana 配置和仪表盘
- Prometheus 监控数据
- StarRocks 数据
- Redis 数据

### 服务发现
使用 Kubernetes DNS 进行服务发现：
- 格式: `<service-name>.<namespace>`
- 例如: `mysql.a8-turbo`

## 故障排除

### 常见问题

1. **Pod 启动失败**
   ```bash
   kubectl describe pod <pod-name> -n a8-turbo
   ```

2. **服务无法访问**
   ```bash
   kubectl get endpoints -n a8-turbo
   ```

3. **配置问题**
   ```bash
   kubectl get configmap -n a8-turbo
   kubectl describe configmap <configmap-name> -n a8-turbo
   ```

### 重新部署
```bash
kubectl delete namespace a8-turbo
kubectl apply -f deploy-all.yaml
```

## 扩展和维护

### 扩容服务
```bash
kubectl scale deployment ojp-server --replicas=3 -n a8-turbo
```

### 更新镜像
```bash
kubectl set image deployment/ojp-server ojp-server=ojp-server:v2.0 -n a8-turbo
```

### 备份配置
```bash
kubectl get all -n a8-turbo -o yaml > backup.yaml
```

## 监控和日志

- **Grafana**: 提供系统监控仪表盘
- **Prometheus**: 收集和存储监控指标
- **导出器**: 为各组件提供监控数据
- **Kubernetes 日志**: 使用 `kubectl logs` 查看应用日志