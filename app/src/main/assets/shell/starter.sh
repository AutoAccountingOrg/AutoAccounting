#!/system/bin/sh

#
# Copyright (C) 2024 ankio(ankio@ankio.net)
# Licensed under the Apache License, Version 3.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-3.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#   limitations under the License.
#
DIR="$(cd "$(dirname "$0")" && pwd)"
PORT=52045
SERVER_NAME="自动记账"
info() {
    echo   " [INFO] $1 "
}

success() {
    echo  " [SUCCESS] $1"
}

warn() {
    echo  " [WARN] $1"
}

error() {
    echo   " [FATAL] $1"
}

get_pid(){
  netstat -tulnp 2>/dev/null | grep ":$PORT" | awk '$6 == "LISTEN" {print $7}' | cut -d'/' -f1 | head -n 1
}

info "$SERVER_NAME 启动脚本执行中...."
info "工作路径：$DIR"
ASSOCIATED_PIDS=$(ps -A | grep "auto_accounting_starter" | awk '{print $2}')
        # 结束所有关联的进程
        for pid in $ASSOCIATED_PIDS; do
            kill -9 "$pid"
            info "结束关联进程 PID $pid."
        done

# 获取CPU架构
CPU_ARCH=$(uname -m)

# 根据CPU架构选择二进制文件路径
SHELL_PATH=$(dirname "$0")
case "$CPU_ARCH" in
    "aarch64")
        BINARY_PATH="arm64-v8a"
        ;;
    "x86_64")
        BINARY_PATH="x86_64"
        ;;
    *)
        error "不支持的CPU架构: $CPU_ARCH"
        exit 2
        ;;
esac

OLD_PATH="$SHELL_PATH/$BINARY_PATH/auto_accounting_starter"

# 启动二进制文件
if [ -f "$OLD_PATH" ]; then
  TARGET_PATH="/data/local/tmp/autoAccount"
  rm -rf "$TARGET_PATH"
  mkdir -p "$TARGET_PATH/"
  info "正在创建工作文件夹 $TARGET_PATH/"
  NEW_PATH="$TARGET_PATH/auto_accounting_starter"
  cp -r "$OLD_PATH" "$TARGET_PATH"
  info "执行自动记账二进制文件 $NEW_PATH"
  chmod +x "$NEW_PATH"
  retries=0
  "$NEW_PATH" "$DIR" > "$SHELL_PATH/daemon.log" 2>&1 & #自动记账的工作路径就是自动记账的缓存路径
  info "等待 $SERVER_NAME 服务启动... "
  while [ $retries -lt 120 ]; do
     PID=$(get_pid)
     if [ -n "$PID" ]; then
         success "$SERVER_NAME 服务启动成功, PID: $PID"
         warn "若脚本未自动退出，请按Ctrl+C退出。"
         exit 0
     else
         sleep 2
         retries=$((retries+1))
         info "重试：$retries"
     fi
  done
  error "$SERVER_NAME 服务在多次尝试启动后仍未启动成功，请将该问题报告给自动记账开发团队，日志路径为：$SHELL_PATH/daemon.log。"
  exit 1
else
  error "未找到 $SERVER_NAME 服务二进制文件：$OLD_PATH"
  exit 3
fi
exit 0