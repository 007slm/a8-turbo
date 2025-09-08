@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
cls

echo.
echo ==============================================
echo          OJP项目本地构建脚本
echo ==============================================
echo.

:: 1. 检查Maven是否安装
echo [步骤 1/9] 正在检查Maven安装状态...
call mvn -v >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo 错误: Maven未安装！请先安装Maven。
    echo 提示: 确保"mvn"命令已添加到系统环境变量"Path"中。
    pause
    exit /b 1
)
echo Maven已安装。

:: 2. 检查Java是否安装
echo.
echo [步骤 2/9] 正在检查Java安装状态...
call java -version >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo 错误: Java未安装！请先安装Java。
    echo 提示: 确保"java"命令已添加到系统环境变量"Path"中。
    pause
    exit /b 1
)
echo Java已安装。

:: 检查Docker是否安装
echo.
echo [步骤 3/9] 正在检查Docker安装状态...
call docker --version >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo 错误: Docker未安装或未运行！
    echo 提示: 请安装Docker并确保其正在运行。
    pause
    exit /b 1
)
echo Docker已安装。

:: 检查Docker Compose是否安装
echo.
echo [步骤 4/9] 正在检查Docker Compose安装状态...
call docker-compose --version >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo 错误: Docker Compose未安装！
    echo 提示: 请安装Docker Compose。
    pause
    exit /b 1
)
echo Docker Compose已安装。

:: 显示环境信息
echo.
echo [步骤 5/9] 显示环境信息...
echo.
echo ---------------- Maven版本 ----------------
call mvn -v
echo.
echo ---------------- Java版本 -----------------
call java -version
echo.
echo ---------------- Docker版本 ----------------
call docker --version
echo.
echo ---------------- Docker Compose版本 ----------------
call docker-compose --version
echo.

:: 进入项目根目录（脚本所在目录）
echo [步骤 6/9] 导航到项目根目录...
cd /d %~dp0
echo 当前工作目录: %cd%
echo.

:: 清理并构建项目
echo [步骤 7/9] 清理之前的构建并开始新构建...
echo 这可能需要几分钟时间...
echo.
call mvn clean package -DskipTests -Dmaven.javadoc.skip=true
if %errorLevel% neq 0 (
    echo.
    echo 错误: 项目构建失败！请检查上述错误日志。
    pause
    exit /b 1
)
echo.
echo Maven构建过程成功完成！
echo.

:: 验证JAR文件是否存在
set "jarPath=ojp-server\target\ojp-server-0.0.8-alpha.jar"
if not exist "%jarPath%" (
    echo 错误: 在 %jarPath% 位置未找到JAR文件
    echo Maven构建可能失败
    pause
    exit /b 1
)

echo JAR文件验证通过: %jarPath%
echo 文件大小: 
for %%A in ("%jarPath%") do echo %%~zA 字节
echo.

:: 验证shopservice JAR文件是否存在
set "shopJarPath=shopservice\target\shopservice-0.0.7-alpha.jar"
if not exist "%shopJarPath%" (
    echo 错误: 在 %shopJarPath% 位置未找到JAR文件
    echo Maven构建可能失败
    pause
    exit /b 1
)

echo JAR文件验证通过: %shopJarPath%
echo 文件大小: 
for %%A in ("%shopJarPath%") do echo %%~zA 字节
echo.

echo ==============================================
echo Maven构建完成。开始Docker构建...
echo ==============================================
echo.

:: 构建所有Docker镜像（利用缓存优化）
echo [步骤 8/9] 正在构建Docker镜像...
echo.
cd ..

:: 构建ojp-server镜像
echo 构建ojp-server镜像...
call docker-compose -f docker-compose.yml build --build-arg BUILDKIT_INLINE_CACHE=1 ojp-server
if %errorLevel% neq 0 (
    echo.
    echo 错误: ojp-server镜像构建失败！
    echo 请检查上述错误信息。
    pause
    exit /b 1
)
echo ojp-server镜像构建成功！

:: 构建ojp-ui镜像
echo 构建ojp-ui镜像...
call docker-compose -f docker-compose.yml build --build-arg BUILDKIT_INLINE_CACHE=1 ojp-ui
if %errorLevel% neq 0 (
    echo.
    echo 错误: ojp-ui镜像构建失败！
    echo 请检查上述错误信息。
    pause
    exit /b 1
)
echo ojp-ui镜像构建成功！

:: 构建shopservice镜像
echo 构建shopservice镜像...
call docker-compose -f docker-compose.yml build --build-arg BUILDKIT_INLINE_CACHE=1 shopservice
if %errorLevel% neq 0 (
    echo.
    echo 错误: shopservice镜像构建失败！
    echo 请检查上述错误信息。
    pause
    exit /b 1
)
echo shopservice镜像构建成功！

echo.
echo 所有Docker镜像构建成功完成！
echo.

:: 使用docker-compose启动服务
echo [步骤 9/9] 正在启动OJP相关服务...
call docker-compose -f docker-compose.yml up -d
if %errorLevel% neq 0 (
    echo.
    echo 错误: 服务启动失败！
    echo 请检查上述错误信息。
    pause
    exit /b 1
)

echo.
echo ==============================================
echo 所有服务启动成功！
echo ==============================================
echo.
echo OJP服务状态:
call docker-compose -f docker-compose.yml ps
echo.
echo 构建和部署成功完成！
echo 现在可以访问相关服务。
pause