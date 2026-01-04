package ebpf

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"

	"github.com/cilium/ebpf"
	"github.com/cilium/ebpf/link"
	"golang.org/x/sys/unix"
)

// Loader eBPF 程序加载器
type Loader struct {
	collection *ebpf.Collection
	licenseMap *ebpf.Map
	program    *ebpf.Program
	links      map[string]link.Link // veth -> link
}

// LicenseConfig 授权配置结构（与 C 代码中的结构体对应）
type LicenseConfig struct {
	IsValid  uint32
	Reserved uint32
}

// NewLoader 创建 eBPF 加载器
func NewLoader() (*Loader, error) {
	return &Loader{
		links: make(map[string]link.Link),
	}, nil
}

// Load 加载 eBPF 程序并 Pin 到文件系统
func (l *Loader) Load() error {
	// 1. 允许 RLIMIT_MEMLOCK
	if err := unix.Setrlimit(unix.RLIMIT_MEMLOCK, &unix.Rlimit{
		Cur: unix.RLIM_INFINITY,
		Max: unix.RLIM_INFINITY,
	}); err != nil {
		return fmt.Errorf("failed to set rlimit: %w", err)
	}

	// 2. 加载 BPF 对象文件
	bpfObjPath := "/app/sentinel.bpf.o"
	spec, err := ebpf.LoadCollectionSpec(bpfObjPath)
	if err != nil {
		return fmt.Errorf("failed to load eBPF spec: %w", err)
	}

	// 3. Pin 路径设置
	pinPath := "/sys/fs/bpf"

	// 确保 /sys/fs/bpf 是 bpffs 挂载点
	var st unix.Statfs_t
	if err := unix.Statfs(pinPath, &st); err != nil {
		return fmt.Errorf("failed to statfs %s: %w", pinPath, err)
	}
	// BPF_FS_MAGIC = 0xcafe4a11
	if st.Type != 0xcafe4a11 {
		fmt.Printf("Mounting bpffs at %s...\n", pinPath)
		if err := unix.Mount("bpf", pinPath, "bpf", 0, ""); err != nil {
			return fmt.Errorf("failed to mount bpffs: %w", err)
		}
	}

	progPinPath := filepath.Join(pinPath, "sentinel_prog")

	// 清理旧的 pin 文件
	_ = os.Remove(progPinPath)

	// 4. 创建 Collection
	coll, err := ebpf.NewCollection(spec)
	if err != nil {
		return fmt.Errorf("failed to create collection: %w", err)
	}
	l.collection = coll

	// 获取 Program 和 Map
	l.program = coll.Programs["sentinel_ingress"]
	if l.program == nil {
		return fmt.Errorf("program 'sentinel_ingress' not found in collection")
	}

	l.licenseMap = coll.Maps["license_map"]
	if l.licenseMap == nil {
		return fmt.Errorf("map 'license_map' not found in collection")
	}

	// 5. Pin Program
	if err := l.program.Pin(progPinPath); err != nil {
		return fmt.Errorf("failed to pin program: %w", err)
	}

	fmt.Printf("✓ eBPF program pinned to %s\n", progPinPath)

	return nil
}

// AttachToVeth 将 eBPF 程序挂载到规定的 veth 网卡 (使用 Pinned Program)
func (l *Loader) AttachToVeth(vethName string) error {
	progPinPath := "/sys/fs/bpf/sentinel_prog"

	// 1. 确保 clsact qdisc 存在
	cmd := exec.Command("tc", "qdisc", "add", "dev", vethName, "clsact")
	_ = cmd.Run() // 可能已存在，忽略错误

	// 2. 在 ingress 方向 attach pinned prog
	attachCmd := func(direction string) error {
		// 先无条件尝试删除旧 filter，忽略错误（如果不存在会报错，没关系）
		_ = exec.Command("tc", "filter", "del", "dev", vethName, direction).Run()

		cmd := exec.Command("tc", "filter", "add", "dev", vethName,
			direction, "prio", "1", "handle", "1", "bpf",
			"direct-action", "object-pinned", progPinPath)

		output, err := cmd.CombinedOutput()
		if err != nil {
			return fmt.Errorf("failed to attach: %w, output: %s", err, string(output))
		}
		return nil
	}

	if err := attachCmd("ingress"); err != nil {
		return fmt.Errorf("ingress attach failed: %w", err)
	}
	fmt.Printf("  ✓ eBPF attached to %s (ingress)\n", vethName)

	if err := attachCmd("egress"); err != nil {
		return fmt.Errorf("egress attach failed: %w", err)
	}
	fmt.Printf("  ✓ eBPF attached to %s (egress)\n", vethName)

	return nil
}

// DetachFromVeth 从指定的 veth 网卡卸载 eBPF 程序
func (l *Loader) DetachFromVeth(vethName string) error {
	// 删除 ingress tc filter
	cmd := exec.Command("tc", "filter", "del", "dev", vethName, "ingress")
	_ = cmd.Run() // 忽略错误

	// 删除 egress tc filter
	cmd = exec.Command("tc", "filter", "del", "dev", vethName, "egress")
	_ = cmd.Run() // 忽略错误

	fmt.Printf("✓ eBPF detached from %s\n", vethName)
	return nil
}

// UpdateLicenseState 更新授权状态 (直接更新 Go 持有的 Map 即可)
func (l *Loader) UpdateLicenseState(isValid bool) error {
	validVal := uint8(0)
	if isValid {
		validVal = 1
	}

	key := uint32(0)
	config := LicenseConfig{
		IsValid: uint32(validVal),
	}

	// 因为使用了 Pinned Prog，所有 attach 的 filter 都共享同一个 Map
	// 所以只需要更新 l.licenseMap 即可
	if l.licenseMap == nil {
		return fmt.Errorf("license map not loaded")
	}
	if err := l.licenseMap.Put(&key, &config); err != nil {
		return fmt.Errorf("failed to update license map: %w", err)
	}

	fmt.Printf("✓ License state updated: %v\n", isValid)
	return nil
}

// Close 关闭 Loader 并清理资源
func (l *Loader) Close() error {
	// 卸载所有挂载点
	for veth, lnk := range l.links {
		if err := lnk.Close(); err != nil {
			fmt.Printf("Error closing link for %s: %v\n", veth, err)
		}
	}

	// 关闭 collection (会自动关闭 prog 和 maps)
	if l.collection != nil {
		l.collection.Close()
	}

	// 清理 Pin 文件?
	// 通常进程退出应清理，但我们希望 filter 继续生效吗？
	// 不，如果 ojp-sentinel 挂了，filter 会残留但 prog pin 会失效？
	// 这里我们先不 remove pin，因为 tc 用着呢

	return nil
}
