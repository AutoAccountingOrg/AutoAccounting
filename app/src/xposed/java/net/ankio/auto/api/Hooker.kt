/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.api

/*
 * Copyright (C) 2021 dreamn(dream@dreamn.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.launch
import net.ankio.auto.HookMainApp
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.HookUtils
import net.ankio.dex.Dex
import net.ankio.dex.model.Clazz

abstract class Hooker : iHooker {
    abstract var partHookers: MutableList<PartHooker>
    open val applicationClazz = "android.app.Application"
    private var TAG = "AutoAccounting"

    private fun hookMainInOtherAppContext(classLoader: ClassLoader) {
        var hookStatus = false

        fun onCachedApplication(application: Application) {
            hookStatus = true
            runCatching {
                initLoadPackage(application.classLoader, application)
            }.onFailure {
                XposedBridge.log("自动记账Hook异常..${it.message}.")
                Log.e("AutoAccountingError", it.message.toString())
                it.printStackTrace()
            }
        }

        runCatching {
            XposedHelpers.findAndHookMethod(
                applicationClazz,
                classLoader,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        if (hookStatus) {
                            return
                        }

                        val context = param.thisObject as Application

                        onCachedApplication(context)
                    }
                },
            )
        }.onFailure {
            runCatching {
                XposedHelpers.findAndHookMethod(
                    applicationClazz,
                    classLoader,
                    "attachBaseContext",
                    Context::class.java,
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            super.afterHookedMethod(param)
                            if (hookStatus) {
                                return
                            }

                            val context = param.thisObject as Application

                            onCachedApplication(context)
                        }
                    },
                )
            }
        }
    }

    fun initLoadPackage(
        classLoader: ClassLoader,
        application: Application,
    ) {
        XposedBridge.log("[$TAG] Welcome to AutoAccounting")


        HookUtils.setApplication(application)

        if (!autoAdaption(application, classLoader)) {
            XposedBridge.log("[AutoAccounting]自动适配失败，停止模块运行")
            return
        }

        hookLoadPackage(classLoader, application)

        AppUtils.getService().connect()

        for (hook in partHookers) {
            try {
                HookUtils.logD(
                    "main process",
                    "init hook ${hook.hookName}",
                )
                hook.onInit(classLoader, application)
            } catch (e: Exception) {
                HookUtils.logD(
                    "main process",
                    "hook error : ${e.message}.",
                )
                HookUtils.writeData("adaptation", "0")

            }
        }
        HookUtils.logD("main process", "hook success, welcome to use 自动记账")
    }

    @Throws(ClassNotFoundException::class)
    abstract fun hookLoadPackage(
        classLoader: ClassLoader,
        context: Context,
    )

    override fun onLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        val pkg = lpparam?.packageName
        val processName = lpparam?.processName
        if (lpparam != null) {
            if (!lpparam.isFirstApplication) return
        }
        if (
            pkg != packPageName ||
            processName != packPageName
        ) {
            return
        }
        hookMainInOtherAppContext(lpparam.classLoader)
    }

    open var clazz = HashMap<String, String>()

    open val rule = ArrayList<Clazz>()

    fun autoAdaption(
        context: Application,
        classLoader: ClassLoader,
    ): Boolean {
        val code = HookUtils.getVersionCode()
        if (rule.size == 0) {
            return true
        }
        val adaptationVersion = HookUtils.readData("adaptation").toIntOrNull() ?: 0
        if (adaptationVersion == code) {
            runCatching {
                clazz =
                    Gson().fromJson(
                        HookUtils.readData("clazz"),
                        HashMap::class.java,
                    ) as HashMap<String, String>
                if (clazz.size != rule.size) {
                    throw Exception("适配失败")
                }
            }.onFailure {
                HookUtils.writeData("adaptation", "0")
                XposedBridge.log(it)
            }.onSuccess {
                return true
            }
        }
        HookUtils.logD("main process"," ${context.packageResourcePath}")
        HookUtils.toast("自动记账开始适配中...")
        val total = rule.size
        val hashMap = Dex.findClazz(context.packageResourcePath, classLoader, rule)
        if (hashMap.size == total) {
            HookUtils.writeData("adaptation", code.toString())
            clazz = hashMap
            HookUtils.writeData("clazz", Gson().toJson(clazz))
            HookUtils.logD("main process"," 适配成功:$hashMap")
            HookUtils.toast("适配成功")
            return true
        } else {
            HookUtils.logD("main process"," 适配失败:$hashMap")
            HookUtils.toast("适配失败")
            return false
        }
    }
}
