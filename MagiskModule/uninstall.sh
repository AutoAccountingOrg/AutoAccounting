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

# 定义要删除的文件夹路径
FOLDER_TO_REMOVE="/data/system/net.ankio.auto.xposed"

# 检查文件夹是否存在
if [ -d "$FOLDER_TO_REMOVE" ]; then
    # 查找文件夹内的所有文件和子文件夹，并逐个删除
    find "$FOLDER_TO_REMOVE" -type f -exec rm {} \;
    find "$FOLDER_TO_REMOVE" -type d -exec rm -r {} \;

    echo "Folder $FOLDER_TO_REMOVE and its contents have been safely removed."
else
    echo "Folder $FOLDER_TO_REMOVE does not exist. Nothing to remove."
fi
