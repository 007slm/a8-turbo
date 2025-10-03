#!/bin/bash
# 修复macvlan网络隔离问题
# 需要在WSL 2中以root权限执行（sudo -i）

echo "修复macvlan网络隔离问题..."

# 方案1：创建bridge接口解决隔离问题
# 删除现有的mac0接口
ip link delete mac0 2>/dev/null || true

# 创建一个bridge接口
ip link add name br-macvlan type bridge
ip link set br-macvlan up

# 创建一个veth pair连接bridge和容器网络
ip link add veth-host type veth peer name veth-container
ip link set veth-host master br-macvlan
ip link set veth-host up

# 将veth-container移动到容器的网络命名空间（这需要额外配置）
# 这里我们使用另一种方法：创建macvlan-bridge模式

# 方案2：使用macvlan bridge模式（推荐）
# 删除现有网络
docker network rm macvlan-wsl-net 2>/dev/null || true

# 重新创建使用bridge模式的macvlan网络
docker network create -d macvlan \
  --subnet=192.168.50.0/24 \
  --gateway=192.168.50.1 \
  --ip-range=192.168.50.96/28 \
  --opt parent=eth0 \
  --opt macvlan_mode=bridge \
  macvlan-wsl-net

# 创建一个macvlan接口用于主机通信，使用不同的模式
ip link add mac0 link eth0 type macvlan mode bridge
ip addr add 192.168.50.99/24 dev mac0
ip link set mac0 up

# 添加路由确保流量正确转发
ip route add 192.168.50.96/28 dev mac0 2>/dev/null || true

echo "macvlan网络隔离问题修复完成"
echo "请重新启动容器测试连通性"