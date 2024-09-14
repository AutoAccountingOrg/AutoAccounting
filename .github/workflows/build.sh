#!/bin/bash
# Copyright (C) 2024 ankio(ankio@ankio.net)
# Licensed under the Apache License, Version 3.0 (the "License");
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

# 获取当前目录路径
rootDir=$(pwd)

# 获取最近的 tag
latestTag=$(git describe --tags --abbrev=0)

# 检查从最近的 tag 到当前是否有新的提交
if [[ -z $(git log ${latestTag}..HEAD --oneline) ]]; then
  echo "没有从最近的 tag ${latestTag} 到当前的提交，终止编译。"
  exit 0
fi

echo "检测到从最近的 tag ${latestTag} 到当前的提交，继续编译..."

# 构建渠道
channel=$1

# 配置版本号和版本名
versionName=$(grep 'versionName' "${rootDir}/app/build.gradle" | sed -n 's/.*versionName "\([^"]*\)".*/\1/p')
versionCode=$(grep 'versionCode' "${rootDir}/app/build.gradle" | sed -n 's/.*versionCode \([0-9]*\).*/\1/p')

# 设置标签名，使用 '+' 分隔构建元数据
tagVersionName="${versionName}-${channel}.$(date +'%Y%m%d%H%M%S')"

# 输出结果到 GitHub Actions 环境
echo "tag_version_name=v${tagVersionName}" >> $GITHUB_OUTPUT
echo "${tagVersionName}" >> "${GITHUB_WORKSPACE}/tagVersionName.txt"
echo "${versionCode}" >> "${GITHUB_WORKSPACE}/versionCode.txt"

# 配置运行权限
chmod +x "${rootDir}/gradlew"

# 创建 release 目录
mkdir -p "${rootDir}/release/"


# 优先构建 xposed 版本
for flavor in xposed ; do
  "${rootDir}/gradlew" "assemble${flavor^}Release"
  cp "${rootDir}/app/build/outputs/apk/${flavor}/release/app.apk" "${rootDir}/release/app-${flavor}.apk"
done


# 检查编译是否成功
if [ $? -eq 0 ]; then
  git tag "CanaryBuild-${tagVersionName}"
  git push origin "CanaryBuild-${tagVersionName}"
else
  echo "编译失败，退出..."
  exit 1
fi

