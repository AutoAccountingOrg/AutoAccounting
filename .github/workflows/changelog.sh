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

# 从文件中读取版本号
VERSION=$(cat "${{github.workspace}}/package/tagVersionName.txt")
echo "# 下载地址" > $GITHUB_STEP_SUMMARY
channel=$1

if [ $channel == "Canary" ||  $channel == "Beta"  ]; then
  uri="https://github.com/${{ github.repository }}/actions/runs/${{ github.run_id }}"
  echo "- [Github Action](${uri})" >> $GITHUB_STEP_SUMMARY
else then
  uri="https://github.com/${{ github.repository }}/release"
  echo "- [Github Release](${uri})" >> $GITHUB_STEP_SUMMARY
fi

uri="https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/${channel}"
echo "- [自动记账网盘](${uri})" >> $GITHUB_STEP_SUMMARY

echo "# 更新日志" >> $GITHUB_STEP_SUMMARY
echo "- 版本：$VERSION" >> $GITHUB_STEP_SUMMARY
echo "- 时间：$(date +'%Y-%m-%d %H:%M:%S')" >> $GITHUB_STEP_SUMMARY
echo "" >> $GITHUB_STEP_SUMMARY
cat <<EOF > changelog.txt
  ${{ steps.github_release.outputs.changelog }}
EOF
cat changelog.txt >> $GITHUB_STEP_SUMMARY