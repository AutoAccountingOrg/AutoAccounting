# hook应用列表

> [!IMPORTANT]
> HOOK 原则
> 1. 不同版本动态变化的class名称，请使用class查找规则实现自动适配；
> 2. 最小化Class的使用，尽可能使用反射来处理，减少class的适配工作；

### android

安卓系统内hook

- Service: 自动记账服务。debug模式下不会启动自动记账服务，release模式下会启动自动记账服务。
- Notification: 获取通知栏的消息，用于解析消费记录。
- Permission: 动态授权被hook的应用权限（无需用户交互）。
- PermissionCheck: AppOps部分hook。

### auto

自动记账Hook，debug模式下会启动自动记账服务（就是使用Android Studio运行时），release模式下不会启动自动记账服务。

- Active: 设置自动记账激活状态

### alipay

支付宝相关的hook

- MessageBox: 支付宝消息盒子的hook, 用于获取支付消息。
- RedPackage: 支付宝红包hook，用于获取红包消息。
- WebView: 支付宝WebView的hook，用于获取支付宝账单消息。

### wechat

微信相关的hook

- DataBase: 微信数据库的hook，用于获取微信消息卡片的推送。

### qianji

钱迹的hook

- SideBar: 钱迹侧边栏hook，用于添加自动记账的标签，支持钱迹的自动同步。
- Auto: 钱迹的自动记账接口hook，给钱迹加上原本没有的：债务、报销功能，同时优化错误提示。

### sms

短信的hook
- SmsIntent: 短信Intent的hook，用于获取短信消息。