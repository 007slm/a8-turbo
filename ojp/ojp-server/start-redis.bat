@echo off
echo ========================================
echo OJP Server Redis 启动脚本
echo ========================================
echo.

echo 检查 Redis 服务状态...
sc query Redis >nul 2>&1
if %errorlevel% equ 0 (
    echo Redis 服务已安装，正在启动...
    net start Redis
    if %errorlevel% equ 0 (
        echo Redis 服务启动成功！
        echo 正在测试连接...
        timeout /t 2 >nul
        powershell -Command "Test-NetConnection -ComputerName localhost -Port 6379" >nul 2>&1
        if %errorlevel% equ 0 (
            echo Redis 连接测试成功！
        ) else (
            echo Redis 连接测试失败，请检查服务状态
        )
    ) else (
        echo Redis 服务启动失败！
        echo 请以管理员身份运行此脚本
    )
) else (
    echo Redis 服务未安装，尝试直接启动 Redis 服务器...
    echo.
    echo 请确保已安装 Redis for Windows
    echo 下载地址: https://github.com/microsoftarchive/redis/releases
    echo.
    echo 如果已安装，请手动运行: redis-server
    echo.
    pause
)
