@echo off
REM 设置UTF-8代码页以避免中文乱码
chcp 65001 >nul

REM OJP 开发环境启动脚本
REM 用于启动开发环境的 docker-compose 配置

setlocal enabledelayedexpansion

REM 解析命令行参数
set BUILD=false
set CLEAN=false
set LOGS=false
set HELP=false
set COMPILE=false
set SYNC=false

:parse_args
if "%~1"=="" goto end_parse
if /i "%~1"=="-build" set BUILD=true
if /i "%~1"=="-clean" set CLEAN=true
if /i "%~1"=="-logs" set LOGS=true
if /i "%~1"=="-help" set HELP=true
if /i "%~1"=="-sync" set SYNC=true
if /i "%~1"=="--help" set HELP=true
if /i "%~1"=="/?" set HELP=true
if /i "%~1"=="-compile" set COMPILE=true
shift
goto parse_args
:end_parse

REM 显示帮助信息
if "%HELP%"=="true" (
    echo OJP 开发环境启动脚本
    echo 用法: start-dev.bat [选项]
    echo.
    echo 选项:
    echo   -build    重新构建镜像
    echo   -clean    启动前清理现有容器
    echo   -compile  使用 mvnd 编译 ojp 项目
    echo   -logs     显示服务日志
    echo   -help     显示此帮助信息
    echo.
    echo 示例:
    echo   start-dev.bat           启动开发环境
    echo   start-dev.bat -build    重新构建并启动
    echo   start-dev.bat -clean    清理并启动
    echo   start-dev.bat -compile  编译 ojp 项目
    echo   start-dev.bat -logs     显示日志
    goto end
)

REM 检查 Docker Compose 是否可用
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到 docker-compose 命令
    echo 请确保已安装 Docker Desktop 或 Docker Compose
    exit /b 1
)

REM 使用 mvnd 编译 ojp 项目
if "%COMPILE%"=="true" (
    echo 正在使用 mvnd 编译 ojp 项目...
    cd ojp
    mvnd install -DskipTests
    if errorlevel 1 (
        echo 编译失败，请检查错误信息
        exit /b 1
    )
    echo 编译成功完成!
    cd ..
    goto end
)

REM 构建 Docker Compose 参数
set COMPOSE_ARGS=-f docker-compose.yml -f docker-compose-cdc-sync.yml -f .devcontainer/backend/docker-compose.yml -f .devcontainer/frontend/docker-compose.yml  -f docker-compose-shopservice.yml -f docker-compose-kong.yml   --profile dev

REM 显示服务日志
if "%LOGS%"=="true" (
    echo 正在显示服务日志...
    docker-compose %COMPOSE_ARGS% logs -f ojp-server ojp-ui
    goto end
)

echo 正在启动 OJP 开发环境...

REM 清理现有容器
if "%CLEAN%"=="true" (
    echo 正在清理现有容器...
    docker-compose %COMPOSE_ARGS% down --remove-orphans
    docker system prune -f
)

REM 重新构建镜像
if "%BUILD%"=="true" (
    echo 正在重新构建镜像...
    docker-compose %COMPOSE_ARGS% build --no-cache
    if errorlevel 1 (
        echo 构建失败，请检查错误信息
        exit /b 1
    )
    echo 镜像构建完成
)

REM 启动服务
echo 正在启动服务...
docker-compose %COMPOSE_ARGS% up -d

if errorlevel 1 (
    echo 启动失败，请检查错误信息
    exit /b 1
)

echo 开发环境启动成功!
echo.
echo 服务 URLs:
echo   前端开发服务器: http://localhost:5173
echo   后端 API 服务: http://localhost:8010
echo   Kong 网关:        http://localhost:8000
echo.
echo 常用命令:
echo   查看日志: start-dev.bat -logs
echo   停止服务: stop-dev.bat
echo   编译 ojp: start-dev.bat -compile
echo.

REM 等待服务启动
echo 正在等待服务启动...
timeout /t 5 /nobreak >nul

REM 检查服务状态
echo 服务状态:
docker-compose %COMPOSE_ARGS% ps

:end
endlocal