#!/bin/bash

# Redis Smart Cache Web API 启动脚本
# 这个脚本用于快速启动Redis Smart Cache Web API服务

echo "=========================================="
echo "🚀 Redis Smart Cache Web API 启动脚本"
echo "=========================================="

# 设置Java环境
JAVA_HOME=${JAVA_HOME:-}
if [ -z "$JAVA_HOME" ]; then
    echo "⚠️  警告: JAVA_HOME 未设置，使用系统默认Java"
    JAVA_CMD="java"
else
    JAVA_CMD="$JAVA_HOME/bin/java"
    echo "✅ 使用Java: $JAVA_HOME"
fi

# 检查Java版本
echo "🔍 检查Java版本..."
$JAVA_CMD -version 2>&1 | head -1

# 设置应用参数
APP_NAME="Redis Smart Cache Web API"
JAR_FILE="target/redis-smart-cache-web-api.jar"
MAIN_CLASS="com.redis.smartcache.webapi.RedisSmartCacheWebApiApplication"

# 设置JVM参数
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 设置环境变量（可通过命令行参数覆盖）
export REDIS_HOST=${REDIS_HOST:-localhost}
export REDIS_PORT=${REDIS_PORT:-6379}
export REDIS_DATABASE=${REDIS_DATABASE:-0}
export REDIS_PASSWORD=${REDIS_PASSWORD:-}
export SMARTCACHE_APP_NAME=${SMARTCACHE_APP_NAME:-smartcache}
export SERVER_PORT=${SERVER_PORT:-8080}

echo "🔧 当前配置:"
echo "   Redis主机: $REDIS_HOST"
echo "   Redis端口: $REDIS_PORT"
echo "   Redis数据库: $REDIS_DATABASE"
echo "   应用名称: $SMARTCACHE_APP_NAME"
echo "   服务端口: $SERVER_PORT"

# 检查是否需要编译
if [ ! -f "$JAR_FILE" ]; then
    echo "📦 JAR文件不存在，开始编译..."
    ./mvnw clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        echo "❌ 编译失败，请检查错误信息"
        exit 1
    fi
    echo "✅ 编译完成"
fi

# 启动应用
echo "🚀 启动 $APP_NAME..."
echo "📱 访问地址: http://localhost:$SERVER_PORT"
echo "📖 API文档: http://localhost:$SERVER_PORT/swagger-ui.html"
echo "❤️  健康检查: http://localhost:$SERVER_PORT/actuator/health"
echo ""
echo "按 Ctrl+C 停止服务"
echo "=========================================="

# 使用Maven Spring Boot插件启动（开发模式）
if [ "$1" = "dev" ]; then
    echo "🔧 使用开发模式启动..."
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
else
    # 使用JAR文件启动（生产模式）
    $JAVA_CMD $JVM_OPTS -jar "$JAR_FILE" "$@"
fi