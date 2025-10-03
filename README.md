# 启动开发环境
dev:
    docker-compose --profile dev up -d

# 启动生产环境
prod:
    docker-compose --profile prod up -d



wsl -- ip -4 addr show eth0 | Select-String -Pattern 'inet\s+(\d+\.\d+\.\d+\.\d+)' | ForEach-Object { $_.Matches.Groups[1].Value }

172.29.139.111

"Subnet" : "172.18.0.0/16",

route add 172.18.0.0 mask 255.255.255.0 172.29.139.111
# 先删除旧的错误路由（如果存在）
route delete 172.18.0.0

# 添加正确的路由（子网掩码对应 /16 网段）
# 172.29.139.111 是你的 WSL2 IP
route add 172.18.0.0 mask 255.255.0.0 172.29.139.111
