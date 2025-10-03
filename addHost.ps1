# 以管理员身份运行此PowerShell脚本
# 功能：在hosts文件中添加mysql和redis域名解析到127.0.0.1

# 定义hosts文件路径
$hostsPath = "$env:SystemRoot\System32\drivers\etc\hosts"

# 定义要添加的解析条目
$entries = @(
    "127.0.0.1    mysql",
    "127.0.0.1    redis",
    "127.0.0.1    starrocks",
    "127.0.0.1    shopservice"
)

# 读取现有hosts内容
$existingContent = Get-Content -Path $hostsPath -Raw

# 检查并添加条目
foreach ($entry in $entries) {
    $domain = $entry -split '\s+' | Select-Object -Last 1
    if (-not ($existingContent -match "\b$domain\b")) {
        Add-Content -Path $hostsPath -Value $entry
        Write-Host "已添加: $entry"
    } else {
        Write-Host "已存在: $domain 的解析，跳过"
    }
}

# 刷新DNS缓存
Write-Host "刷新DNS缓存..."
ipconfig /flushdns | Out-Null

Write-Host "操作完成"
