@echo off
REM 设置UTF-8代码页以避免中文乱码
chcp 65001 >nul

REM OJP 生产环境启动脚本
REM 1. 使用 Maven 构建所有必要的 JAR 文件
REM 2. 使用 Docker Compose 启动生产环境

setlocal enabledelayedexpansion

REM 解析命令行参数
set BUILD_MAVEN=true
set BUILD_DOCKER=false
set CLEAN=false
set LOGS=false
set HELP=false
set DOWN=false
set SKIP_MAVEN=true
set DEBUG=false

:parse_args
if "%~1"=="" goto end_parse
if /i "%~1"=="-build-maven" set SKIP_MAVEN=false
if /i "%~1"=="-build" set BUILD_DOCKER=true
if /i "%~1"=="-clean" set CLEAN=true
if /i "%~1"=="-logs" set LOGS=true
if /i "%~1"=="-down" set DOWN=true
if /i "%~1"=="-debug" set DEBUG=true
if /i "%~1"=="-help" set HELP=true
if /i "%~1"=="--help" set HELP=true
if /i "%~1"=="/?" set HELP=true
shift
goto parse_args
:end_parse

REM 启用调试模式
if "%DEBUG%"=="true" (
    echo [DEBUG] 调试模式已启用
    @echo on
)

if not "%HELP%"=="true" goto end_help
echo OJP 开发环境启动脚本
echo 用法: dev.bat [选项]
echo.
echo 选项:
echo   -build-maven 强制在宿主机本地构建 Maven
echo   -build       重新构建 Docker 镜像
echo   -clean       启动前清理现有容器
echo   -down        停止并删除所有容器
echo   -logs        显示服务日志
echo   -debug       启用调试模式
echo   -help        显示此帮助信息
echo.
echo 示例:
echo   dev.bat              启动开发环境 (容器内动态编译运行)
echo   dev.bat -build-maven 强制在宿主机本地构建 Maven
echo   dev.bat -build       重新构建 Docker 镜像
echo   dev.bat -clean       清理并重新启动
echo   dev.bat -down        停止所有服务
echo   dev.bat -logs        显示日志
echo   dev.bat -debug       调试模式启动
goto end
:end_help

REM 停止服务
if "%DOWN%"=="true" (
    echo 正在停止开发环境服务...
    docker compose -f docker-compose.yml -f docker-compose-dev.yml down --remove-orphans
    echo 服务已停止
    goto end
)

REM 显示服务日志
if "%LOGS%"=="true" (
    echo 正在显示服务日志...
    docker compose -f docker-compose.yml -f docker-compose-dev.yml logs -f ojp-server shopservice ojp-ui
    goto end
)

echo ========================================
echo   OJP Dev Environment Startup Script
echo ========================================
echo.

REM Clean up residual processes occupying backend ports
echo Checking and clearing processes on backend ports (8010, 8330, 8280)...
for %%p in (8010 8330 8280) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr /r /c:":%%p "') do (
        echo Found port %%p occupied by process PID:%%a, killing it...
        taskkill /f /fi "IMAGENAME eq java.exe" /pid %%a >nul 2>&1
    )
)
echo Port check and cleanup completed.
echo.

REM Check if Docker CLI is available
docker compose version >nul 2>&1
if errorlevel 1 (
    echo ERROR: docker compose command not found.
    echo Please make sure Docker Desktop is installed and added to PATH.
    exit /b 1
)

REM Check if Docker Daemon is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker Desktop daemon is not running.
    echo Please start Docker Desktop and wait until it is fully ready, then try again.
    exit /b 1
)

