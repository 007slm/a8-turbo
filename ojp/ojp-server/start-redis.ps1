# OJP Server Redis 启动脚本
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "OJP Server Redis 启动脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查是否以管理员身份运行
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Host "警告: 此脚本需要管理员权限才能启动 Redis 服务" -ForegroundColor Yellow
    Write-Host "请以管理员身份运行 PowerShell" -ForegroundColor Yellow
    Write-Host ""
}

# 检查 Redis 服务状态
Write-Host "检查 Redis 服务状态..." -ForegroundColor Green
$redisService = Get-Service -Name "Redis" -ErrorAction SilentlyContinue

if ($redisService) {
    Write-Host "Redis 服务已安装" -ForegroundColor Green
    
    if ($redisService.Status -eq "Running") {
        Write-Host "Redis 服务已在运行中" -ForegroundColor Green
    } else {
        Write-Host "正在启动 Redis 服务..." -ForegroundColor Yellow
        try {
            Start-Service -Name "Redis"
            Start-Sleep -Seconds 2
            $redisService = Get-Service -Name "Redis"
            if ($redisService.Status -eq "Running") {
                Write-Host "Redis 服务启动成功！" -ForegroundColor Green
            } else {
                Write-Host "Redis 服务启动失败！" -ForegroundColor Red
            }
        } catch {
            Write-Host "启动 Redis 服务时发生错误: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    # 测试连接
    Write-Host "正在测试 Redis 连接..." -ForegroundColor Green
    try {
        $connection = Test-NetConnection -ComputerName localhost -Port 6379 -InformationLevel Quiet
        if ($connection) {
            Write-Host "Redis 连接测试成功！" -ForegroundColor Green
            Write-Host "Redis 服务已准备就绪，可以启动 OJP Server" -ForegroundColor Green
        } else {
            Write-Host "Redis 连接测试失败" -ForegroundColor Red
        }
    } catch {
        Write-Host "连接测试失败: $($_.Exception.Message)" -ForegroundColor Red
    }
    
} else {
    Write-Host "Redis 服务未安装" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "请按照以下步骤安装 Redis:" -ForegroundColor Yellow
    Write-Host "1. 下载 Redis for Windows: https://github.com/microsoftarchive/redis/releases" -ForegroundColor Yellow
    Write-Host "2. 解压到 C:\Redis" -ForegroundColor Yellow
    Write-Host "3. 以管理员身份运行: redis-server --service-install redis.windows.conf" -ForegroundColor Yellow
    Write-Host "4. 启动服务: net start Redis" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "或者直接运行: redis-server" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
