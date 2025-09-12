package net.ankio.auto.xposed.core.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.utils.AppRuntime
import java.lang.reflect.Method
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Xposed hooker utility for streamlined hooking operations.
 */
object Hooker {

    private val logger = KotlinLogging.logger(this::class.java.name)

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
            logger.error(e) { "Class not found: $clazz" }
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid parameter type: ${e.message}" }
        } catch (e: Exception) {
            logger.error(e) { "Error hooking method after: $clazz.$method - ${e.message}" }
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
            logger.error(e) { "Error hooking method before: $clazz.$method - ${e.message}" }
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
            logger.error(e) { "Class not found: $clazz" }
        } catch (e: Exception) {
            logger.error(e) { "Error hooking method before: $clazz.$method - ${e.message}" }
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
            logger.error(e) { "Error hooking method before: $clazz.$method - ${e.message}" }
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
            logger.error(e) { "Error hooking once method after: $clazz.$method - ${e.message}" }
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
            logger.error(e) { "Error hooking once method before: $clazz.$method - ${e.message}" }
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
                logger.error(e) { "Error hooking method before: ${method.name} - ${e.message}" }
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
                logger.error(e) { "Error hooking method after: ${method.name} - ${e.message}" }
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
            logger.error(e) { "Class not found: $clazz" }
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid parameter type: ${e.message}" }
        } catch (e: Exception) {
            logger.error(e) { "Error replacing method: $clazz.$method - ${e.message}" }
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
            logger.error(e) { "Class not found: $clazz" }
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid parameter type: ${e.message}" }
        } catch (e: Exception) {
            logger.error(e) { "Error replacing return value: $clazz.$method - ${e.message}" }
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
                is ClassNotFoundException -> logger.error(e) { "Class not found: $clazz" }
                else -> logger.error(e) { "Error hooking method: $clazz.$methodName" }
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
                is ClassNotFoundException -> logger.error(e) { "Class not found: $clazz" }
                else -> logger.error(e) { "Error hooking method: $clazz.$methodName" }
            }
        }
    }


}