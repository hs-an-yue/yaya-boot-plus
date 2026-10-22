#!/bin/bash
# stop.sh
APP_NAME="yaya-boot-plus-1.0.0.jar"

# 查询进程PID
PID=$(ps -ef | grep "$APP_NAME" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "[INFO] $APP_NAME 未运行，无需停止"
    exit 0
fi

echo "[INFO] 准备停止 $APP_NAME , PID: $PID"
# 先优雅停机
kill -15 $PID
echo "[INFO] 等待进程优雅退出（最多等待10秒）"
WAIT_TIME=0
MAX_WAIT=10

while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    PID_CHECK=$(ps -ef | grep "$APP_NAME" | grep -v grep | awk '{print $2}')
    if [ -z "$PID_CHECK" ]; then
        echo "[SUCCESS] 服务已正常停止"
        exit 0
    fi
    sleep 1
    WAIT_TIME=$((WAIT_TIME+1))
done

# 超时强制kill
echo "[WARN] 优雅停机超时，强制 kill -9 $PID"
kill -9 $PID
echo "[SUCCESS] 强制停止完成"
