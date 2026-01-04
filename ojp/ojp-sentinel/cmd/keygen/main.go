package main

import (
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"flag"
	"fmt"
	"log"
	"os"
	"path/filepath"
)

type LicensePayload struct {
	Customer   string `json:"customer"`
	ExpiryDate string `json:"expiry_date"`
}

func main() {
	customer := flag.String("customer", "", "客户名称")
	expiry := flag.String("expiry", "", "过期时间 (YYYY-MM-DD)")
	keyDir := flag.String("keys", "./keys", "密钥存储目录")
	flag.Parse()

	if *customer == "" || *expiry == "" {
		fmt.Println("Usage: go run main.go -customer \"张三\" -expiry \"2025-12-31\"")
		return
	}

	// 1. 确保密钥存在
	privKey, pubKey, err := loadOrGenKeys(*keyDir)
	if err != nil {
		log.Fatalf("密钥加载失败: %v", err)
	}

	// 2. 构造 Payload
	payload := LicensePayload{
		Customer:   *customer,
		ExpiryDate: *expiry,
	}
	payloadBytes, _ := json.Marshal(payload)
	payloadB64 := base64.StdEncoding.EncodeToString(payloadBytes)

	// 3. 计算签名
	hashed := sha256.Sum256(payloadBytes)
	signature, err := rsa.SignPKCS1v15(rand.Reader, privKey, crypto.SHA256, hashed[:])
	if err != nil {
		log.Fatalf("签名失败: %v", err)
	}
	sigB64 := base64.StdEncoding.EncodeToString(signature)

	// 4. 输出最终授权码
	licenseCode := fmt.Sprintf("%s.%s", payloadB64, sigB64)

	fmt.Println("\n========================================")
	fmt.Printf("客户: %s\n", *customer)
	fmt.Printf("到期时间: %s\n", *expiry)
	fmt.Println("----------------------------------------")
	fmt.Println("生成的授权码 (OJP_LICENSE):")
	fmt.Println(licenseCode)
	fmt.Println("========================================\n")

	// 打印公钥，方便更新到 Sentinel 代码中
	pubKeyBytes, _ := x509.MarshalPKIXPublicKey(pubKey)
	pubPEM := pem.EncodeToMemory(&pem.Block{Type: "PUBLIC KEY", Bytes: pubKeyBytes})
	fmt.Println("提示：请确保 Sentinel 容器或代码中使用的是以下公钥：")
	fmt.Println(string(pubPEM))
}

func loadOrGenKeys(dir string) (*rsa.PrivateKey, *rsa.PublicKey, error) {
	privPath := filepath.Join(dir, "private.pem")
	pubPath := filepath.Join(dir, "public.pem")

	if _, err := os.Stat(privPath); os.IsNotExist(err) {
		fmt.Println("未检测到密钥对，正在生成新密钥...")
		if err := os.MkdirAll(dir, 0700); err != nil {
			return nil, nil, err
		}

		key, err := rsa.GenerateKey(rand.Reader, 2048)
		if err != nil {
			return nil, nil, err
		}

		// 保存私钥
		privFile, _ := os.Create(privPath)
		pem.Encode(privFile, &pem.Block{Type: "RSA PRIVATE KEY", Bytes: x509.MarshalPKCS1PrivateKey(key)})
		privFile.Close()

		// 保存公钥
		pubFile, _ := os.Create(pubPath)
		pubBytes, _ := x509.MarshalPKIXPublicKey(&key.PublicKey)
		pem.Encode(pubFile, &pem.Block{Type: "PUBLIC KEY", Bytes: pubBytes})
		pubFile.Close()

		return key, &key.PublicKey, nil
	}

	// 加载现有私钥
	data, _ := os.ReadFile(privPath)
	block, _ := pem.Decode(data)
	key, _ := x509.ParsePKCS1PrivateKey(block.Bytes)

	return key, &key.PublicKey, nil
}
