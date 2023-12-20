# 开发变更日志

该日志用于记录重大变更

## Xposed版本剔除系统服务的Hook

原计划使用系统服务来为自动记账提供支持，但是有SELinux拦路，咱们换个思路。

~~使用Lsposed重新实现的XSharedPreferences~~使用RemotePreferences提供数据交互支持，在HookApp中先执行数据分析，然后直接使用intent拉起自动记账，将数据存储到自动记账里面来。

至于自动记账的数据同步第三方工具的话，可以让第三方工具接入自动记账sdk，在app启动的时候向自动记账发起查询请求获取未同步数据。