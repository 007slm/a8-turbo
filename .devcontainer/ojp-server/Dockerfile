# 基础镜像
FROM eclipse-temurin:22-jdk

# 安装必要工具
RUN apt-get update && apt-get install -y \
  bash \
  curl \
  git \
  vim \
  maven \
  && rm -rf /var/lib/apt/lists/*

# 创建应用目录
WORKDIR /workspace

# 创建非root用户
RUN groupadd -r ojp && useradd -r -g ojp ojp

# 创建Maven目录并设置权限
RUN mkdir -p /home/ojp/.m2/repository && chown -R ojp:ojp /home/ojp

# 安装Node.js (用于前端开发)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs

# 清理
RUN apt-get clean

# 切换到非root用户
USER ojp

# 暴露端口
EXPOSE 8010

# 设置环境变量
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV MAVEN_OPTS="-Xmx1024m"