#!/bin/bash

echo "Testing OJP Server RESTful API..."

# 等待服务器启动
echo "Waiting for server to start..."
sleep 5

# 测试健康检查
echo "Testing health check..."
curl -s http://localhost:8010/actuator/health

# 测试获取规则列表
echo ""
echo "Testing get rules..."
curl -s http://localhost:8010/api/rules

echo ""
echo "API test completed."
