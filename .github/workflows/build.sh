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
#
rootDir=$(pwd)/../../
# 配置版本号和版本名
versionName=$(grep 'versionName' "${rootDir}/app/build.gradle" | sed -n 's/.*versionName "\([^"]*\)".*/\1/p' | tr -d '[:space:]')
versionCode=$(grep 'versionCode' "${rootDir}/app/build.gradle" |  awk '{print $2}' | tr -d '[:space:]')
echo "VERSION_NAME=${versionName}" >> $GITHUB_ENV
echo "VERSION_CODE=${versionCode}" >> $GITHUB_ENV
# 设置标签名
echo "TAG_VERSION_NAME=v${versionName}-commit-$(date +'%Y%m%d%H%M%S')" >> $GITHUB_ENV
# 配置运行权限
chmod +x "${rootDir}/gradlew"
 # 创建release目录
mkdir "${rootDir}/release/"
# 构建二进制文件
"${rootDir}/gradlew" app:buildCMakeRelWithDebInfo
 # 优先构建xposed版本
for flavor in xposed ; do
    "${rootDir}/gradlew" "assemble${flavor^}Release"
    cp "${rootDir}/app/build/outputs/apk/${flavor}/release/app.apk" "${rootDir}/release/app-${flavor}.apk"
done

