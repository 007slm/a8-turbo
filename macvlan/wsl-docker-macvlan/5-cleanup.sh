# 在WSL中执行，清理所有配置
docker stop macvlan-test
docker rm macvlan-test
docker network rm macvlan-wsl-net

# 删除WSL中的macvlan接口
ip link delete mac0

# 清除IP转发配置
echo 0 > /proc/sys/net/ipv4/ip_forward
iptables -D FORWARD -i mac0 -j ACCEPT
iptables -D FORWARD -o mac0 -j ACCEPT

echo "清理完成"
    