#!/bin/bash
# 注意：此脚本需在WSL 2中以root权限执行（sudo -i）

# 配置参数（根据实际网络修改）
WSL_NIC="eth0"                  # WSL 2的虚拟网卡（通常是eth0）
PHYSICAL_SUBNET="192.168.50.0/24" # 宿主机所在物理网段
GATEWAY="192.168.50.1"           # 物理网络网关（路由器IP）
WSL_MACVLAN_IP="192.168.50.99/24" # 分配给WSL的macvlan IP（需在物理网段内）
DOCKER_MACVLAN_RANGE="192.168.50.96/28" # 容器IP范围（96-111）

# 1. 创建Docker macvlan网络
docker network create -d macvlan \
  --subnet=$PHYSICAL_SUBNET \
  --gateway=$GATEWAY \
  --ip-range=$DOCKER_MACVLAN_RANGE \
  --opt parent=$WSL_NIC \
  macvlan-wsl-net

# 2. 在WSL中创建macvlan接口（供宿主机通信）
ip link add mac0 link $WSL_NIC type macvlan mode bridge
ip addr add $WSL_MACVLAN_IP dev mac0
ip link set mac0 up

# 3. 配置WSL的IP转发（允许宿主机通过WSL访问容器）
echo 1 > /proc/sys/net/ipv4/ip_forward
iptables -A FORWARD -i mac0 -j ACCEPT
iptables -A FORWARD -o mac0 -j ACCEPT

echo "WSL中macvlan网络创建完成"
echo "WSL的macvlan接口IP: $WSL_MACVLAN_IP"
    