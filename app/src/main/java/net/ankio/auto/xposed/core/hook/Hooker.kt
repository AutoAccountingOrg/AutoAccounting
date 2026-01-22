package net.ankio.auto.xposed.core.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime
import java.lang.reflect.Method

/**
 * Xposed hooker utility for streamlined hooking operations.
 */
object Hooker {

    private val hookMap = HashMap<String, XC_MethodHook.Unhook>()

    /**
     * 加载类
     */
    fun loader(clazz: String, classloader: ClassLoader? = null): Class<*> {
        return classloader?.loadClass(clazz) ?: AppRuntime.classLoader.loadClass(clazz)
    }

    /**
     * 构建参数类型
     */
    private fun buildParameterTypes(vararg parameterTypes: Any): Array<Class<*>> {
        return parameterTypes.map {
            when (it) {
                is Class<*> -> it
                is String -> {
                    try {
                        loader(it, AppRuntime.classLoader)
                    } catch (e: ClassNotFoundException) {
                        throw IllegalArgumentException("Invalid parameter type: $it", e)
                    }
                }

                else -> throw IllegalArgumentException("Invalid parameter type: $it")
            }
        }.toTypedArray()
    }

    /**
     * 在方法执行后进行钩子操作。
     * @param clazz 类的名称。
     * @param method 方法的名称。
     * @param parameterTypes 方法的参数类型。
     * @param hook 钩子函数，用于在方法执行后进行调用。
     */
    fun after(
        clazz: String,
        method: String,
        vararg parameterTypes: Any = emptyArray(),
        hook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        try {
            val loadedClass = loader(clazz, AppRuntime.classLoader)
            val types = buildParameterTypes(*parameterTypes)
            after(loadedClass, method, *types, hook = hook)
        } catch (e: ClassNotFoundException) {
            Logger.e("Class not found: $clazz", e)
        } catch (e: IllegalArgumentException) {
            Logger.e("Invalid parameter type: ${e.message}", e)
        } catch (e: Exception) {
            Logger.e("Error hooking method after: $clazz.$method - ${e.message}", e)
        }
    }

