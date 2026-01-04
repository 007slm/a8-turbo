package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"ojp-sentinel/internal/container"
	bpfloader "ojp-sentinel/internal/ebpf"
	"ojp-sentinel/internal/license"
)

// 这里是您的公钥，必须与 keygen 生成的匹配
const publicKeyPEM = `-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtOR8Yj/6gf3zUrwRwTc1
wzwQrQlaFSqslFSyxOnTu5no3lBOALmgXJd89Zpjt5uM1bi4pNGHTtBIM5NKyKE6
gQyd3Zj1QY7sxFdEPXPKmXgrM2lqX1xyduwO0v2g0tIa2YZ9Rm1EC/GrtTdBcPjZ
TaTG+scDViF9rXmNAewrWk1Yem4TIvzzaBesFDJtG6c7KiJCyeOx5R12NyKC+HTG
N71k8iqyuAS8VnEuXLbdnQL4Uu0zPnWt9YxNMHe3Ul7Rgtf6CD9o1O+eYxeLkyUX
PKHOLBgQVYkx70cLqxqltl6bcaCY6W4m6jy048wFjtFpjEPC1zPNq2ACQMSlZePO
NQIDAQAB
-----END PUBLIC KEY-----`

const licenseFilePath = "/etc/ojp/license/license.key"
const bpfObjectPath = "/app/sentinel.bpf.o"

func main() {
	fmt.Println("========================================")
	fmt.Println("  OJP Sentinel - Kernel-Level License")
	fmt.Println("========================================")
	fmt.Println()

	// 1. 初始化 License 验证器
	verifier, err := license.NewVerifier([]byte(publicKeyPEM))
	if err != nil {
		log.Fatalf("Failed to initialize license verifier: %v", err)
	}
	fmt.Println("✓ License verifier initialized")

	// 2. 初始化容器管理器
	containerMgr, err := container.NewManager()
	if err != nil {
		log.Fatalf("Failed to create container manager: %v", err)
	}
	defer containerMgr.Close()
	fmt.Println("✓ Container manager initialized")

	// 3. 初始化 eBPF 加载器
	loader, err := bpfloader.NewLoader()
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to create eBPF loader: %v\n", err)
		os.Exit(1)
	}
	defer loader.Close()

	// 4. 加载并 Pin eBPF 程序
	if err := loader.Load(); err != nil {
		fmt.Fprintf(os.Stderr, "Failed to load and pin eBPF program: %v\n", err)
		os.Exit(1)
	}

	fmt.Println("✓ eBPF program loaded")

	// 4. 发现并挂载到目标容器
	if err := discoverAndAttach(containerMgr, loader); err != nil {
		log.Fatalf("Failed to attach eBPF: %v", err)
	}

	// 5. 初始化授权状态
	currentLicense := getActiveLicense()
	processLicense(verifier, loader, currentLicense)

	// 6. 启动监控循环
	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)

	fmt.Println()
	fmt.Println("========================================")
	fmt.Println("  Sentinel is now protecting your system")
	fmt.Println("========================================")
	fmt.Println()

	// 动态检测循环
	go func() {
		lastLicense := currentLicense
		ticker := time.NewTicker(5 * time.Second)
		defer ticker.Stop()

		for {
			select {
			case <-ticker.C:
				currentLicense := getActiveLicense()
				if currentLicense != lastLicense {
					fmt.Println("\n>>> License Update Detected <<<")
					processLicense(verifier, loader, currentLicense)
					lastLicense = currentLicense
				}

			case <-stop:
				return
			}
		}
	}()

	<-stop
	fmt.Println("\nOJP Sentinel Stopping...")
}

// discoverAndAttach 发现目标容器并挂载 eBPF
func discoverAndAttach(mgr *container.Manager, loader *bpfloader.Loader) error {
	containers, err := mgr.GetTargetContainers()
	if err != nil {
		return fmt.Errorf("failed to get target containers: %w", err)
	}
	if len(containers) == 0 {
		fmt.Println("⚠ No target containers found (looking for a8-* or ojp-* prefix)")
		return nil
	}

	fmt.Printf("\n>>> Found %d target container(s) <<<\n", len(containers))
	for _, c := range containers {
		// 跳过自己
		if strings.Contains(c.Name, "sentinel") {
			continue
		}

		veth, err := mgr.GetVethName(c.ID)
		if err != nil {
			fmt.Printf("  ⚠ Failed to get veth for %s: %v\n", c.Name, err)
			continue
		}

		fmt.Printf("  - %s -> %s\n", c.Name, veth)

		if err := loader.AttachToVeth(veth); err != nil {
			fmt.Printf("    ✗ Failed to attach to %s: %v\n", veth, err)
			continue
		}
		fmt.Printf("    ✓ eBPF attached to %s\n", veth)
	}

	return nil
}

// getActiveLicense 获取当前的授权码 (优先级: 文件 > 环境变量)
func getActiveLicense() string {
	// 1. 尝试从共享卷读取
	data, err := os.ReadFile(licenseFilePath)
	if err == nil && len(data) > 0 {
		fmt.Printf("DEBUG: Read license file (%d bytes): %q\n", len(data), data)
		return strings.TrimSpace(string(data))
	}

	// 2. 尝试从环境变量读取
	return os.Getenv("OJP_LICENSE")
}

// processLicense 处理授权验证并更新 eBPF Map
func processLicense(v *license.Verifier, loader *bpfloader.Loader, code string) {
	fmt.Printf("DEBUG: processing license code: %q\n", code)
	if code == "" {
		fmt.Println("⚠ No license found")
		loader.UpdateLicenseState(false)
		return
	}

	payload, valid, err := v.Verify(code)
	if err != nil {
		fmt.Printf("✗ License verification failed: %v\n", err)
		loader.UpdateLicenseState(false)
		return
	}

	if !valid {
		fmt.Printf("⚠ License expired for customer: %s (expiry: %s)\n",
			payload.Customer, payload.ExpiryDate)
		loader.UpdateLicenseState(false)
		return
	}

	fmt.Printf("✓ Valid License - Customer: %s, Expiry: %s\n",
		payload.Customer, payload.ExpiryDate)
	loader.UpdateLicenseState(true)
}
