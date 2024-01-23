from urllib.parse import quote

import requests
import json
import os
# 登录
username = os.getenv('USERNAME_ENV_VAR')
password = os.getenv('PASSWORD_ENV_VAR')
import hashlib

hash_salt = "https://github.com/alist-org/alist"
to_hash = f"{password}-{hash_salt}"
hashed_password = hashlib.sha256(to_hash.encode()).hexdigest()

# host
host = os.getenv('HOST_ENV_VAR')
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) '
                  'Chrome/58.0.3029.110 Safari/537.3'
}
url = host + '/api/auth/login/hash'

d = {'Username': username, 'Password': hashed_password}
r = requests.post(url, data=d, headers=headers)

# 对登录后返回的数据进行解析
data = json.loads(r.text)

token = data.get('data').get('token')


def upload(filename, filename_new, auth):
    # 上传文件
    url2 = host + "/api/fs/put"
    filename_new = quote('/阿里云盘/自动记账/版本更新/持续构建版/' + filename_new, 'utf-8')  # 对文件名进行URL编码
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) '
                      'Chrome/58.0.3029.110 Safari/537.3',
        'Authorization': auth,
        'file-path': filename_new
    }
    dir = os.getenv("GITHUB_WORKSPACE")
    # 读取文件内容
    with open(dir+filename, 'rb') as file:
        file_data = file.read()
    res = requests.put(url=url2, data=file_data, headers=headers)
    print(res.text)


changeLog = os.getenv('CHANGELOG')
with open(os.getenv("GITHUB_WORKSPACE")+"/release/README.md", 'w') as file:
    file.writelines(changeLog)
upload("/release/README.md", "README.md", token)
upload("/release/app-xposed.apk", "xposed.apk", token)
upload("/release/MagiskModule.zip", "MagiskModule.zip", token)
# TODO 除了上传文件到服务器以外，还需要通过bot通知到自动记账群的用户。
