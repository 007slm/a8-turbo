#!/bin/bash

# 等待NATS服务启动并检查连接
echo "等待NATS服务启动..."
MAX_WAIT=30
WAIT_COUNT=0

while ! nats account info --server nats://nats:4222 > /dev/null 2>&1; do
    WAIT_COUNT=$((WAIT_COUNT + 1))
    if [ $WAIT_COUNT -gt $MAX_WAIT ]; then
        echo "NATS服务器连接超时"
        exit 1
    fi
    echo "等待NATS服务器就绪... (${WAIT_COUNT}/${MAX_WAIT})"
    sleep 2
done

echo "NATS服务器已就绪"

# 检查cdc-stream是否已初始化
if nats stream info cdc-stream --server nats://nats:4222 > /dev/null 2>&1; then
    echo "cdc-stream流已存在，跳过初始化"
else
    echo "创建cdc-stream流..."
    nats stream add --config /etc/nats/cdc-stream.json --server nats://nats:4222
fi

# 检查monitoring-stream是否已初始化，如果存在但配置不正确则删除重建
if nats stream info monitoring-stream --server nats://nats:4222 > /dev/null 2>&1; then
    echo "删除已存在的monitoring-stream流..."
    nats stream rm monitoring-stream --force --server nats://nats:4222
fi

echo "创建monitoring-stream流..."
nats stream add --config /etc/nats/monitoring-stream.json --server nats://nats:4222

echo "NATS初始化完成"