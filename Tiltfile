# Tiltfile - 修正版：解决本地服务与Docker容器联调问题

# 配置参数
BACKEND_PORT = 8010
MYSQL_PORT = 3306
UI_PORT = 5173
PROJECT_NAME = "a8-turbo"

def get_host_ip():
    """获取宿主机IP地址，用于容器访问本地服务"""
    # 在Tilt中使用简化的IP获取方式
    # 默认使用host.docker.internal或localhost
    return "host.docker.internal"

def configure_docker_services():
    """配置并启动Docker Compose服务"""
    host_ip = get_host_ip()
    print("检测到宿主机IP: %s" % host_ip)

    # 启动Docker Compose服务
    dc = docker_compose(
        'docker-compose.yml',
        project_name=PROJECT_NAME
    )

    # 配置端口转发（宿主机访问容器）- Docker Compose环境
    # 在Docker Compose中，端口转发通过docker-compose.yml文件配置
    # 这里只需要为Tilt UI配置资源监控
    dc_resource('mysql')
    dc_resource('ojp-ui')

    return dc, host_ip

def display_access_info(host_ip):
    """显示服务访问信息"""
    print("\n" + "="*70)
    print("✅ %s 开发环境已启动" % PROJECT_NAME)
    print("="*70)
    print("📌 服务访问信息:")
    print("  - MySQL容器: mysql:%d (用户名: root, 密码: root)" % MYSQL_PORT)
    print("  - UI容器: http://ui:%d" % UI_PORT)
    print("  - 本地后端服务: http://localhost:%d" % BACKEND_PORT)
    print("\n📌 容器内访问本地服务:")
    print("  - 所有容器可通过 http://backend:%d 访问IDE启动的服务" % BACKEND_PORT)
    print("\n📌 IDE配置指南:")
    print("  1. 确保Spring Boot配置:")
    print("     - 端口: %d" % BACKEND_PORT)
    print("     - 绑定地址: 0.0.0.0 (application.properties: server.address=0.0.0.0)")
    print("     - 数据库连接: jdbc:mysql://mysql:%d/myapp" % MYSQL_PORT)
    print("  2. 启动参数建议添加: -Dserver.address=0.0.0.0")
    print("="*70)

# 主执行流程
# 配置Docker服务和网络
dc, host_ip = configure_docker_services()

# 显示访问信息
display_access_info(host_ip)