    /**
     * 在方法执行后进行钩子操作。
     * @param clazz 类对象。
     * @param method 方法的名称。
     * @param parameterTypes 方法的参数类型。
     * @param hook 钩子函数，用于在方法执行后进行调用。
     */
    fun after(
        clazz: Class<*>,
        method: String,
        vararg parameterTypes: Class<*> = emptyArray(),
        hook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        try {
            XposedHelpers.findAndHookMethod(
                clazz,
                method,
                *parameterTypes,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        hook(param)
                    }
                })
        } catch (e: Exception) {
            Logger.e("Error hooking method before: $clazz.$method - ${e.message}", e)
        }
    }

    /**
     * 在方法执行前进行钩子操作。
     * @param clazz 类的名称。
     * @param method 方法的名称。
     * @param parameterTypes 方法的参数类型。
     * @param hook 钩子函数，用于在方法执行前进行调用。
     */
    fun before(
        clazz: String,
        method: String,
        vararg parameterTypes: Any = emptyArray(),
        hook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        try {
            val loadedClass = loader(clazz, AppRuntime.classLoader)
            val types = buildParameterTypes(*parameterTypes)
            before(loadedClass, method, *types, hook = hook)
        } catch (e: ClassNotFoundException) {
            Logger.e("Class not found: $clazz", e)
        } catch (e: Exception) {
            Logger.e("Error hooking method before: $clazz.$method - ${e.message}", e)
        }
    }

    /**
     * 在方法执行前进行钩子操作。
     * @param clazz 类对象。
     * @param method 方法的名称。
     * @param parameterTypes 方法的参数类型。
     * @param hook 钩子函数，用于在方法执行前进行调用。
     */
    fun before(
        clazz: Class<*>,
        method: String,
        vararg parameterTypes: Class<*> = emptyArray(),
        hook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        try {
            XposedHelpers.findAndHookMethod(
                clazz,
                method,
                *parameterTypes,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        hook(param)
                    }
                })
        } catch (e: Exception) {
            Logger.e("Error hooking method before: $clazz.$method - ${e.message}", e)
        }
    }

    /**
     * 一次性在方法执行后进行钩子操作，执行后自动解除钩子。
     * @param clazz 类对象。
     * @param method 方法的名称。
     * @param parameterTypes 方法的参数类型。
     * @param hook 钩子函数，用于在方法执行后进行调用，返回 true 时解除钩子。
     */
    fun onceAfter(
        clazz: Class<*>,
        method: String,
        vararg parameterTypes: Class<*> = emptyArray(),
        hook: (XC_MethodHook.MethodHookParam) -> Boolean
    ) {
        try {
            val hookKey = "$clazz-$method-${parameterTypes.joinToString()}"
            hookMap[hookKey]?.unhook()
            val unhook = XposedHelpers.findAndHookMethod(
                clazz,
                method,
                *parameterTypes,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = hook(param)
                        if (result) {
                            hookMap[hookKey]?.unhook()
                            hookMap.remove(hookKey)
                        }
                    }
                })
            hookMap[hookKey] = unhook
        } catch (e: Exception) {
            Logger.e("Error hooking once method after: $clazz.$method - ${e.message}", e)
        }
    }

    /**
     * 一次性在方法执行前进行钩子操作，执行后自动解除钩子。
     * @param clazz 类对象。
     * @param method 方法的名称。
     * @param parameterTypes 方法的参数类型。
     * @param hook 钩子函数，用于在方法执行前进行调用，返回 true 时解除钩子。
     */
    fun onceBefore(
        clazz: Class<*>,
        method: String,
        vararg parameterTypes: Class<*> = emptyArray(),
        hook: (XC_MethodHook.MethodHookParam) -> Boolean
    ) {
        try {
            val hookKey = "$clazz-$method-${parameterTypes.joinToString()}"
            hookMap[hookKey]?.unhook()
            val unhook = XposedHelpers.findAndHookMethod(
                clazz,
                method,
                *parameterTypes,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val result = hook(param)
                        if (result) {
                            hookMap[hookKey]?.unhook()
                            hookMap.remove(hookKey)
                        }
                    }
                })
            hookMap[hookKey] = unhook
        } catch (e: Exception) {
            Logger.e("Error hooking once method before: $clazz.$method - ${e.message}", e)
        }
    }

    /**
     * 将所有方法的执行前进行钩子操作。
     * @param clazz 类对象。
     * @param hook 钩子函数，用于在每个方法执行前进行调用。
     */
    fun allMethodsBefore(
        clazz: Class<*>,
        hook: (XC_MethodHook.MethodHookParam, Method) -> Any?
    ) {
        clazz.declaredMethods.forEach { method ->
            try {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        hook(param, method)
                    }
                })
            } catch (e: Exception) {
                Logger.e("Error hooking method before: ${method.name} - ${e.message}", e)
            }
        }
    }

    /**
     * 将所有方法的执行后进行钩子操作。
     * @param clazz 类对象。
     * @param hook 钩子函数，用于在每个方法执行后进行调用。
     */
    fun allMethodsAfter(
        clazz: Class<*>,
        hook: (XC_MethodHook.MethodHookParam, Method) -> Any?
    ) {
        clazz.declaredMethods.forEach { method ->
            try {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        hook(param, method)
                    }
                })
            } catch (e: Exception) {
                Logger.e("Error hooking method after: ${method.name} - ${e.message}", e)
            }
        }
    }

    /**
     * 对指定方法名的所有方法执行前进行钩子操作。
     * @param clazz 类对象。
     * @param methodName 要钩子的方法名。
     * @param hook 钩子函数，用于在方法执行前进行调用。
     */
    fun allMethodsEqBefore(
        clazz: Class<*>,
        methodName: String,
        hook: (XC_MethodHook.MethodHookParam, Method) -> Any?
    ) {
        clazz.declaredMethods.filter { it.name == methodName }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    hook(param, method)
                }
            })
        }
    }

    /**
     * 对指定方法名的所有方法执行后进行钩子操作。
     * @param clazz 类对象。
     * @param methodName 要钩子的方法名。
     * @param hook 钩子函数，用于在方法执行后进行调用。
     */
    fun allMethodsEqAfter(
        clazz: Class<*>,
        methodName: String,
        hook: (XC_MethodHook.MethodHookParam, Method) -> Any?
    ) {
        clazz.declaredMethods.filter { it.name == methodName }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    hook(param, method)
                }
            })
        }
    }

    /**
     * 将任意值转换为字符串表示形式。
     * @param value 任意值。
     * @return 字符串表示形式。
     */
    fun valueToString(value: Any?): String {
        return when (value) {
            is Array<*> -> value.joinToString(prefix = "[", postfix = "]")
            is List<*> -> value.joinToString(prefix = "[", postfix = "]")
            is Set<*> -> value.joinToString(prefix = "{", postfix = "}")
            is Map<*, *> -> value.entries.joinToString(
                prefix = "{",
                postfix = "}"
            ) { (k, v) -> "$k=$v" }

            null -> "null"
            else -> value.toString()
        }
    }

    /**
     * 替换指定方法的执行结果。
     * @param clazz 类的名称。
     * @param method 方法的名称。
     * @param parameterTypes 方法的参数类型。
     * @param hook 钩子函数，用于替换方法的执行结果。
     */
    fun replace(
        clazz: String,
        method: String,
        vararg parameterTypes: Any,
        hook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        try {
            val loadedClass = loader(clazz, AppRuntime.classLoader)
            val types = buildParameterTypes(*parameterTypes)
            replace(loadedClass, method, *types, hook = hook)
        } catch (e: ClassNotFoundException) {
            Logger.e("Class not found: $clazz", e)
        } catch (e: IllegalArgumentException) {
            Logger.e("Invalid parameter type: ${e.message}", e)
        } catch (e: Exception) {
            Logger.e("Error replacing method: $clazz.$method - ${e.message}", e)
        }
    }

    /**
     * 替换指定类和方法的执行结果。
     * @param clazz 类对象。
     * @param method 方法的名称。
     * @param parameterTypes 方法的参数类型。
     * @param hook 钩子函数，用于替换方法的执行结果。
     */
    fun replace(
        clazz: Class<*>,
        method: String,
        vararg parameterTypes: Class<*>,
        hook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookMethod(
            clazz,
            method,
            *parameterTypes,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam) {
                    hook(param)
                }
            }
        )
    }

    /**
     * 替换方法执行后的返回值。
     * @param clazz 类的名称。
     * @param method 方法的名称。
     * @param value 要替换的返回值。
     * @param parameterTypes 方法的参数类型。
     */
    fun replaceReturn(
        clazz: String,
        method: String,
        value: Any?,
        vararg parameterTypes: Any
    ) {
        try {
            val loadedClass = loader(clazz, AppRuntime.classLoader)
            val types = buildParameterTypes(*parameterTypes)
            replaceReturn(loadedClass, method, value, *types)
        } catch (e: ClassNotFoundException) {
            Logger.e("Class not found: $clazz", e)
        } catch (e: IllegalArgumentException) {
            Logger.e("Invalid parameter type: ${e.message}", e)
        } catch (e: Exception) {
            Logger.e("Error replacing return value: $clazz.$method - ${e.message}", e)
        }
    }

    /**
     * 替换方法执行后的返回值。
     * @param clazz 类的名称。
     * @param method 方法的名称。
     * @param value 要替换的返回值。
     * @param parameterTypes 方法的参数类型。
     */
    fun replaceReturn(
        clazz: Class<*>,
        method: String,
        value: Any?,
        vararg parameterTypes: Class<*>
    ) {
        XposedHelpers.findAndHookMethod(
            clazz,
            method,
            *parameterTypes,
            XC_MethodReplacement.returnConstant(value)
        )
    }

    /**
     * 调用原始方法执行，只有在replace里面可能需要。
     * 一般情况下使用 before函数并使用it.setResult(null)来阻止方法执行。
     */
    fun XC_MethodHook.MethodHookParam.callOriginalMethod(): Any? {
        return XposedBridge.invokeOriginalMethod(this.method, this.thisObject, this.args)
    }

    /**
     * 生成Hook的唯一键
     * @param clazz 类对象或类名
     * @param method 方法名
     * @param parameterTypes 参数类型数组
     * @return Hook的唯一标识字符串
     */
    private fun generateHookKey(
        clazz: Any,
        method: String,
        parameterTypes: Array<out Any> = emptyArray()
    ): String =
        "$clazz-$method-${parameterTypes.joinToString()}"

    /**
     * 一次性在方法执行前进行Hook，按方法名匹配所有重载方法
     * @param clazz 要Hook的类（可以是Class对象或类名字符串）
     * @param methodName 要Hook的方法名
     * @param hook Hook处理函数
     */
    fun onceBeforeNoParams(
        clazz: Any,
        methodName: String,
        hook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        try {
            val targetClass = when (clazz) {
                is String -> loader(clazz, AppRuntime.classLoader)
                is Class<*> -> clazz
                else -> throw IllegalArgumentException("Invalid class type")
            }

            targetClass.declaredMethods
                .filter { it.name == methodName }
                .forEach { method ->
                    val hookKey = generateHookKey(targetClass, methodName, method.parameterTypes)
                    hookMap[hookKey]?.unhook()

                    val unhook = XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            hook(param)
                            hookMap[hookKey]?.unhook()
                            hookMap.remove(hookKey)
                        }
                    })
                    hookMap[hookKey] = unhook
                }
        } catch (e: Exception) {
            when (e) {
                is ClassNotFoundException -> Logger.e("Class not found: $clazz", e)
                else -> Logger.e("Error hooking method: $clazz.$methodName", e)
            }
        }
    }

    /**
     * 一次性在方法执行后进行Hook，按方法名匹配所有重载方法
     * @param clazz 要Hook的类（可以是Class对象或类名字符串）
     * @param methodName 要Hook的方法名
     * @param hook Hook处理函数
     */
    fun onceAfterNoParams(
        clazz: Any,
        methodName: String,
        hook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        try {
            val targetClass = when (clazz) {
                is String -> loader(clazz, AppRuntime.classLoader)
                is Class<*> -> clazz
                else -> throw IllegalArgumentException("Invalid class type")
            }

            targetClass.declaredMethods
                .filter { it.name == methodName }
                .forEach { method ->
                    val hookKey = generateHookKey(targetClass, methodName, method.parameterTypes)
                    hookMap[hookKey]?.unhook()

                    val unhook = XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            hook(param)
                            hookMap[hookKey]?.unhook()
                            hookMap.remove(hookKey)
                        }
                    })
                    hookMap[hookKey] = unhook
                }
        } catch (e: Exception) {
            when (e) {
                is ClassNotFoundException -> Logger.e("Class not found: $clazz", e)
                else -> Logger.e("Error hooking method: $clazz.$methodName", e)
            }
        }
    }

    /**
     * 监视类的所有方法调用，打印详细信息
     * @param clazz 类名或类对象
     * @param methodFilter 方法名过滤器，只监视包含此关键字的方法（不区分大小写），null表示监视所有方法
     * @param printStack 是否打印调用堆栈
     * @param printArgs 是否打印参数
     * @param printReturn 是否打印返回值
     * @param maxStackDepth 堆栈打印深度，默认5层
     */
    fun watch(
        clazz: Any,
        methodFilter: String? = null,
        printStack: Boolean = true,
        printArgs: Boolean = true,
        printReturn: Boolean = true,
        maxStackDepth: Int = 5
    ) {
        try {
            val targetClass = when (clazz) {
                is String -> loader(clazz, AppRuntime.classLoader)
                is Class<*> -> clazz
                else -> throw IllegalArgumentException("Invalid class type: must be String or Class<*>")
            }

            Logger.d("🔍 开始监视类: ${targetClass.name}")
            Logger.d("   过滤器: ${methodFilter ?: "无（监视所有方法）"}")
            Logger.d("   堆栈: $printStack | 参数: $printArgs | 返回: $printReturn")

            var hookedCount = 0
            targetClass.declaredMethods
                .filter { method ->
                    methodFilter == null || method.name.contains(methodFilter, ignoreCase = true)
                }
                .forEach { method ->
                    try {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val sb = StringBuilder()
                                sb.append("\n" + "=".repeat(80) + "\n")
                                sb.append("📞 方法调用: ${targetClass.simpleName}.${method.name}\n")
                                sb.append("=".repeat(80) + "\n")

                                // 打印调用堆栈
                                if (printStack) {
                                    sb.append("📚 调用堆栈:\n")
                                    val stackTrace = Thread.currentThread().stackTrace
                                    stackTrace.take(maxStackDepth + 3).drop(3)
                                        .forEachIndexed { index, element ->
                                            if (index < maxStackDepth) {
                                                sb.append("   ${index + 1}. ${element.className}.${element.methodName}")
                                                sb.append("(${element.fileName}:${element.lineNumber})\n")
                                            }
                                        }
                                }

                                // 打印参数
                                if (printArgs && param.args.isNotEmpty()) {
                                    sb.append("\n📥 参数列表:\n")
                                    method.parameterTypes.forEachIndexed { index, paramType ->
                                        val argValue = param.args.getOrNull(index)
                                        sb.append(
                                            "   [$index] ${paramType.simpleName} = ${
                                                formatValue(
                                                    argValue
                                                )
                                            }\n"
                                        )
                                    }
                                } else if (printArgs) {
                                    sb.append("\n📥 参数: 无\n")
                                }

                                Logger.d(sb.toString())
                            }

                            override fun afterHookedMethod(param: MethodHookParam) {
                                // 打印返回值
                                if (printReturn) {
                                    val sb = StringBuilder()
                                    sb.append(
                                        "📤 返回值: ${method.returnType.simpleName} = ${
                                            formatValue(
                                                param.result
                                            )
                                        }\n"
                                    )
                                    sb.append("=".repeat(80) + "\n")
                                    Logger.d(sb.toString())
                                }
                            }
                        })
                        hookedCount++
                    } catch (e: Exception) {
                        Logger.e("无法hook方法: ${method.name}", e)
                    }
                }

            Logger.d("✅ 成功监视 $hookedCount 个方法")

        } catch (e: Exception) {
            Logger.e("Watch失败: ${e.message}", e)
        }
    }

    /**
     * 格式化值用于打印
     * 通用格式化函数，不依赖特定类型
     */
    private fun formatValue(value: Any?): String {
        return try {
            when (value) {
                null -> "null"
                is String -> "\"$value\""
                is CharSequence -> "\"$value\""
                is Number -> value.toString()
                is Boolean -> value.toString()
                is Array<*> -> "[${
                    value.take(3).joinToString(", ")
                }${if (value.size > 3) "..." else ""}] (${value.size})"

                is Collection<*> -> "[${
                    value.take(3).joinToString(", ")
                }${if (value.size > 3) "..." else ""}] (${value.size})"

                else -> {
                    val className = value.javaClass.simpleName
                    "$className@${Integer.toHexString(value.hashCode())}"
                }
            }
        } catch (e: Exception) {
            // 任何格式化错误都不应该导致hook崩溃
            "Error:${e.javaClass.simpleName}"
        }
    }


}
