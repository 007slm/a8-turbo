# eBPF 数据包解析能力详解

## 一、能力概述

**答案：能！eBPF 可以完整解析从 L2 到 L7 的所有网络层数据。**

```
┌─────────────────────────────────────────────────────┐
│  OSI 模型          eBPF 可见性        示例           │
├─────────────────────────────────────────────────────┤
│  L7 应用层         ✅ 完全可见       HTTP/HTTPS      │
│  L6 表示层         ✅ 完全可见       TLS/SSL         │
│  L5 会话层         ✅ 完全可见       -               │
│  L4 传输层         ✅ 完全可见       TCP/UDP         │
│  L3 网络层         ✅ 完全可见       IP/ICMP         │
│  L2 数据链路层     ✅ 完全可见       Ethernet        │
│  L1 物理层         ❌ 不可见         -               │
└─────────────────────────────────────────────────────┘
```

## 二、HTTP vs TCP 的区分

### 2.1 数据包结构

```
┌──────────────────────────────────────────────────────┐
│  以太网头 (14 bytes)                                  │
│  ┌─────────────────────────────────────────────┐     │
│  │ 目标MAC | 源MAC | 类型(0x0800=IP)            │     │
│  └─────────────────────────────────────────────┘     │
├──────────────────────────────────────────────────────┤
│  IP 头 (20+ bytes)                                   │
│  ┌─────────────────────────────────────────────┐     │
│  │ 版本 | 长度 | TTL | 协议(6=TCP) | 源IP | 目标IP│     │
│  └─────────────────────────────────────────────┘     │
├──────────────────────────────────────────────────────┤
│  TCP 头 (20+ bytes)                                  │
│  ┌─────────────────────────────────────────────┐     │
│  │ 源端口 | 目标端口 | SEQ | ACK | 标志位       │     │
│  └─────────────────────────────────────────────┘     │
├──────────────────────────────────────────────────────┤
│  HTTP 数据 (Payload)                                 │
│  ┌─────────────────────────────────────────────┐     │
│  │ GET /api/admin/license HTTP/1.1              │ ← eBPF 可以读到这里！
│  │ Host: localhost:8010                         │     │
│  │ User-Agent: Mozilla/5.0                      │     │
│  └─────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────┘
```

### 2.2 识别方法

#### 方法一：检查端口号（简单但不准确）
```c
if (tcp->dest == bpf_htons(80) || tcp->dest == bpf_htons(8080)) {
    // 可能是 HTTP
}
```

**缺点**：
- HTTP 可以运行在任意端口
- 其他协议也可能使用 80 端口

#### 方法二：检查 Payload 内容（准确）
```c
// 读取 TCP payload 的前几个字节
char *payload = (char *)tcp + (tcp->doff * 4);

// 检查 HTTP 方法
if (payload[0]=='G' && payload[1]=='E' && payload[2]=='T' && payload[3]==' ') {
    // 确定是 HTTP GET 请求
}
```

**优点**：
- 100% 准确
- 可以区分 HTTP/1.1、HTTP/2、gRPC 等

## 三、实际应用场景

### 场景 1：只对 HTTP 流量应用授权检查

```c
SEC("classifier")
int smart_license_filter(struct __sk_buff *skb) {
    // 1. 解析到 TCP 层
    struct tcphdr *tcp = parse_tcp(skb);
    
    // 2. 获取 payload
    void *payload = get_tcp_payload(tcp);
    
    // 3. 检查是否是 HTTP
    if (is_http(payload)) {
        // 4. 检查授权状态
        if (!is_licensed()) {
            // 只对 HTTP 流量应用延迟
            return TC_ACT_SHOT; // 或者标记后由 TC netem 处理
        }
    }
    
    // 其他流量（SSH、数据库连接等）不受影响
    return TC_ACT_OK;
}
```

### 场景 2：基于 URL 的精细化控制

```c
SEC("classifier")
int url_based_filter(struct __sk_buff *skb) {
    char url[128];
    
    // 提取 HTTP URL
    if (extract_http_url(skb, url, sizeof(url)) == 0) {
        // 检查是否访问管理接口
        if (starts_with(url, "/api/admin/")) {
            // 只对管理接口要求授权
            if (!is_licensed()) {
                return TC_ACT_SHOT;
            }
        }
    }
    
    // 普通 API 不受限制
    return TC_ACT_OK;
}
```

### 场景 3：区分 HTTP 和 gRPC

