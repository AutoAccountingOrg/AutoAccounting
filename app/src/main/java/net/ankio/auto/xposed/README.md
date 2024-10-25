# 说明

## hooks文件夹


所有的hook逻辑均在这里。

### 新增Hook
每一个文件夹代表一个App的hook，例如：`微信`的hook，就写到`wechat`文件夹下，文件夹（包）的命名尽可能与App的英文名称一致。

每一个App的Hook必须实现`HookerManifest`接口，用于注册hook的入口。并在`Apps`中添加实例化代码。

对于App功能的hook代码，则需要实现`PartHooker`接口，并在`HookerManifest`中的`partHookers`注册。

> [!IMPORTANT]
> 1. 注意在`Apps`添加对应App的hook入口；
> 2. 在`res/values/arrays.xml` 添加对应需要hook的App的包名及注释；
> 3. 最后，请更新`hooks`文件夹中的`README.md`文件，简要说明每个hook的功能。

### Hook

所有Hook相关的操作建议使用`net/ankio/auto/xposed/core/hook/Hooker.kt`内的方法处理（包括loadClass)。

和字段/属性/反射相关的操作建议使用`XposedHelper`处理，在`net/ankio/auto/xposed/core/hook/HookHelper.kt`中拓展了`XposedHelper`的方法。

### 日志

日志建议使用 `HookerManifest` 中的 `log` 方法，这样可以避免添加`TAG`。

## utils文件夹

工具类

## Apps

需要hook的实例