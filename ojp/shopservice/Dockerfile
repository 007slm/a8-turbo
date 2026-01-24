# 镜像名称: fluxdb-shopservice:0.0.7-alpha
# 使用本地构建的jar文件，避免Docker中的Maven构建问题
FROM eclipse-temurin:22-jre

# 创建应用目录
WORKDIR /app

# 安装基本工具(curl, ping等)用于开发测试
RUN apt-get update && apt-get install -y \
  curl \
  wget \
  telnet \
  net-tools \
  iputils-ping \
  dnsutils \
  && rm -rf /var/lib/apt/lists/*

# 创建非root用户
RUN groupadd -r shopservice && useradd -r -g shopservice shopservice

# 从本地复制Spring Boot可执行JAR文件
COPY target/shopservice-0.0.7-alpha.jar shopservice.jar

# 创建日志目录并设置权限
RUN mkdir -p /app/logs && chown -R shopservice:shopservice /app

# 切换到非root用户
USER shopservice

# 暴露端口
EXPOSE 8280

# 设置JVM参数
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8280/actuator/health || exit 1

# 设置Spring Boot标准启动命令，支持外部配置文件
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.config.location=file:/app/config/application.properties -Dorg.slf4j.simpleLogger.defaultLogLevel=INFO -jar shopservice.jar"]