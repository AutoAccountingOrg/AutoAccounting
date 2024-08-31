# hook应用列表

> [!IMPORTANT]
> HOOK 原则
> 1. 不同版本动态变化的class名称，请使用class查找规则实现自动适配；
> 2. 最小化Class的使用，尽可能使用反射来处理，减少class的适配工作；

## Android

安卓系统内hook

- Service: 自动记账服务。debug模式下不会启动自动记账服务，release模式下会启动自动记账服务。
- Notification: 获取通知栏的消息，用于解析消费记录。
- Permission: 动态授权被hook的应用权限（无需用户交互）。

### Auto

自动记账Hook，debug模式下会启动自动记账服务（就是使用Android Studio运行时），release模式下不会启动自动记账服务。

- Active: 设置自动记账激活状态