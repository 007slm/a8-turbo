#!/bin/bash

echo "Starting OJP Server with unified HTTP + gRPC support..."

# 设置统一服务端口（HTTP + gRPC）
export OJP_UNIFIED_PORT=${OJP_UNIFIED_PORT:-8010}

# 设置 Prometheus 端口（默认 9090）
export OJP_PROMETHEUS_PORT=${OJP_PROMETHEUS_PORT:-9090}

# 启动服务器（单端口双协议）
java -Dserver.port=$OJP_UNIFIED_PORT \
     -Dojp.prometheus.port=$OJP_PROMETHEUS_PORT \
     -jar ojp-server-0.0.8-alpha.jar

echo "Server stopped."
