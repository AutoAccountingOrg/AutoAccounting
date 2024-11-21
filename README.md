![AutoAccounting](https://socialify.git.ci/AutoAccountingOrg/AutoAccounting/image?description=1&font=Bitter&forks=1&issues=1&logo=https%3A%2F%2Fs21.ax1x.com%2F2024%2F10%2F14%2FpAtnhcD.png&name=1&pattern=Circuit%20Board&pulls=1&stargazers=1&theme=Light)

> [!IMPORTANT]
> 由于自动记账正在快速迭代，请不要自行编译RELEASE版本，因为RELEASE版本会发送错误信息到Bugsug，会导致我们排查问题存在困扰。
> 测试版更新频繁，也不保证可用性和数据完整性，随时可能有重大bug，建议希望正常用的用户不要频繁更新（能用就别动）。
> 遇到问题自查四部曲：
> 1. 检查数据里面是否有相关的支付数据（没有可能是bug)。
> 2. 支付数据如果没有被规则匹配上可以检查规则页面是否有对应规则（没有规则长按首页更新按钮强制更新）。 
> 3. 如果有对应规则还没匹配上，检查日志是否报错（有报错反馈github bug)。
> 4. 如果一切都没问题还是不能识别账单，点数据页面的上传按钮上传数据到云端等适配。


![Framework](https://img.shields.io/static/v1?label=framework&message=Xposed%2F%E6%97%A0%E9%9A%9C%E7%A2%8D&color=success&style=for-the-badge) ![License](https://img.shields.io/static/v1?label=licenes&message=GPL3.0&color=important&style=for-the-badge)

## 💸 支持的记账软件

| 软件名称 | 简介               | 主页                        | 备注                        |
| -------- |------------------|---------------------------|---------------------------|
| 钱迹     | 无广告、无开屏、无理财的记账软件 | <https://www.qianji.app/> | 从4.0Beta13开始，钱迹补丁合并进入自动记账 |

## 🌝 开发者适配

Waiting...

## 📱 支持的应用

| 软件名称  | Xposed | 无障碍 | 备注 |
| --------- | ------ | ------ | ---- |
| 微信      | ✔️      | ✔️      |      |
| QQ        | ✔️      | ✔️      |      |
| 支付宝    | ✔️      | ✔️      |      |
| 云闪付    | ✔️      | ❌      |      |
| 美团      | ✔️      | ❌      |      |
| 山大v卡通 | ✔️      | ❌      |      |

## 📖 文字教程

[点击开始教程](https://auto.ankio.net)


## 📺 视频教程

Waiting...

## 🎉 贡献指南

> [!IMPORTANT]
> 目前自动记账正处于试验阶段，功能、数据库等逻辑层面随时可能有较大的变化，所以提交PR时请**避免修改了很多部分再提交**。
> 提交代码/PR前请**务必**先阅读贡献指南中的代码规范及Commit规范。

[贡献指南](CONTRIBUTING.md)

## 🛠️ 编译步骤

- 下载源代码到本地

```bash
git clone https://github.com/AutoAccountingOrg/AutoAccounting
```
- 使用[Android Studio](https://developer.android.com/studio)打开，等待自动配置完成，如出现失败请配置科学上网

- 点击菜单中 `Build` - `Build Bundle(s) / APK(s)` - `Build APK(s)`

## ⬇️ 下载

- [Canary](https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/Canary)：每隔3小时自动构建，可能会有新功能或者bug修复，不保证可用性。
- [Beta](https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/Beta): 稳定版发布前的测试版本，已通过小规模测试，具备高可用性，但是仍可能存在BUG。
- [Stable](https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/Stable): 稳定版本，不会有新功能，只会修复bug。


## ❤️ 支持赞助
[![](https://img.shields.io/badge/-%E7%88%B1%E5%8F%91%E7%94%B5-%23977ce4?style=for-the-badge&logo=buymeacoffee&logoColor=%23ffffff)](https://afdian.com/a/ankio/) 


## 📝 License

Copyright © 2024 [Ankio](https://www.ankio.net).<br />
This project is [GPL3.0](https://github.com/AutoAccountingOrg/AutoAccounting/blob/master/LICENSE) licensed.



