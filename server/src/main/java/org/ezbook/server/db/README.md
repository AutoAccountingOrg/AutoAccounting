# 数据库相关

数据库使用安卓Room实现，不同模式下数据库存储路径不同：

- 在无障碍模式下，数据库存储于`自动记账App`的私有数据目录：
  `/data/data/net.ankio.auto.helper/database/`。
- 在Xposed模式下，数据库存储于`安卓系统`的私有数据目录：`/data/data/andriod/database/`，如果是`Debug`
  版本（从AS直接运行），则存储于`自动记账App`的私有数据目录。

> [!IMPORTANT]
>
如果你修改了数据库结构，请务必编写迁移代码，详细参考[迁移 Room 数据库](https://developer.android.com/training/data-storage/room/migrating-db-versions?hl=zh-cn)

# 数据库调用

> [!IMPORTANT]
> 在`Server`模块的`Route`中，可以直接调用Dao方法，默认运行在`IO`线程；
> 在其他模块中调用，请在`Model`类中进行封装（API调用）后再进行调用；

```kotlin
// 直接调用，仅限于Server模块
Db.get().xxDao().xxMethod()
//封装调用，在其他模块中调用
xxModel.list()
```

# 数据库模型

- `AppDataModel`: 存储获取到的App数据、通知
- `AssetsModel`: 存储资产信息
- `BillInfoModel`: 存储识别到的账单信息
- `LogModel`: 存储日志信息
- `SettingModel`: 存储设置信息
- `RuleModel`: 存储规则信息，包括用户规则（暂未开放）和云规则