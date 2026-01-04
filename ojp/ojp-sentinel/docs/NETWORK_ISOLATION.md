# eBPF 网络隔离与精确控制

## 一、核心原理

### 1.1 TC (Traffic Control) 的工作方式

TC 是**按网卡（网络接口）粒度**工作的，不是全局的：

```
tc filter add dev <网卡名称> ingress bpf obj program.o
                    ↑
                    只影响这个网卡
```

### 1.2 Docker 容器的网络拓扑

每个 Docker 容器都有一对 veth（虚拟以太网）设备：

```
┌─────────────────────────────────────────────────────┐
│  容器: ojp-server                                    │
│  ┌─────────────┐                                     │
│  │   eth0      │ ← 容器内看到的网卡                   │
│  └──────┬──────┘                                     │
└─────────┼────────────────────────────────────────────┘
          │ (veth pair)
          │
┌─────────┼────────────────────────────────────────────┐
│  宿主机 │                                             │
│  ┌─────┴──────────┐                                  │
│  │ veth123abc     │ ← 宿主机看到的网卡（随机名称）     │
│  └────────────────┘                                  │
│         ↓                                            │
│  [eBPF 挂载在这里]  ← 只影响这个容器的流量            │
└──────────────────────────────────────────────────────┘
```

## 二、精确控制实现

### 2.1 识别目标容器的网卡

```go
package main

import (
    "context"
    "fmt"
    "os/exec"
    "strings"
    
    "github.com/docker/docker/api/types"
    "github.com/docker/docker/client"
)

// 获取所有 ojp- 开头的容器
func getOJPContainers() ([]types.Container, error) {
    cli, err := client.NewClientWithOpts(client.FromEnv)
    if err != nil {
        return nil, err
    }
    
    // 过滤器：只获取 ojp- 开头的容器
    containers, err := cli.ContainerList(context.Background(), types.ContainerListOptions{
        Filters: filters.NewArgs(
            filters.Arg("name", "ojp-"),
        ),
    })
    
    return containers, err
}

// 获取容器的 veth 网卡名称
func getVethName(containerID string) (string, error) {
    // 1. 获取容器的网络命名空间 PID
    cmd := exec.Command("docker", "inspect", "-f", "{{.State.Pid}}", containerID)
    output, err := cmd.Output()
    if err != nil {
        return "", err
    }
    pid := strings.TrimSpace(string(output))
    
    // 2. 进入容器的网络命名空间，获取 eth0 的 ifindex
    cmd = exec.Command("nsenter", "-t", pid, "-n", "cat", "/sys/class/net/eth0/iflink")
    output, err = cmd.Output()
    if err != nil {
        return "", err
    }
    ifindex := strings.TrimSpace(string(output))
    
    // 3. 在宿主机上找到对应的 veth 设备
    cmd = exec.Command("ip", "link", "show")
    output, err = cmd.Output()
    if err != nil {
        return "", err
    }
    
    // 解析输出，找到 ifindex 对应的网卡名称
    // 例如: "123: veth123abc@if124: ..."
    lines := strings.Split(string(output), "\n")
    for _, line := range lines {
        if strings.Contains(line, ifindex+":") {
            parts := strings.Split(line, ":")
            if len(parts) >= 2 {
                vethName := strings.TrimSpace(parts[1])
                vethName = strings.Split(vethName, "@")[0]
                return vethName, nil
            }
        }
    }
    
    return "", fmt.Errorf("veth not found for container %s", containerID)
}

// 在指定网卡上挂载 eBPF 程序
func attacheBPFToVeth(vethName string, bpfObjPath string) error {
    // 使用 tc 命令挂载 eBPF
    cmd := exec.Command("tc", "qdisc", "add", "dev", vethName, "clsact")
    _ = cmd.Run() // 可能已存在，忽略错误
    
    cmd = exec.Command("tc", "filter", "add", "dev", vethName, 
        "ingress", "bpf", "da", "obj", bpfObjPath, "sec", "classifier")
    
    return cmd.Run()
}

// 主函数：只对 OJP 容器应用 eBPF
func main() {
    // 1. 获取所有 ojp- 容器
    containers, err := getOJPContainers()
    if err != nil {
        panic(err)
    }
    
    fmt.Printf("找到 %d 个 OJP 容器\n", len(containers))
    
    // 2. 对每个容器挂载 eBPF
    for _, container := range containers {
        vethName, err := getVethName(container.ID)
        if err != nil {
            fmt.Printf("跳过容器 %s: %v\n", container.Names[0], err)
            continue
        }
        
        fmt.Printf("容器 %s -> veth %s\n", container.Names[0], vethName)
        
        err = attacheBPFToVeth(vethName, "/app/sentinel.bpf.o")
        if err != nil {
            fmt.Printf("挂载失败: %v\n", err)
        } else {
            fmt.Printf("✓ eBPF 已挂载到 %s\n", vethName)
        }
    }
}
```

### 2.2 验证隔离性

```bash
# 查看所有网卡的 TC 规则
tc filter show dev eth0        # 宿主机物理网卡 - 应该为空
tc filter show dev docker0     # Docker 网桥 - 应该为空
tc filter show dev veth123abc  # OJP 容器网卡 - 应该有 eBPF 规则

# 输出示例（只有 ojp 容器的 veth 有规则）：
# filter protocol all pref 49152 bpf chain 0 
# filter protocol all pref 49152 bpf chain 0 handle 0x1 sentinel.bpf.o:[classifier]
```