```c
// HTTP/1.1: "GET /path HTTP/1.1"
// HTTP/2 (gRPC): 二进制帧，magic string "PRI * HTTP/2.0"

if (is_http1(payload)) {
    bpf_printk("检测到 HTTP/1.1 请求");
} else if (is_http2(payload)) {
    bpf_printk("检测到 HTTP/2/gRPC 请求");
} else if (is_grpc(payload)) {
    bpf_printk("检测到 gRPC 调用");
}
```

## 四、性能考虑

### 4.1 解析开销

| 解析层级 | 开销 | 说明 |
|---------|------|------|
| L2 (Ethernet) | ~10 ns | 固定偏移，极快 |
| L3 (IP) | ~20 ns | 需要检查 IP 头长度 |
| L4 (TCP) | ~30 ns | 需要检查 TCP 选项 |
| L7 (HTTP) | ~100 ns | 需要字符串匹配 |

**总开销**：< 1 微秒（对比应用层处理的毫秒级延迟，可忽略）

### 4.2 eBPF 限制

1. **指令数限制**：早期内核限制 4096 条指令，现代内核可达 100 万条
2. **栈空间限制**：512 字节（需要谨慎使用局部变量）
3. **循环限制**：必须是有界循环（内核 5.3+ 支持）
4. **Helper 函数**：只能调用内核提供的 helper 函数

## 五、OJP 项目的应用建议

### 当前方案（简单粗暴）
```
所有流量 → 检查授权 → 全部延迟 5s（如果无授权）
```

**优点**：
- 实现简单
- 100% 覆盖

**缺点**：
- 影响所有流量（包括 SSH、监控等）

### 优化方案（精细化控制）
```
所有流量 → 分类
  ├─ HTTP 流量 → 检查授权 → 延迟（如果无授权）
  ├─ gRPC 流量 → 检查授权 → 延迟（如果无授权）
  └─ 其他流量 → 放行
```

**优点**：
- 只影响业务流量
- 不影响运维操作（SSH、监控）

**实现示例**：
```c
SEC("classifier")
int ojp_smart_filter(struct __sk_buff *skb) {
    struct tcphdr *tcp = parse_tcp(skb);
    void *payload = get_payload(tcp);
    
    // 检查是否是 OJP 业务流量
    __u16 dest_port = bpf_ntohs(tcp->dest);
    
    if (dest_port == 8010) {  // ojp-server 端口
        if (is_http(payload) || is_grpc(payload)) {
            // 检查授权
            if (!check_license()) {
                // 标记为需要延迟的包
                skb->mark = 1;
            }
        }
    }
    
    return TC_ACT_OK;
}
```

然后在 TC 规则中：
```bash
# 只对标记为 1 的包应用延迟
tc filter add dev veth123 parent 1: protocol ip prio 1 handle 1 fw flowid 1:1
tc qdisc add dev veth123 parent 1:1 netem delay 5000ms
```

## 六、HTTPS 的特殊情况

### 问题
HTTPS 流量是加密的，eBPF 无法直接读取 HTTP 内容。

### 解决方案

#### 方案 1：TLS 握手检测
```c
// 检查 TLS Client Hello
if (payload[0] == 0x16 && payload[1] == 0x03) {
    bpf_printk("检测到 TLS 握手");
    // 可以提取 SNI (Server Name Indication)
}
```

#### 方案 2：端口 + 连接特征
```c
if (tcp->dest == bpf_htons(443) || tcp->dest == bpf_htons(8443)) {
    // 大概率是 HTTPS
}
```

#### 方案 3：使用 eBPF kprobe 拦截 SSL 函数
```c
// 在 SSL_read/SSL_write 之前拦截，可以看到明文
SEC("kprobe/SSL_read")
int trace_ssl_read(struct pt_regs *ctx) {
    // 可以读取解密后的数据
}
```

## 七、总结

### eBPF 的强大之处
✅ 可以解析任意网络协议（HTTP、gRPC、MySQL、Redis 等）  
✅ 可以提取应用层数据（URL、SQL 语句、命令等）  
✅ 性能开销极低（纳秒级）  
✅ 内核级执行，无法被应用层绕过  

### 对 OJP 项目的意义
您可以实现：
1. **智能流量分类**：只对业务流量应用授权检查
2. **API 级别控制**：不同 API 路径不同授权策略
3. **协议感知**：区分 HTTP、gRPC、数据库协议
4. **零性能损耗**：相比应用层中间件，eBPF 几乎无开销

**建议**：
- 初期：使用简单方案（全流量检查）
- 后期：升级为智能方案（HTTP/gRPC 精细化控制）
