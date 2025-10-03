#!/bin/bash
# 替代解决方案：使用host网络模式或自定义bridge网络
# 需要在WSL 2中执行

echo "使用替代网络方案..."

# 方案1：使用host网络模式（最简单但安全性较低）
echo "方案1：使用host网络模式"
docker stop macvlan-test 2>/dev/null || true
docker rm macvlan-test 2>/dev/null || true
docker run -d --name macvlan-test-host --network host nginx
echo "容器使用host网络启动，可通过WSL IP访问"

# 方案2：使用自定义bridge网络 + 端口映射
echo "方案2：使用bridge网络 + 端口映射"
docker network create --driver bridge \
  --subnet=172.20.0.0/16 \
  --gateway=172.20.0.1 \
  custom-bridge 2>/dev/null || true

docker stop macvlan-test-bridge 2>/dev/null || true
docker rm macvlan-test-bridge 2>/dev/null || true
docker run -d --name macvlan-test-bridge \
  --network custom-bridge \
  -p 8080:80 \
  nginx
echo "容器使用bridge网络启动，通过端口8080访问"

# 方案3：使用ipvlan网络（如果macvlan有问题）
echo "方案3：创建ipvlan网络"
docker network rm ipvlan-wsl-net 2>/dev/null || true
docker network create -d ipvlan \
  --subnet=192.168.50.0/24 \
  --gateway=192.168.50.1 \
  --ip-range=192.168.50.96/28 \
  --opt parent=eth0 \
  ipvlan-wsl-net

docker stop macvlan-test-ipvlan 2>/dev/null || true
docker rm macvlan-test-ipvlan 2>/dev/null || true
docker run -d --name macvlan-test-ipvlan \
  --network ipvlan-wsl-net \
  --ip 192.168.50.102 \
  nginx
echo "容器使用ipvlan网络启动，IP: 192.168.50.102"

echo "替代方案部署完成，请测试各个方案的连通性"