@echo off
echo Testing OJP Server RESTful API...

REM 等待服务器启动
echo Waiting for server to start...
timeout /t 5 /nobreak >nul

REM 测试健康检查
echo Testing health check...
curl -s http://localhost:8010/actuator/health

REM 测试获取规则列表
echo.
echo Testing get rules...
curl -s http://localhost:8010/api/rules

echo.
echo API test completed.
pause
