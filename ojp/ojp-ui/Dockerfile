# 使用 Node.js 18 Alpine 镜像
FROM node:18-alpine

# 安装开发工具 (git, github-cli, bash)
RUN apk add --no-cache bash git github-cli

# 升级npm到与Node.js 18兼容的版本以支持lockfileVersion 3
RUN npm install -g npm@10

# 设置工作目录
WORKDIR /app

# 复制 package 文件
COPY package*.json ./

# 安装依赖
RUN npm ci

# 复制源代码
COPY . .

# 暴露端口
EXPOSE 3000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3000/ || exit 1

# 启动 Vite 开发服务器
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--config", "/app/vite.config.js"]