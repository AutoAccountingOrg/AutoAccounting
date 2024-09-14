from urllib.parse import quote

import requests
import markdown
import json
import time
import os
import re


"""
提取字符串中的第一个整数
"""
def extract_int(s):
    # 使用正则表达式匹配字符串中的第一个连续数字序列
    match = re.search(r'\d+', s.replace(".", ""))
    if match:
        # 如果找到数字，将其转换为整数
        return int(match.group())
    else:
        # 如果没有找到数字，返回 None 或抛出异常
        return 0

"""
使用alist的API上传文件
"""
def upload(filename, filename_new,channel):
    # 上传文件
    url2 =   "https://cloud.ankio.net/api/fs/put"
    #
    filename_new = quote('/自动记账/自动记账/版本更新/'+channel +"/" + filename_new, 'utf-8')  # 对文件名进行URL编码
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
将数据写入文件
"""
def put_file():
    # 写入版本文件
    with open('log/changelog.txt', 'r', encoding='utf-8') as file:
        log = file.read()
    with open('package/versionCode.txt', 'r', encoding='utf-8') as file:
        code = file.read()
    with open('package/tagVersionName.txt', 'r', encoding='utf-8') as file:
        name = file.read()
    t = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
    # 版本文件
    data = {
        "version": name,
        "code": code,
        "log": "\n".join(log.splitlines()[5:]),
        "date": t
    }
    json_str = json.dumps(data, indent=4)
    with open(os.getenv("GITHUB_WORKSPACE") + "/index.json", 'w') as file:
        file.writelines(json_str)
    # 日志README
    with open(os.getenv("GITHUB_WORKSPACE") + "/README.md", 'w') as file:
        file.writelines(markdown_log)
    return name, t, html

"""
上传到网盘
"""
def upload_pan(name,channel):
    # 获取GITHUB_WORKSPACE环境变量并拼接dist目录
    dir = os.getenv("GITHUB_WORKSPACE")

    # 获取目录长度，后面用于生成相对路径

    upload(dir + "/index.json", "/index.json",channel)

    upload(dir + "/README.md", "/README.md",channel)

    upload(os.getenv("GITHUB_WORKSPACE") + "/release/app-xposed-signed.apk", name+".apk",channel)

"""
调用机器人发送消息
"""
def send_bot():
    pass

"""
在社区发帖
"""
def send_forums(api_key,title,content,channel):
    url = "https://forum.ez-book.org/posts.json"
    headers = {
        "Content-Type": "application/json",
        "Api-Key": api_key,
        "Api-Username": "system"
    }
    data = {
        "title": title,
        "raw": content,
        "category": 8,
        "tags": ["版本发布",channel]
    }
    response = requests.post(url, headers=headers, json=data)
    print(response.json())

"""
通知用户
"""
def send_notify(title,content):
    # 在社区发帖
    send_forums(os.getenv("FORUMS_API_KEY"),title,content)
    # 在群里通知
    send_bot()


if __name__ == '__main__':
    channel = os.getenv("CHANNEL_ID")
    name, t, log = put_file()
    upload_pan(name,channel)
    send_notify(name,log,channel)