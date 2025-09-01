#!/usr/bin/env python3
import datetime
import json
import os
import re
import subprocess
import sys
from urllib.parse import quote
import md2tgmd
import requests

flavors = ['release']  # 项目只有一个标准构建版本

def get_latest_tag_with_prefix(prefix):
    print(f"获取最新的 tag: {prefix}")

    # 获取所有标签，按照日期排序
    result = subprocess.run(
        ['git', 'for-each-ref', '--sort=taggerdate', '--format', "%(refname:short)", 'refs/tags'],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )
    tags = result.stdout.strip().split('\n')

    # 根据 channel 设置正则表达式
    if prefix == 'Stable':
        pattern = r"^v\d+\.\d+\.\d+$"  # 用于匹配 v2.0.0 形式的标签
    else:
        pattern = rf"\d+\.\d+\.\d+-{re.escape(prefix)}\.\d{{8}}_\d{{4}}"

    # 逆序查找匹配的标签
    for tag in reversed(tags):
        tag = tag.split(' ')[0]
        match = re.match(pattern, tag)
        if match:
            return tag

    # 如果没有找到匹配的标签，则返回最后一个标签
    return tags[-1] if tags else None


def get_changed_files_since_tag(tag):
    # 执行 git diff 命令获取自指定 tag 以来的变更文件
    result = subprocess.run(
        ['git', 'diff', '--name-only', f'{tag}..HEAD'],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    # 将输出的文件路径按行分割
    changed_files = result.stdout.strip().split('\n')

    # 判断是否有文件路径以 'server' 开头
    for file in changed_files:
        if file.startswith('server') or file.startswith('app/src/lsposed/java/net/ankio/auto/hooks/android'):
            return True

    return False
def get_commits_since_tag(tag):
    result = subprocess.run(['git', 'log', f'{tag}..HEAD', '--oneline'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    commits = result.stdout.strip().split('\n')
    # 对每一个commit进行处理
    # 删除 hash，提取emoji和message
    commitItems  = []
    for commit in commits:
        commit = commit[8:].strip()
        # 提取emoji
        emoji = re.search(r'^:(\w+):', commit)
        if emoji:
            emoji = emoji.group(1)
        else:
            continue

        # 提取message
        message = re.sub(r'^\s*([^ ]+)\s', '', commit)
        commitItems.append({
            "emoji": emoji,
            "message": message
        })
    return commitItems

def get_and_set_version(channel,workspace):
    with open(workspace + '/app/build.gradle.kts') as file:
        content = file.read()
    versionName = re.search(r'versionName = "(.*)"', content).group(1)
    print(f"versionName: {versionName}")
    
    # 获取版本代码 (需要先计算)
    versionCode = get_version_code(workspace)
    print(f"versionCode: {versionCode}")
    
    # 新的版本号
    tagVersionName = f"{versionName}-{channel}.{datetime.datetime.now().strftime('%Y%m%d_%H%M')}"
    # 替换 versionName
    content = re.sub(r'versionName = "(.*)"', f'versionName = "{tagVersionName}"', content)
    with open(workspace+'/app/build.gradle.kts', 'w') as file:
        file.write(content)
    return tagVersionName, versionCode

def get_version_code(workspace):
    """获取版本代码，模拟 calculateVersionCode() 函数的逻辑"""
    # 这里简化处理，实际项目中可能有复杂的版本代码计算逻辑
    # 可以通过执行 gradle 任务来获取实际的 versionCode
    result = subprocess.run(
        ['./gradlew', 'app:properties', '-q'], 
        cwd=workspace,
        capture_output=True, 
        text=True
    )
    
    for line in result.stdout.split('\n'):
        if 'versionCode:' in line:
            return line.split(':')[1].strip()
    
    # 如果无法获取，使用时间戳作为后备方案
    return str(int(datetime.datetime.now().timestamp()))

def build_logs(commits,workspace):
    with open(workspace+ '/.github/workflows/configuration.json') as file:
        content = json.loads(file.read())
    categories = content['categories']
    logs = {}
    for commit in commits:
        for cate in categories:
            if commit['emoji'] in cate['labels']:
                if cate['title'] not in logs:
                    logs[cate['title']] = []
                if commit['message'] not in logs[cate['title']]:
                    logs[cate['title']].append(commit['message'])
    return logs

def write_logs(logs,workspace,channel,tag,repo,restart):
    # 创建dist目录
    os.makedirs(workspace + '/dist', exist_ok=True)
    log_data = ""
    for cate in logs:
        log_data += f"{cate}\n"
        for log in logs[cate]:
            log_data += f"- {log}\n"
    t = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    data = {
        "version": tag,
        "code": 0,
        "log": log_data,
        "date": t
    }
    json_str = json.dumps(data, indent=4)
    with open(workspace + '/dist/index.json', 'w') as file:
        file.write(json_str)

    with open(workspace + '/dist/README.md', 'w', encoding='utf-8') as file:
        file.write("# 下载地址\n")
        # 对tag进行编码
        file.write(f" - [Github 下载](https://github.com/{repo}/releases/download/{tag}/app-release-signed.apk)\n")
        file.write(f" - [网盘下载](https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/{channel}/{tag}-release.apk)\n")
        if restart:
            file.write("# 重启提示\n")
            file.write(" - 由于修改了Android Framework部分，需要重新启动生效。\n")
        file.write("# 更新日志\n")
        file.write(" - 版本：" + tag + "\n")
        file.write(" - 发布时间：" + t + "\n")
        file.write(log_data)
    return log_data
def run_command_live(command):
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    for line in process.stdout:
        sys.stdout.write(line)
    process.wait()
    if process.returncode != 0:
        print(f"命令执行失败: {' '.join(command)}")
        sys.exit(1 )

def build_apk(workspace, version_name, version_code):
    print("开始构建 APK")
    gradlew_path = os.path.join(workspace, 'gradlew')
    
    # 构建标准 release 版本
    assemble_task = "assembleRelease"
    print(f"开始构建: {assemble_task}")
    run_command_live([gradlew_path, assemble_task])

    # 根据 archivesBaseName 构造实际的文件名
    actual_apk_name = f"app-{version_name}({version_code}).apk"
    apk_source_path = os.path.join(workspace, 'app', 'build', 'outputs', 'apk', 'release', actual_apk_name)
    apk_dest_path = os.path.join(workspace, "dist", 'app-release.apk')
    
    print(f"查找 APK 文件: {apk_source_path}")
    if not os.path.exists(apk_source_path):
        # 如果按预期名称找不到，尝试查找目录中的 APK 文件
        apk_dir = os.path.join(workspace, 'app', 'build', 'outputs', 'apk', 'release')
        apk_files = [f for f in os.listdir(apk_dir) if f.endswith('.apk')]
        if apk_files:
            actual_apk_name = apk_files[0]
            apk_source_path = os.path.join(apk_dir, actual_apk_name)
            print(f"找到 APK 文件: {actual_apk_name}")
        else:
            print(f"错误: 在 {apk_dir} 中找不到 APK 文件")
            sys.exit(1)
    
    subprocess.run(['cp', apk_source_path, apk_dest_path], check=True)
    print(f"APK 文件已保存到: {apk_dest_path}, 开始签名")
    sign_apk(apk_dest_path, workspace)

def sign_apk(absolute_path,workspace):
    androidHome = os.getenv('ANDROID_HOME')
    if not androidHome:
        print("ANDROID_HOME 环境变量未设置")
        sys.exit(1)
    version  = os.getenv("BUILD_TOOLS_VERSION") or '29.0.3'
    buildTools = os.path.join(androidHome, 'build-tools',version)
    apkSigner = os.path.join(buildTools, 'apksigner')
    sign_apk_file = absolute_path.replace('.apk', '-signed.apk')
    subprocess.run([
        apkSigner, 'sign',
        '--ks', workspace+'/.github/workflows/key',
        '--ks-key-alias', os.getenv("SIGN_ALIAS"),
        '--ks-pass', 'pass:'+os.getenv("SIGN_PASSWORD"),
        '--key-pass', 'pass:' + os.getenv("SIGN_PASSWORD"),
        '--out', sign_apk_file, absolute_path
    ], check=True)
    print(f"APK 文件已签名: {sign_apk_file}")

def create_tag(tag,channel):
    tag_name = f"{channel}.{tag}"
    result = subprocess.run(
        ['git', 'tag',  tag_name],
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    print(result.stdout)  # 打印标准输出
    result = subprocess.run(
        ['git', 'push', "origin", tag_name],
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    print(result.stdout)  # 打印标准输出
    pass
"""
发布 APK
"""
def publish_apk(repo, tag_name,workspace,log,channel):
    publish_to_github(repo, tag_name,  tag_name, log,f"{workspace}/dist/",False if channel == 'Stable' else True)
    publish_to_pan(workspace,tag_name,channel)
    pass

def upload(filename, filename_new, channel):
    # 上传文件
    url2 = "https://cloud.ankio.net/api/fs/put"
    #
    filename_new = quote('/自动记账/自动记账/版本更新/' + channel + "/" + filename_new, 'utf-8')  # 对文件名进行URL编码
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) '
                      'Chrome/58.0.3029.110 Safari/537.3',
        'Authorization': os.getenv("ALIST_TOKEN"),
        'file-path': filename_new,
        'As-Task': 'true'
    }
    # 读取文件内容
    with open(filename, 'rb') as file:
        file_data = file.read()
    res = requests.put(url=url2, data=file_data, headers=headers,timeout=120)
    print(res.text)


"""
发布到 Github
"""
def publish_to_github(repo, tag_name, release_name, release_body, file_path, prerelease=False):
    """
    创建 GitHub release 并上传文件
    :param repo: GitHub 仓库名
    :param tag_name: 发布的标签名 (版本号)
    :param release_name: 发布的标题
    :param release_body: 发布的描述信息
    :param file_path: 要上传的文件路径
    :param prerelease: 是否是 pre-release
    """
    # 从环境变量中获取 GitHub token
    token = os.getenv("GITHUB_TOKEN")

    if not token:
        raise ValueError("GITHUB_TOKEN is not set in the environment variables.")

    # 创建 release
    create_release_url = f"https://api.github.com/repos/{repo}/releases"
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json"
    }
    data = {
        "tag_name": tag_name,
        "name": release_name,
        "body": release_body,
        "draft": False,
        "prerelease": prerelease
    }

    response = requests.post(create_release_url, headers=headers, data=json.dumps(data))

    if response.status_code == 201:
        release_info = response.json()
        release_id = release_info['id']
        release_url = release_info['html_url']
        print(f"Release created successfully: {release_url}")
        # 上传单个 APK 文件
        path = file_path + "app-release-signed.apk"
        # 读取文件内容
        with open(path, 'rb') as file:
            file_data = file.read()  # 读取文件的二进制内容
        # 上传文件
        upload_url = release_info['upload_url'].split('{')[0]  # 去掉 URL 中的占位符
        file_name = os.path.basename(path)
        upload_response = requests.post(
            upload_url + f"?name={file_name}",
            headers={
                "Authorization": f"token {token}",
                "Accept": "application/vnd.github.v3+json",
                "Content-Type": "application/octet-stream"  # 明确指定上传内容类型
            },
            data=file_data  # 使用 data 参数传递文件的二进制内容
        )

        if upload_response.status_code == 201:
            print(f"File uploaded successfully: {upload_response.json()['browser_download_url']}")
        else:
            print(f"Failed to upload file: {upload_response.status_code}, {upload_response.text}")
    else:
        print(f"Failed to create release: {response.status_code}, {response.text}")



"""
发布到网盘
"""
def publish_to_pan(workspace,tag,channel):
    upload(workspace + "/dist/index.json", "/index.json", channel)
    upload(workspace + "/dist/README.md", "/README.md", channel)
    upload(workspace + "/dist/app-release-signed.apk", f"/{tag}-release.apk", channel)

def truncate_content(content):
    # 正则替换，将 ## 替换好
    content = md2tgmd.escape(content)
    # 检查字符串的长度
    if len(content) > 1024:
        # 截取前 4000 个字符并在末尾加上省略号
        return content[:1024] + "\\.\\.\\."
    else:
        # 如果长度不超过 4000，返回原字符串
        return content
def send_apk_with_changelog(workspace, title):
    # 读取更新日志
    with open(workspace + '/dist/README.md', 'r', encoding='utf-8') as file:
        content = file.read()
    
    token = os.getenv("TELEGRAM_BOT_TOKEN")
    channel_id = "@qianji_auto"
    base_url = f"https://api.telegram.org/bot{token}"
    
    # 上传 APK 文件
    file_ids = []
    file_path = os.path.join(workspace, 'dist', 'app-release-signed.apk')
    new_name = f"{title}-release.apk"
    
    try:
        with open(file_path, "rb") as apk_file:
            response = requests.post(
                f"{base_url}/sendDocument",
                data={
                    "chat_id": "@ezbook_archives",
                    "caption": "" # 空的说明文本
                },
                files={"document": (new_name, apk_file)}
            )
            response.raise_for_status()
            file_id = response.json()['result']['document']['file_id']
            file_ids.append(file_id)
            print(f"成功上传 release 版本")
    except Exception as e:
        print(f"上传 release 版本时出错: {str(e)}")
    
    # 构建媒体组
    media = []
    for i, file_id in enumerate(file_ids):
        item = {
            "type": "document",
            "media": file_id,
        }
        # 在最后一个文件添加说明文本
        if i == len(file_ids) - 1:
            item.update({
                "caption": truncate_content(content),
                "parse_mode": "MarkdownV2"
            })
        media.append(item)
    
    # 发送媒体组
    try:
        response = requests.post(
            f"{base_url}/sendMediaGroup",
            json={
                "chat_id": channel_id,
                "media": media
            }
        )
        response.raise_for_status()
        print("成功发送所有APK文件")
    except Exception as e:
        print(f"发送媒体组时出错: {str(e)}")
        print(f"错误详情: {response.text if 'response' in locals() else '无响应'}")


def send_forums( title, channel,workspace):
    if channel != 'Stable':
        print("非正式版，不发送到论坛")
        return
    with open(workspace + '/dist/README.md', 'r', encoding='utf-8') as file:
        content = file.read()
    url = "https://forum.ez-book.org/api/discussions"
    headers = {
        "Content-Type": "application/json",
        "Authorization": "Token " + os.getenv("FORUMS_API_TOKEN"),
    }

    data = {
        "data": {
            "type": "discussions",
            "attributes": {
                "title": title,
                "content": content
            },
            "relationships": {
                "tags": {
                    "data": [
                        {
                            "type": "tags",
                            "id": "5"
                        }
                    ]
                }
            }
        }
    }
    response = requests.post(url, headers=headers, data=json.dumps(data))
    print(response.json())
"""
通知
"""
def notify(title,channel,workspace):
    send_forums( title,channel,workspace)
    send_apk_with_changelog( workspace,title)


def main(repo):
    channel = os.getenv('CHANNEL') or 'Stable'
    print(f"渠道: {channel}")
    workspace = os.getenv("GITHUB_WORKSPACE") or os.getcwd()
    print(f"工作目录: {workspace}")
    tag = get_latest_tag_with_prefix(channel)
    print(f"最新 tag: {tag}")
    commits = get_commits_since_tag(tag)
    if len(commits) == 0:
        print("没有新的提交，无需构建")
        sys.exit(0)
    tagVersionName, versionCode = get_and_set_version(channel,workspace)
    print(f"新的版本号: {tagVersionName}, 版本代码: {versionCode}")
    restart = get_changed_files_since_tag(tag)
    logs = build_logs(commits,workspace)
    log_data = write_logs(logs,workspace,channel,tagVersionName,repo,restart)
    build_apk(workspace, tagVersionName, versionCode)
    publish_apk(repo, tagVersionName,workspace,log_data,channel)
    notify(tagVersionName, channel, workspace)
    #create_tag(tagVersionName, channel)

main("AutoAccountingOrg/AutoAccounting")