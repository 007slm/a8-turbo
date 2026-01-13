# 阶段 1: 构建前端
FROM node:18-alpine AS frontend-builder
WORKDIR /app
COPY docker/skywalking-ui/workdir/package*.json ./
ENV CYPRESS_INSTALL_BINARY=0
RUN npm install
COPY docker/skywalking-ui/workdir/ .
# 应用补丁 (覆盖原文件)
COPY docker/skywalking-ui/patch/ .
RUN npm run build && \
    if [ -d "dist/skywalking" ]; then \
    cp -r dist/skywalking/* dist/ && \
    rm -rf dist/skywalking; \
    fi

# 阶段 2: 运行时 (Nginx)
FROM nginx:alpine

# 复制自定义 Nginx 配置
COPY docker/skywalking-ui/nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制编译后的文件到 Nginx 默认目录
COPY --from=frontend-builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
