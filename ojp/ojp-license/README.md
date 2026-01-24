# OJP Sentinel

OJP Sentinel 是 Open JDBC Proxy 的 eBPF 授权哨兵模块，基于 Linux eBPF 技术和 Go 语言实现内核级别的流量控制和授权验证。

## 📋 目录

- [功能特性](#功能特性)
- [架构设计](#架构设计)
- [工作原理](#工作原理)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [部署方式](#部署方式)
- [故障排查](#故障排查)
- [安全分析](#安全分析)

## ✨ 功能特性

### 核心功能

- **内核级流量控制**: 使用 eBPF 在 Linux 内核层面拦截和控制网络流量
- **RSA 签名验证**: 基于非对称加密的客户端授权验证
- **延迟惩罚机制**: 对未授权访问实施网络延迟惩罚（5秒延迟）
- **实时授权检查**: 持续监控授权状态，动态调整流量控制策略

### 技术特性

- **eBPF 技术栈**:
  - **cilium/ebpf**: Go 语言 eBPF 操作库
  - **C 语言字节码**: 高性能内核程序
  - **BPF Map**: 用户态与内核态数据共享

- **安全特性**:
  - **TC (Traffic Control)**: Linux 流量控制子系统
  - **netem**: 网络模拟工具，实现延迟控制
  - **权限隔离**: 需要 privileged 容器权限

- **部署特性**:
  - **Docker 特权模式**: 必需的系统权限
  - **主机网络模式**: 直接操作宿主机网络栈
  - **自动网卡发现**: 智能识别 OJP 相关容器网卡

## 🏗️ 架构设计

### 组件架构

```
┌─────────────────────────────────────────────────────────┐
│                 OJP Sentinel                            │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │   Go Manager │   License    │   TC Controller     │  │
│  │   (用户态)   │   Verifier   │   (netem)           │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼────────┐┌─▼──────────┐┌─▼──────────────┐
│   eBPF Program ││  BPF Map    ││  TC netem     │
│   (内核态 C)   ││  (共享内存) ││  (延迟队列)   │
└────────────────┘└─────────────┘└────────────────┘
```

### 核心组件

- **Go Manager**: 用户态主程序，负责授权验证和 eBPF 管理
- **eBPF Program**: 内核态 C 程序，实现流量拦截逻辑
- **BPF Map**: 用户态与内核态共享内存，用于状态同步
- **TC netem**: Linux 流量控制工具，实现网络延迟

## 🔍 工作原理

### 双层防护机制

#### 第一层：eBPF 程序（内核态）
- **位置**: TC ingress hook 点
- **功能**: 读取 BPF Map 中的授权状态
- **作用**: 快速检查授权状态，决定是否放行数据包

#### 第二层：TC netem（内核态配置）
- **工具**: Linux Traffic Control netem
- **功能**: 对网卡应用延迟规则
- **效果**: 未授权时所有数据包延迟 5 秒

### 数据流向

```
客户端请求 → 物理网卡 → TC ingress → eBPF 检查 → 允许/延迟 → OJP Server
                              ↓
                        BPF Map (授权状态)
                              ↓
                    License 文件验证
```

### 授权验证流程

1. **文件监控**: 每 5 秒检查 license.key 文件
2. **RSA 验证**: 使用公钥验证签名
3. **状态更新**: 更新 BPF Map 中的授权状态
4. **TC 配置**: 根据授权状态配置网络延迟

## 🚀 快速开始

### 环境要求

- **Linux 内核**: 5.4+ (支持 eBPF)
- **Go**: 1.21+
- **Docker**: 20.10+ (特权模式)
- **clang**: 11+ (eBPF 编译)

### 构建要求

```bash
# 安装 eBPF 编译工具
apt-get update && apt-get install -y clang llvm libbpf-dev

# 安装 Go
# 下载并安装 Go 1.21+
```

### 构建项目

```bash
cd ojp/ojp-sentinel

# 生成 eBPF 字节码
go generate ./...

# 构建 Go 程序
go build -o sentinel ./cmd/sentinel
```

### 运行测试

```bash
# 运行 sentinel
./sentinel

# 检查 eBPF 程序状态
bpftool prog list | grep sentinel

# 检查 TC 规则
tc qdisc show dev eth0
```

## ⚙️ 配置说明

### 环境变量

| 变量名 | 描述 | 默认值 | 必需 |
|--------|------|--------|------|
| `LICENSE_FILE_PATH` | License 文件路径 | `/app/license.key` | 是 |
| `PUBLIC_KEY_PATH` | RSA 公钥文件路径 | `/app/public.pem` | 是 |
| `CHECK_INTERVAL` | 授权检查间隔(秒) | `5` | 否 |
| `PENALTY_DELAY_MS` | 惩罚延迟时间(毫秒) | `5000` | 否 |
| `LOG_LEVEL` | 日志级别 | `INFO` | 否 |

### License 文件格式

```
-----BEGIN LICENSE-----
授权数据 (JSON 格式，包含过期时间、客户端信息等)
-----END LICENSE-----

-----BEGIN SIGNATURE-----
RSA 签名 (Base64 编码)
-----END SIGNATURE-----
```

### Docker 配置

```yaml
version: '3.8'
services:
  ojp-sentinel:
    build: ./ojp-sentinel
    privileged: true          # 必需：eBPF 和 TC 权限
    network_mode: host        # 必需：操作宿主机网络
    volumes:
      - /lib/modules:/lib/modules:ro    # 内核模块
      - /sys/kernel/debug:/sys/kernel/debug:rw  # eBPF 调试
      - ./license.key:/app/license.key:ro      # License 文件
      - ./public.pem:/app/public.pem:ro        # 公钥文件
    environment:
      - LICENSE_FILE_PATH=/app/license.key
      - PUBLIC_KEY_PATH=/app/public.pem
      - CHECK_INTERVAL=5
      - PENALTY_DELAY_MS=5000
    restart: always
```

## 🚢 部署方式

### 生产部署

```yaml
# docker-compose.yml
version: '3.8'
services:
  ojp-sentinel:
    image: ojp-sentinel:latest
    privileged: true
    network_mode: host
    volumes:
      - /lib/modules:/lib/modules:ro
      - /sys/kernel/debug:/sys/kernel/debug:rw
      - /path/to/license:/app/license.key:ro
      - /path/to/public-key:/app/public.pem:ro
    environment:
      - LICENSE_FILE_PATH=/app/license.key
      - PUBLIC_KEY_PATH=/app/public.pem
    healthcheck:
      test: ["CMD", "./sentinel", "--health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### Kubernetes 部署

```yaml
apiVersion: apps/v1
kind: DaemonSet  # 使用 DaemonSet 确保每个节点运行一个实例
metadata:
  name: ojp-sentinel
spec:
  selector:
    matchLabels:
      app: ojp-sentinel
  template:
    metadata:
      labels:
        app: ojp-sentinel
    spec:
      hostNetwork: true  # 必需：访问宿主机网络
      securityContext:
        privileged: true  # 必需：eBPF 权限
      containers:
      - name: sentinel
        image: ojp-sentinel:latest
        volumeMounts:
        - name: modules
          mountPath: /lib/modules
          readOnly: true
        - name: debug
          mountPath: /sys/kernel/debug
        - name: license
          mountPath: /app/license.key
          readOnly: true
        - name: public-key
          mountPath: /app/public.pem
          readOnly: true
        env:
        - name: LICENSE_FILE_PATH
          value: "/app/license.key"
        - name: PUBLIC_KEY_PATH
          value: "/app/public.pem"
      volumes:
      - name: modules
        hostPath:
          path: /lib/modules
      - name: debug
        hostPath:
          path: /sys/kernel/debug
      - name: license
        secret:
          secretName: ojp-license
      - name: public-key
        configMap:
          name: ojp-public-key
```

## 🔧 故障排查

### 常见问题

#### 1. eBPF 加载失败

**可能原因**:
- 内核版本不支持 eBPF
- 权限不足
- clang 编译失败

**检查方法**:
```bash
# 检查内核版本
uname -r

# 检查 eBPF 支持
cat /sys/kernel/debug/tracing/available_filter_functions | head -10

# 检查权限
capsh --print | grep cap_bpf

# 查看内核日志
dmesg | grep bpf
```

#### 2. TC 命令失败

**可能原因**:
- 网卡名称错误
- iproute2 未安装
- 权限不足

**检查方法**:
```bash
# 检查网卡
ip link show

# 检查 iproute2
which tc
tc --version

# 检查 TC 规则
tc qdisc show
```

#### 3. 授权验证失败

**可能原因**:
- License 文件不存在
- 公钥文件错误
- 签名验证失败

**检查方法**:
```bash
# 检查文件存在
ls -la /app/license.key /app/public.pem

# 检查文件内容
head -5 /app/license.key
head -5 /app/public.pem

# 查看日志
docker logs ojp-sentinel | grep -i license
```

### 日志分析

```bash
# 查看应用日志
docker logs ojp-sentinel

# 查看内核日志
dmesg | grep -E "(bpf|tc|netem)"

# 查看 eBPF 程序
bpftool prog show
bpftool map show
```

### 调试模式

```bash
# 启用详细日志
export LOG_LEVEL=DEBUG

# 运行调试版本
./sentinel --debug --verbose

# 检查 BPF Map 内容
bpftool map dump id <map_id>
```

## 🔒 安全分析

### 安全性优势

#### 内核级防护
- **深度防御**: 在网络层拦截，未授权流量无法到达应用层
- **性能高效**: eBPF 程序执行时间 < 1μs
- **难以绕过**: 需要 root 权限才能修改内核规则

#### 双重验证
- **文件验证**: RSA 签名确保 License 文件完整性
- **实时监控**: 持续检查授权状态，及时响应变化
- **降级保护**: 授权失效时自动启用惩罚机制

### 潜在风险

#### 权限要求
- 需要 `privileged: true`，存在安全风险
- 建议在隔离环境中运行

#### 内核依赖
- 依赖特定内核版本和配置
- eBPF 功能可能被安全策略禁用

### 最佳实践

1. **最小权限原则**: 只在必要容器中启用 privileged 模式
2. **网络隔离**: 使用专用网络命名空间
3. **监控审计**: 记录所有授权检查和策略变更
4. **定期更新**: 及时更新内核和 eBPF 程序

## 📊 性能影响

### 资源消耗

- **CPU**: eBPF 程序 < 1μs 执行时间，几乎无额外开销
- **内存**: ~1MB BPF Map 和程序存储
- **网络**: 授权状态下无延迟，未授权时固定 5 秒延迟

### 监控指标

```bash
# eBPF 程序统计
bpftool prog show | grep sentinel

# TC 队列统计
tc -s qdisc show dev eth0

# 网络延迟统计
ping -c 5 localhost  # 检查延迟是否生效
```

## 🤝 开发指南

### 项目结构

```
ojp-sentinel/
├── cmd/sentinel/          # 主程序入口
├── internal/
│   ├── ebpf/             # eBPF 相关代码
│   │   ├── sentinel.bpf.c    # eBPF C 程序
│   │   └── sentinel.go       # Go eBPF 封装
│   ├── license/          # 授权验证
│   ├── tc/               # TC 控制器
│   └── config/           # 配置管理
├── pkg/
│   └── utils/            # 工具函数
├── Dockerfile            # 容器构建
├── go.mod               # Go 模块
└── Makefile             # 构建脚本
```

### 添加新功能

1. **扩展 eBPF 程序**:
```c
// sentinel.bpf.c
SEC("tc")
int sentinel_ingress(struct __sk_buff *skb) {
    // 添加新的流量检查逻辑
    return TC_ACT_OK;
}
```

2. **扩展 Go 控制器**:
```go
// internal/tc/controller.go
func applyCustomRule(interface string, rule CustomRule) error {
    // 实现自定义 TC 规则
}
```

### 测试开发

```bash
# 运行单元测试
go test ./...

# 运行集成测试
go test -tags=integration ./...

# 构建和测试 eBPF
make build-ebpf
make test-ebpf
```

## 📄 许可证

本项目采用 MIT 许可证。

---

**OJP Sentinel** - 内核级别的数据库代理安全卫士！</content>
<parameter name="filePath">E:\a8-turbo\ojp\ojp-sentinel\README.md