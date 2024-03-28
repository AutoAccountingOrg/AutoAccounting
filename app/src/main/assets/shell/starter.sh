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
echo "info: starter.sh begin"
PORT=52045
SERVER_NAME="自动记账"

get_pid(){
  netstat -tulnp 2>/dev/null | grep ":$PORT" | awk '$6 == "LISTEN" {print $7}' | cut -d'/' -f1 | head -n 1
}

ASSOCIATED_PIDS=$(ps -A | grep "auto_accounting_starter" | awk '{print $2}')
        # 结束所有关联的进程
        for pid in $ASSOCIATED_PIDS; do
            kill -9 "$pid"
            echo "info: Terminating process with PID $pid."
        done

# 获取CPU架构
CPU_ARCH=$(uname -m)

# 根据CPU架构选择二进制文件路径
SHELL_PATH=$(dirname "$0")
case "$CPU_ARCH" in
    "armv7l" | "armv8l")
        BINARY_PATH="armeabi-v7a"
        ;;
    "aarch64")
        BINARY_PATH="arm64-v8a"
        ;;
    "i386" | "i686")
        BINARY_PATH="x86"
        ;;
    "x86_64")
        BINARY_PATH="x86_64"
        ;;
    *)
        echo "fatal: unsupported this cpu: $CPU_ARCH"
        exit 2
        ;;
esac

OLD_PATH="$SHELL_PATH/$BINARY_PATH/auto_accounting_starter"

# 启动二进制文件
if [ -f "$OLD_PATH" ]; then
  TARGET_PATH="/data/local/tmp/autoAccount"
  # 检查文件夹是否存在
  if [ ! -d "$TARGET_PATH" ]; then
      # 文件夹不存在，创建文件夹
      mkdir -p "$TARGET_PATH/"
      echo "info：create dir $TARGET_PATH/"
  fi
  NEW_PATH="$TARGET_PATH/auto_accounting_starter"
  cp -r "$OLD_PATH" "$TARGET_PATH"
  echo "info: exec $NEW_PATH"
  chmod +x "$NEW_PATH"
    retries=0
    $NEW_PATH stop
    sleep 3
    $NEW_PATH start
    echo "info: waiting $SERVER_NAME service start... "
    while [ $retries -lt 120 ]; do
           PID=$(get_pid)
           if [ -n "$PID" ]; then
               echo "info: $SERVER_NAME service start success, PID: $PID"
               exit 0
           else
               sleep 2
               retries=$((retries+1))
           fi
       done
    echo "fatal: $SERVER_NAME service start failed after 10 attempts"
    exit 1
else
    echo "fatal: can't find binary file , please restart $SERVER_NAME: $OLD_PATH"
    exit 3
fi
