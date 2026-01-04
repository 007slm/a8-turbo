package license

import (
	"crypto"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"errors"
	"fmt"
	"strings"
	"time"
)

type LicensePayload struct {
	Customer   string `json:"customer"`
	ExpiryDate string `json:"expiry_date"`
}

type Verifier struct {
	PublicKey *rsa.PublicKey
}

func NewVerifier(pemKey []byte) (*Verifier, error) {
	block, _ := pem.Decode(pemKey)
	if block == nil {
		return nil, errors.New("failed to parse PEM block containing the public key")
	}

	pub, err := x509.ParsePKIXPublicKey(block.Bytes)
	if err != nil {
		return nil, err
	}

	rsaPub, ok := pub.(*rsa.PublicKey)
	if !ok {
		return nil, errors.New("not an RSA public key")
	}

	return &Verifier{PublicKey: rsaPub}, nil
}

// Verify 验证符合 [Base64-Payload].[RSA-Signature] 格式的授权码
func (v *Verifier) Verify(licenseCode string) (*LicensePayload, bool, error) {
	parts := strings.Split(licenseCode, ".")
	if len(parts) != 2 {
		return nil, false, errors.New("invalid license format, expected [Payload].[Signature]")
	}

	payloadB64 := parts[0]
	signatureB64 := parts[1]

	// 1. 解码 Payload
	payloadBytes, err := base64.StdEncoding.DecodeString(payloadB64)
	if err != nil {
		return nil, false, fmt.Errorf("failed to decode payload: %v", err)
	}

	var payload LicensePayload
	if err := json.Unmarshal(payloadBytes, &payload); err != nil {
		return nil, false, fmt.Errorf("failed to unmarshal payload: %v", err)
	}

	// 2. 验证签名
	signature, err := base64.StdEncoding.DecodeString(signatureB64)
	if err != nil {
		return nil, false, fmt.Errorf("failed to decode signature: %v", err)
	}

	hashed := sha256.Sum256(payloadBytes)
	err = rsa.VerifyPKCS1v15(v.PublicKey, crypto.SHA256, hashed[:], signature)
	if err != nil {
		return &payload, false, fmt.Errorf("signature verification failed: %v", err)
	}

	// 3. 验证日期
	expiry, err := time.Parse("2006-01-02", payload.ExpiryDate)
	if err != nil {
		return &payload, false, fmt.Errorf("invalid expiry date format: %v", err)
	}

	if time.Now().After(expiry.AddDate(0, 0, 1)) { // 包含过期当天
		return &payload, false, nil // 已过期
	}

	return &payload, true, nil
}
