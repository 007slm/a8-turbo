# 镜像名称: fluxdb-server:0.0.8-alpha
# 使用本地构建的jar文件，避免Docker中的Maven构建问题
FROM eclipse-temurin:22-jre

# 创建应用目录
WORKDIR /app

# 创建非root用户
RUN groupadd -r ojp && useradd -r -g ojp ojp

# 从本地复制Spring Boot可执行JAR文件
COPY ojp-server/target/ojp-server-0.0.8-alpha.jar ojp-server.jar

# 创建日志目录并设置权限
RUN mkdir -p /app/logs && chown -R ojp:ojp /app

# 切换到非root用户
USER ojp

# 暴露端口 (修正为8010，统一HTTP+gRPC端口)
EXPOSE 8010

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8010/api/actuator/health || exit 1

# 设置Spring Boot标准启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.config.location=file:/app/config/application.yml -Dorg.slf4j.simpleLogger.defaultLogLevel=INFO -jar ojp-server.jar"]