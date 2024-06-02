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

PORT=52045
get_pid(){
  netstat -tulnp 2>/dev/null | grep ":$PORT" | awk '$6 == "LISTEN" {print $7}' | cut -d'/' -f1 | head -n 1
}
PID=$(get_pid)
if [ -n "$PID" ]; then
    kill -9 "$PID"
    echo " [INFO] 关闭进程 PID $PID."
else
    echo " [INFO] 未找到进程."
fi
