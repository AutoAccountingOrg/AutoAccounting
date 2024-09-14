#!/usr/bin/env python3
import datetime
import json
import os
import re
import subprocess
import sys
from urllib.parse import quote

import requests

def get_latest_tag_with_prefix(prefix):
    print(f"获取最新的 tag: {prefix}")
    result = subprocess.run(['git', 'tag', '--list','--sort=v:refname', f'{prefix}*'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    tags = result.stdout.strip().split('\n')
    if not tags or tags[-1] == "":
        return get_latest_tag_with_prefix('v')
    return tags[-1]

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
    with open(workspace + '/app/build.gradle') as file:
        content = file.read()
    versionName = re.search(r'versionName "(.*)"', content).group(1)
    print(f"versionName: {versionName}")
    # 新的版本号
    # tagVersionName="${versionName}-${channel}.$(date +'%Y%m%d%H%M%S')"
    tagVersionName = f"{versionName}-{channel}.{datetime.datetime.now().strftime('%Y%m%d%H%M%S')}"
    # 替换 versionName
    content = re.sub(r'versionName "(.*)"', f'versionName "{tagVersionName}"', content)
    with open(workspace+'/app/build.gradle', 'w') as file:
        file.write(content)
    return tagVersionName

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

def write_logs(logs,workspace,channel,tag,repo):
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
        file.write(f" - [Github](https://github.com/{repo}/releases/tag/{tag})\n")
        file.write(f" - [网盘](https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/{channel}.apk)\n")
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
        sys.exit(1)

def build_apk(workspace):
    print("开始构建 APK")
    gradlew_path = os.path.join(workspace, 'gradlew')
    # 构建 APK
    for flavor in ['xposed']:
        assemble_task = f"assemble{flavor.capitalize()}Release"
        print(f"开始构建 {flavor} 版本: {assemble_task}")
        run_command_live([gradlew_path, assemble_task])

        apk_source_path = os.path.join(workspace, 'app', 'build', 'outputs', 'apk', flavor, 'release', 'app.apk')
        apk_dest_path = os.path.join(workspace,"dist", f'app-{flavor}.apk')
        subprocess.run(['cp', apk_source_path, apk_dest_path], check=True)
        print(f"{flavor} 版本的 APK 文件已保存到: {apk_dest_path}, 开始签名")
        sign_apk(apk_dest_path,workspace)

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
    publish_to_github(repo, tag_name,  tag_name, log,f"{workspace}/dist/app-xposed-signed.apk",False if channel == 'Stable' else True)
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
        'file-path': filename_new
    }
    # 读取文件内容
    with open(filename, 'rb') as file:
        file_data = file.read()
    res = requests.put(url=url2, data=file_data, headers=headers)
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

        # 上传文件
        upload_url = release_info['upload_url'].split('{')[0]  # 去掉 URL 中的占位符
        file_name = os.path.basename(file_path)
        with open(file_path, 'rb') as file:
            files = {
                'file': (file_name, file, 'application/octet-stream')
            }
            upload_response = requests.post(
                upload_url + f"?name={file_name}",
                headers={
                    "Authorization": f"token {token}",
                    "Accept": "application/vnd.github.v3+json"
                },
                files=files
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

    upload(workspace + "/dist/app-xposed-signed.apk", tag + ".apk", channel)


"""
通知
"""
def notify(title, content,channel):
    send_forums( title, content,channel)
def send_forums( title, content, channel):
    url = "https://forum.ez-book.org/posts.json"
    headers = {
        "Content-Type": "application/json",
        "Api-Key": os.getenv("FORUMS_API_KEY"),
        "Api-Username": "system"
    }
    data = {
        "title": title,
        "raw": content,
        "category": 8,
        "tags": ["版本发布", channel]
    }
    response = requests.post(url, headers=headers, json=data)
    print(response.json())
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
        sys.exit(1)
    tagVersionName = get_and_set_version(channel,workspace)
    print(f"新的版本号: {tagVersionName}")
    logs = build_logs(commits,workspace)
    log_data = write_logs(logs,workspace,channel,tagVersionName,repo)
    build_apk(workspace)
    create_tag(tagVersionName,channel)
    publish_apk(repo, tagVersionName,workspace,log_data,channel)
    notify(tagVersionName,log_data,channel)

main("AutoAccountingOrg/AutoAccounting")