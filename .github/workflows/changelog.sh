#!/bin/bash

# 从文件中读取版本号
VERSION=$(cat "${{github.workspace}}/package/tagVersionName.txt")
echo "# 下载地址" > "$GITHUB_STEP_SUMMARY"
channel=$1

if [ "$channel" = "Canary" ] || [ "$channel" = "Beta" ]; then
  uri="https://github.com/${{ github.repository }}/actions/runs/${{ github.run_id }}"
  echo "- [Github Action](${uri})" >> "$GITHUB_STEP_SUMMARY"
else
  uri="https://github.com/${{ github.repository }}/release"
  echo "- [Github Release](${uri})" >> "$GITHUB_STEP_SUMMARY"
fi

uri="https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/${channel}"
echo "- [自动记账网盘](${uri})" >> "$GITHUB_STEP_SUMMARY"

echo "# 更新日志" >> "$GITHUB_STEP_SUMMARY"
echo "- 版本：$VERSION" >> "$GITHUB_STEP_SUMMARY"
echo "- 时间：$(date +'%Y-%m-%d %H:%M:%S')" >> "$GITHUB_STEP_SUMMARY"
echo "" >> "$GITHUB_STEP_SUMMARY"

cat <<EOF > changelog.txt
${{ steps.github_release.outputs.changelog }}
EOF
cat changelog.txt >> "$GITHUB_STEP_SUMMARY"
