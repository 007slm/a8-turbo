#!/bin/bash

# Flink CDC作业初始化脚本
# 该脚本会检查Flink集群是否就绪，然后提交CDC作业

echo "开始Flink CDC作业初始化..."

# 检查Flink JobManager是否就绪
echo "检查Flink集群状态..."
MAX_WAIT=60
WAIT_COUNT=0

while true; do
    # 使用curl检查Flink Web UI是否可访问
    if curl -s http://jobmanager:8081/ > /dev/null; then
        echo "Flink集群已就绪"
        break
    fi
    
    WAIT_COUNT=$((WAIT_COUNT + 1))
    if [ $WAIT_COUNT -gt $MAX_WAIT ]; then
        echo "错误: Flink集群启动超时"
        exit 1
    fi
    
    echo "等待Flink集群启动... (${WAIT_COUNT}/${MAX_WAIT})"
    sleep 2
done

# 提交ojp-sync作业
echo "提交ojp-sync作业..."
/opt/flink/bin/flink run -m jobmanager:8081 -c io.a8.sync.MySQLToStarRocksSync \
  /opt/flink-cdc/lib/ojp-sync-1.0-SNAPSHOT.jar \
  /opt/flink_jobs/sync-config.yaml

if [ $? -eq 0 ]; then
    echo "Flink CDC作业提交成功"
else
    echo "错误: Flink CDC作业提交失败"
    exit 1
fi
echo "Flink CDC初始化完成"