## 三、安全边界

### 3.1 受影响的范围

| 网络接口 | 是否受影响 | 说明 |
|---------|-----------|------|
| eth0 (宿主机) | ❌ 否 | 宿主机的网络不受影响 |
| docker0 | ❌ 否 | Docker 网桥不受影响 |
| veth-ojp-server | ✅ 是 | OJP 容器的流量受控 |
| veth-ojp-cache | ✅ 是 | OJP 容器的流量受控 |
| veth-nginx | ❌ 否 | 其他容器不受影响 |
| veth-mysql | ❌ 否 | 其他容器不受影响 |

### 3.2 流量路径分析

```
场景 1: 访问宿主机上的其他服务
浏览器 → 宿主机 eth0 → 其他服务
                ↑
            不经过 veth，不受 eBPF 影响

场景 2: 访问 OJP 容器
浏览器 → 宿主机 eth0 → docker0 → veth-ojp-server → ojp-server 容器
                                        ↑
                                    eBPF 在这里检查

场景 3: 访问其他 Docker 容器
浏览器 → 宿主机 eth0 → docker0 → veth-nginx → nginx 容器
                                      ↑
                                  没有 eBPF，不受影响
```

## 四、动态管理

### 4.1 容器生命周期监听

```go
// 监听 Docker 事件，自动处理容器的启动和停止
func watchContainerEvents() {
    cli, _ := client.NewClientWithOpts(client.FromEnv)
    
    events, _ := cli.Events(context.Background(), types.EventsOptions{
        Filters: filters.NewArgs(
            filters.Arg("type", "container"),
            filters.Arg("event", "start"),
            filters.Arg("event", "die"),
        ),
    })
    
    for event := range events {
        containerName := event.Actor.Attributes["name"]
        
        // 只处理 ojp- 开头的容器
        if !strings.HasPrefix(containerName, "ojp-") {
            continue
        }
        
        switch event.Action {
        case "start":
            // 容器启动，挂载 eBPF
            vethName, _ := getVethName(event.Actor.ID)
            attacheBPFToVeth(vethName, "/app/sentinel.bpf.o")
            fmt.Printf("✓ 新容器 %s 已受保护\n", containerName)
            
        case "die":
            // 容器停止，自动清理（veth 设备会被删除）
            fmt.Printf("容器 %s 已停止\n", containerName)
        }
    }
}
```

### 4.2 清理机制

```go
// 卸载 eBPF 程序
func detacheBPFFromVeth(vethName string) error {
    cmd := exec.Command("tc", "filter", "del", "dev", vethName, "ingress")
    return cmd.Run()
}

// 清理所有 eBPF 规则
func cleanupAll() {
    containers, _ := getOJPContainers()
    for _, container := range containers {
        vethName, _ := getVethName(container.ID)
        detacheBPFFromVeth(vethName)
    }
}
```

## 五、测试验证

### 5.1 验证隔离性

```bash
# 1. 启动 OJP 环境
docker-compose -f docker-compose.yml -f docker-compose-sentinel.yml up -d

# 2. 启动一个测试容器（非 OJP）
docker run -d --name test-nginx nginx

# 3. 检查 eBPF 挂载情况
# 获取 ojp-server 的 veth
VETH_OJP=$(docker exec ojp-sentinel get-veth ojp-server)
tc filter show dev $VETH_OJP  # 应该有 eBPF 规则

# 获取 test-nginx 的 veth
VETH_NGINX=$(docker exec ojp-sentinel get-veth test-nginx)
tc filter show dev $VETH_NGINX  # 应该为空（没有规则）

# 4. 测试流量
# OJP 容器：受授权控制
curl http://localhost:8010/api/test  # 如果无授权，会被阻断

# Nginx 容器：不受影响
curl http://localhost:8080  # 正常访问，不受授权影响
```

### 5.2 性能测试

```bash
# 测试宿主机网络性能（不应受影响）
iperf3 -c <宿主机IP>  # 应该达到正常速度

# 测试其他容器性能（不应受影响）
docker exec test-nginx iperf3 -c <目标>  # 应该达到正常速度

# 测试 OJP 容器性能（授权有效时不应有额外开销）
docker exec ojp-server iperf3 -c <目标>  # 应该达到正常速度
```

## 六、总结

### ✅ 安全保证

1. **精确隔离**：只影响 `ojp-` 开头的容器
2. **宿主机安全**：宿主机网络完全不受影响
3. **其他容器安全**：其他 Docker 容器不受影响
4. **动态管理**：容器启停自动处理

### 🎯 实现要点

1. 使用 Docker API 过滤目标容器
2. 通过网络命名空间找到 veth 设备
3. 只在特定 veth 上挂载 eBPF
4. 监听容器事件，动态调整

### 📊 影响范围对比

| 方案 | 影响范围 | 风险 |
|-----|---------|------|
| ❌ 全局 iptables | 整个宿主机 | 高 |
| ❌ 全局 eBPF (XDP) | 整个宿主机 | 高 |
| ✅ TC + 精确 veth | 仅 OJP 容器 | 低 |

**结论**：我们的方案是安全的，只影响 OJP 产品的容器，不会影响宿主机或其他应用！
