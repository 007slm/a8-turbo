package container

import (
	"fmt"
	"os/exec"
	"strings"
)

// Manager 管理 Docker 容器和网卡（简化版 - 直接使用网桥）
type Manager struct{}

// NewManager 创建容器管理器
func NewManager() (*Manager, error) {
	// 检查 docker 命令是否可用
	cmd := exec.Command("docker", "version")
	if err := cmd.Run(); err != nil {
		return nil, fmt.Errorf("docker command not available: %w", err)
	}

	return &Manager{}, nil
}

// ContainerInfo 容器信息
type ContainerInfo struct {
	ID   string
	Name string
}

// GetVethName 获取容器在宿主机上的 veth 接口名称
func (m *Manager) GetVethName(containerID string) (string, error) {
	// 1. 获取容器 PID
	cmd := exec.Command("docker", "inspect", "-f", "{{.State.Pid}}", containerID)
	output, err := cmd.Output()
	if err != nil {
		return "", fmt.Errorf("failed to get container PID: %w", err)
	}
	pid := strings.TrimSpace(string(output))
	if pid == "0" || pid == "" {
		return "", fmt.Errorf("container is not running")
	}

	// 2. 进入容器的网络命名空间，执行 ip link show eth0
	// 输出类似: "2: eth0@if45: <...>"，其中 45 是宿主机上的 veth 索引
	cmd = exec.Command("nsenter", "-t", pid, "-n", "ip", "link", "show", "eth0")
	output, err = cmd.Output()
	if err != nil {
		return "", fmt.Errorf("failed to exec ip link in container: %w", err)
	}

	outStr := string(output)
	// 查找 @if<数字>
	if idx := strings.Index(outStr, "@if"); idx != -1 {
		// 截取 @if 之后的部分
		after := outStr[idx+3:] // +3 跳过 "@if"
		// 找到接下来的第一个非数字字符（通常是冒号）
		endIdx := 0
		for i, r := range after {
			if r < '0' || r > '9' {
				endIdx = i
				break
			}
		}
		if endIdx > 0 {
			peerIndex := after[:endIdx]

			// 3. 在宿主机上查找对应索引的接口名称
			// 使用 ip link show | grep "^<index>:"
			cmd := exec.Command("ip", "link", "show")
			output, err = cmd.Output()
			if err != nil {
				return "", fmt.Errorf("failed to list host interfaces: %w", err)
			}

			prefix := peerIndex + ":"
			lines := strings.Split(string(output), "\n")
			for _, line := range lines {
				line = strings.TrimSpace(line)
				if strings.HasPrefix(line, prefix) {
					// 格式如: "45: vethxxxx@if2: <...>"
					parts := strings.Split(line, ":")
					if len(parts) >= 2 {
						namePart := strings.TrimSpace(parts[1])
						// 去掉 @if... 部分
						if atIdx := strings.Index(namePart, "@"); atIdx != -1 {
							return namePart[:atIdx], nil
						}
						return namePart, nil
					}
				}
			}
			return "", fmt.Errorf("failed to find host interface with index %s", peerIndex)
		}
	}

	return "", fmt.Errorf("failed to parse peer index from output: %s", outStr)
}

// GetTargetContainers 获取所有需要控制的容器（a8- 或 ojp- 开头）
func (m *Manager) GetTargetContainers() ([]ContainerInfo, error) {
	// 使用 docker ps 获取运行中的容器
	cmd := exec.Command("docker", "ps", "--format", "{{.ID}}|{{.Names}}")
	output, err := cmd.Output()
	if err != nil {
		return nil, fmt.Errorf("failed to list containers: %w", err)
	}

	var containers []ContainerInfo
	lines := strings.Split(strings.TrimSpace(string(output)), "\n")

	for _, line := range lines {
		if line == "" {
			continue
		}

		parts := strings.Split(line, "|")
		if len(parts) != 2 {
			continue
		}

		id := parts[0]
		name := parts[1]

		// 过滤出 a8- 或 ojp- 开头的容器
		if strings.HasPrefix(name, "a8-") || strings.HasPrefix(name, "ojp-") {
			containers = append(containers, ContainerInfo{
				ID:   id,
				Name: name,
			})
		}
	}

	return containers, nil
}

// GetTargetInterfaces 这里的实现改为返回 nil，因为我们现在按容器处理，不再按网桥处理
func (m *Manager) GetTargetInterfaces() ([]string, error) {
	return nil, nil
}

// Close 关闭管理器（shell 版本无需关闭）
func (m *Manager) Close() error {
	return nil
}