REM 步骤 1: 使用 Maven 构建 JAR 文件
if "%SKIP_MAVEN%"=="false" (
    echo [1/3] 正在构建 JAR 文件...
    echo.
    
    REM 步骤 0.9: 构建 ojp-cache
    echo [0.9] 构建 ojp-cache...
    cd ojp\ojp-cache
    call mvnd clean install -DskipTests
    if errorlevel 1 (
        echo 错误: ojp-cache 构建失败
        cd ..\..
        exit /b 1
    )
    cd ..\..
    echo ✓ ojp-cache 构建成功
    echo.

    REM 步骤 1.0: 构建 ojp-server-common
    echo [1.0] 构建 ojp-server-common...
    cd ojp\ojp-server-common
    call mvnd clean install -DskipTests
    if errorlevel 1 (
        echo 错误: ojp-server-common 构建失败
        cd ..\..
        exit /b 1
    )
    cd ..\..
    echo ✓ ojp-server-common 构建成功
    echo.

    REM 步骤 1.1: 构建 ojp-grpc-commons
    echo [1.1] 构建 ojp-grpc-commons...
    cd ojp\ojp-grpc-commons
    call mvnd clean install -DskipTests
    if errorlevel 1 (
        echo 错误: ojp-grpc-commons 构建失败
        cd ..\..
        exit /b 1
    )
    cd ..\..
    echo ✓ ojp-grpc-commons 构建成功
    echo.
    
    REM 步骤 1.2: 构建 ojp-jdbc-driver
    echo [1.2] 构建 ojp-jdbc-driver...
    cd ojp\ojp-jdbc-driver
    call mvnd clean install -DskipTests
    if errorlevel 1 (
        echo 错误: ojp-jdbc-driver 构建失败
        cd ..\..
        exit /b 1
    )
    cd ..\..
    echo ✓ ojp-jdbc-driver 构建成功
    echo.
    
    REM 步骤 1.3: 构建 ojp-server
    echo [1.3] 构建 ojp-server...
    cd ojp\ojp-server
    copy /y .\src\main\resources\application.yml ..\..\docker\ojp\server\
    call mvnd clean package -DskipTests
    if errorlevel 1 (
        echo 错误: ojp-server 构建失败
        cd ..\..
        exit /b 1
    )
    cd ..\..
    echo ✓ ojp-server 构建成功
    echo.
    
    REM 步骤 1.4: 构建 shopservice
    echo [1.4] 构建 shopservice...
    cd ojp\shopservice
    copy /y .\src\main\resources\application.properties ..\..\docker\ojp\shopservice\
    call mvnd clean package -DskipTests
    if errorlevel 1 (
        echo 警告: shopservice 构建失败
        set SKIP_SHOPSERVICE=true
    ) else (
        echo ✓ shopservice 构建成功
    )
    cd ..\..
    echo.
    
    echo ========================================
    echo   所有 JAR 文件构建完成
    echo ========================================
    echo.
) else (
    echo [1/3] 跳过 Maven 构建步骤
    echo.
)

REM 清理现有容器
if "%CLEAN%"=="true" (
    echo [2/3] 正在清理现有容器...
    docker compose -f docker-compose-prod.yml down --remove-orphans
    docker compose -f docker-compose.yml -f docker-compose-dev.yml down --remove-orphans
    docker system prune -f
    echo ✓ 清理完成
    echo.
) else (
    echo [2/3] 跳过清理步骤
    echo.
)

REM 步骤 2: 启动 Docker Compose
echo [3/3] 正在启动 Docker 容器...
echo.

if "%BUILD_DOCKER%"=="true" (
    echo 重新构建 Docker 镜像...
    docker compose -f docker-compose-prod.yml build --no-cache
    if errorlevel 1 (
        echo 错误: Docker 镜像构建失败
        exit /b 1
    )
    echo ✓ 镜像构建完成
    echo.
)

echo 停止旧容器并清理环境...
REM 尝试停止生产环境容器（如果存在）
@REM docker compose -f docker-compose-prod.yml down --remove-orphans
REM 尝试停止开发环境容器（如果存在）
@REM docker compose -f docker-compose.yml -f docker-compose-dev.yml down --remove-orphans

echo Starting services...
docker compose -f docker-compose.yml -f docker-compose-dev.yml up -d

echo Starting backend services on host via Maven...
start "OJP Server" /D ojp\ojp-server cmd /k chcp 65001 >nul ^& mvn spring-boot:run -Dspring-boot.run.arguments=--spring.data.redis.host=127.0.0.1

echo Waiting 20s for OJP Server to initialize before starting Shop Service...
timeout /t 20 /nobreak >nul

start "Shop Service" /D ojp\shopservice cmd /k chcp 65001 >nul ^& mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:ojp[127.0.0.1:8330]_mysql://127.0.0.1:3306/shopdb --chinook.datasource.url=jdbc:ojp[127.0.0.1:8330]_mysql://127.0.0.1:3306/Chinook"

echo %cd%
pnpm -C ojp/ojp-ui dev

if errorlevel 1 (
    echo ERROR: Service startup failed
    exit /b 1
)

echo.
echo ========================================
echo   开发环境容器模式启动成功
echo ========================================
echo.
echo 服务 URLs:
echo   OJP Server:    http://localhost:8010
echo   Shop Service:  http://localhost:8280
echo   Kong Gateway:  http://localhost:8000
echo   Grafana:       http://localhost:3000/grafana
echo   SkyWalking UI: http://localhost:8080
echo.
echo 常用命令:
echo   查看日志:   dev.bat -logs
echo   停止服务:   dev.bat -down
echo   重新构建:   dev.bat -build
echo   查看状态:   docker compose -f docker-compose.yml -f docker-compose-dev.yml ps
echo.

REM 等待服务启动
echo 正在等待服务启动...
timeout /t 5 /nobreak >nul

REM 检查服务状态
echo 服务状态:
docker compose -f docker-compose.yml -f docker-compose-dev.yml ps

:end
endlocal
