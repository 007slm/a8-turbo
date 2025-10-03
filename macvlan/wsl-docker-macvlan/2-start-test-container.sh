#!/bin/bash
# 在WSL中执行，启动一个Nginx容器测试网络

docker run -d --name macvlan-test --network macvlan-wsl-net --ip 192.168.50.101 nginx

echo "测试容器启动完成，IP: 192.168.50.101"
    