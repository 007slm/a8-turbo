@echo off
echo Starting OJP Server with unified HTTP + gRPC support...

REM 设置统一服务端口（HTTP + gRPC）
set OJP_UNIFIED_PORT=8010

REM 设置 Prometheus 端口（默认 9090）
set OJP_PROMETHEUS_PORT=9090

REM 启动服务器（单端口双协议）
java -Dserver.port=%OJP_UNIFIED_PORT% ^
     -Dojp.prometheus.port=%OJP_PROMETHEUS_PORT% ^
     -jar ojp-server-0.0.8-alpha.jar

echo Server stopped.
pause
