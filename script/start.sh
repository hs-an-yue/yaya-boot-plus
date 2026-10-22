#!/bin/bash

# ================= 基础配置 =================
APP_NAME="yaya-boot-plus-1.0.0.jar"
PROFILES="prod,prod-mysql"
LOG_FILE="./logs/app.log"

# JVM 参数 (根据服务器实际内存调整)
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# ================= 前置检查 =================
# 1. 检查 jar 包是否存在
if [ ! -f "$APP_NAME" ]; then
    echo "[ERROR] 未找到 $APP_NAME，请确认脚本与jar包在同一目录"
    exit 1
fi

# 2. 检查是否已在运行
PID=$(ps -ef | grep "$APP_NAME" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "[WARN] $APP_NAME 已在运行中 (PID: $PID)"
    exit 0
fi

# 3. 创建日志目录
mkdir -p "$(dirname "$LOG_FILE")"

# ================= 启动服务 =================
echo "[INFO] 正在启动 $APP_NAME ..."
echo "[INFO] 激活配置: $PROFILES"
echo "[INFO] 日志输出: $LOG_FILE"

nohup java $JVM_OPTS \
    -jar "$APP_NAME" \
    --spring.profiles.active=$PROFILES \
    > "$LOG_FILE" 2>&1 &

# 等待并验证启动
sleep 3
NEW_PID=$(ps -ef | grep "$APP_NAME" | grep -v grep | awk '{print $2}')

if [ -n "$NEW_PID" ]; then
    echo "[SUCCESS] 启动成功 (PID: $NEW_PID)"
    echo "[INFO] 查看日志: tail -f $LOG_FILE"
else
    echo "[ERROR] 启动失败，请检查日志: $LOG_FILE"
    tail -n 20 "$LOG_FILE"
    exit 1
fi