# 镜像名称: ojp-server:0.0.8-alpha
FROM eclipse-temurin:22-jre

# 创建应用目录
WORKDIR /app

# 复制构建好的JAR文件到容器中
COPY target/ojp-server-0.0.8-alpha-shaded.jar ojp-server.jar

# 暴露端口
EXPOSE 1059

# 设置启动命令，包含JVM参数和明确指定mainClass
ENTRYPOINT ["java", "-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG", "-cp", "ojp-server.jar", "org.openjdbcproxy.grpc.server.GrpcServer"]
