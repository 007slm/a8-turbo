# 构建阶段
FROM openjdk:22-jdk-slim AS build

# 安装Maven
RUN apt-get update && \
    apt-get install -y wget && \
    wget https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz && \
    tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt && \
    ln -s /opt/apache-maven-3.9.6 /opt/maven && \
    rm apache-maven-3.9.6-bin.tar.gz && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置Maven环境变量
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH
ENV MAVEN_OPTS -Dmaven.repo.local=/root/.m2/repository


# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY . .

# 构建项目（使用缓存挂载Maven仓库）
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -DskipTests

# 第二阶段：运行时镜像
FROM openjdk:22-jdk-slim

# 安装常用工具
RUN apt-get update && \
    apt-get install -y \
        curl \
        wget \
        vim \
        telnet \
        net-tools \
        iputils-ping \
        dnsutils \
        procps \
        lsof \
        strace \
        tcpdump && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 创建非root用户
RUN groupadd -r shopservice && useradd -r -g shopservice shopservice

# 从构建阶段复制jar文件
COPY --from=build /app/target/*.jar app.jar

# 创建日志目录并设置权限
RUN mkdir -p /app/logs && chown -R shopservice:shopservice /app

# 切换到非root用户
USER shopservice

# 暴露端口
EXPOSE 8180 5005

# 设置JVM参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=docker"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8180/actuator/health || exit 1

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]