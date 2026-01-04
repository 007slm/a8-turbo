#!/bin/bash

# 确保在脚本所在目录运行
cd "$(dirname "$0")"

# 1. 检查并创建密钥目录
mkdir -p ./keys

PRIVATE_KEY="./keys/private.pem"
PUBLIC_KEY="./keys/public.pem"

# 2. 检查是否存在密钥对，如果不存在则生成
if [ ! -f "$PRIVATE_KEY" ]; then
    echo "未检测到密钥对，正在生成新的 RSA 2048 位密钥..."
    openssl genrsa -out "$PRIVATE_KEY" 2048 2>/dev/null
    openssl rsa -in "$PRIVATE_KEY" -pubout -out "$PUBLIC_KEY" 2>/dev/null
    echo "✓ 密钥对已生成并保存到 ./keys/ 目录"
    echo ""
fi

# 3. 交互式输入
echo "========================================"
echo "   OJP 商业授权码生成工具 (OpenSSL)"
echo "========================================"
read -p "请输入客户名称 (例如: 某某有限公司): " CUSTOMER
read -p "请输入过期时间 (格式: YYYY-MM-DD): " EXPIRY

if [[ -z "$CUSTOMER" || -z "$EXPIRY" ]]; then
    echo "❌ 错误: 客户名称和过期时间不能为空。"
    exit 1
fi

# 4. 构造 JSON Payload
PAYLOAD=$(cat <<EOF
{"customer":"$CUSTOMER","expiry_date":"$EXPIRY"}
EOF
)

# 5. Base64 编码 Payload
PAYLOAD_B64=$(echo -n "$PAYLOAD" | base64 -w 0)

# 6. 使用私钥对 Payload 进行 SHA256 + RSA 签名
echo -n "$PAYLOAD" | openssl dgst -sha256 -sign "$PRIVATE_KEY" -out /tmp/signature.bin 2>/dev/null
SIGNATURE_B64=$(base64 -w 0 < /tmp/signature.bin)
rm -f /tmp/signature.bin

# 7. 组合成最终授权码
LICENSE_CODE="${PAYLOAD_B64}.${SIGNATURE_B64}"

# 8. 输出结果
echo ""
echo "========================================"
echo "✓ 授权码生成成功！"
echo "========================================"
echo "客户: $CUSTOMER"
echo "到期时间: $EXPIRY"
echo "----------------------------------------"
echo "生成的授权码 (OJP_LICENSE):"
echo ""
echo "$LICENSE_CODE"
echo ""
echo "========================================"
echo ""
echo "📋 提示："
echo "1. 请妥善保管私钥文件: $PRIVATE_KEY"
echo "2. 将以下公钥内容更新到 ojp-sentinel 的 main.go 中："
echo ""
cat "$PUBLIC_KEY"
echo ""
