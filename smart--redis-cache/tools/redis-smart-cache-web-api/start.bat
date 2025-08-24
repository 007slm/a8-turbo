@echo off
chcp 65001 >nul
echo ==========================================
echo 🚀 Redis Smart Cache Web API 启动脚本
echo ==========================================

REM 设置Java环境
if "%JAVA_HOME%"=="" (
    echo ⚠️  警告: JAVA_HOME 未设置，使用系统默认Java
    set JAVA_CMD=java
) else (
    echo ✅ 使用Java: %JAVA_HOME%
    set JAVA_CMD=%JAVA_HOME%\bin\java
)

REM 检查Java版本
echo 🔍 检查Java版本...
%JAVA_CMD% -version

REM 设置应用参数
set APP_NAME=Redis Smart Cache Web API
set JAR_FILE=target\redis-smart-cache-web-api.jar
set MAIN_CLASS=com.redis.smartcache.webapi.RedisSmartCacheWebApiApplication

REM 设置JVM参数
set JVM_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

REM 设置环境变量（可通过系统环境变量覆盖）
if "%REDIS_HOST%"=="" set REDIS_HOST=localhost
if "%REDIS_PORT%"=="" set REDIS_PORT=6379
if "%REDIS_DATABASE%"=="" set REDIS_DATABASE=0
if "%REDIS_PASSWORD%"=="" set REDIS_PASSWORD=
if "%SMARTCACHE_APP_NAME%"=="" set SMARTCACHE_APP_NAME=smartcache
if "%SERVER_PORT%"=="" set SERVER_PORT=8080

echo 🔧 当前配置:
echo    Redis主机: %REDIS_HOST%
echo    Redis端口: %REDIS_PORT%
echo    Redis数据库: %REDIS_DATABASE%
echo    应用名称: %SMARTCACHE_APP_NAME%
echo    服务端口: %SERVER_PORT%

REM 检查是否需要编译
if not exist "%JAR_FILE%" (
    echo 📦 JAR文件不存在，开始编译...
    call mvnw.cmd clean package -DskipTests
    
    if errorlevel 1 (
        echo ❌ 编译失败，请检查错误信息
        pause
        exit /b 1
    )
    echo ✅ 编译完成
)

REM 启动应用
echo 🚀 启动 %APP_NAME%...
echo 📱 访问地址: http://localhost:%SERVER_PORT%
echo 📖 API文档: http://localhost:%SERVER_PORT%/swagger-ui.html
echo ❤️  健康检查: http://localhost:%SERVER_PORT%/actuator/health
echo.
echo 按 Ctrl+C 停止服务
echo ==========================================

REM 使用Maven Spring Boot插件启动（开发模式）
if "%1"=="dev" (
    echo 🔧 使用开发模式启动...
    call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
) else (
    REM 使用JAR文件启动（生产模式）
    %JAVA_CMD% %JVM_OPTS% -jar "%JAR_FILE%" %*
)

pause