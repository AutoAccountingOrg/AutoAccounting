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
import net.ankio.auto.utils.HookUtils
import net.ankio.dex.Dex
import net.ankio.dex.model.Clazz

abstract class Hooker : iHooker {
    abstract var partHookers: MutableList<PartHooker>
     open val applicationClazz = "android.app.Application"
    private var TAG = "AutoAccounting"
    lateinit var hookUtils: HookUtils
    private fun hookMainInOtherAppContext(classLoader: ClassLoader) {
        var hookStatus = false



        fun onCachedApplication(application: Application){
            hookStatus = true
            runCatching {
                initLoadPackage(application.classLoader,application)
            }.onFailure {
                XposedBridge.log("自动记账Hook异常..${it.message}.")
                Log.e("AutoAccountingError", it.message.toString())
                it.printStackTrace()
            }
        }



        runCatching {
            XposedHelpers.findAndHookMethod(
                applicationClazz,classLoader,"attach",
                Context::class.java, object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        if(hookStatus){
                            return
                        }

                        val context = param.thisObject as Application

                        onCachedApplication(context)
                    }
                })
        }.onFailure {
            runCatching {
                XposedHelpers.findAndHookMethod(
                    applicationClazz,classLoader,"attachBaseContext",
                    Context::class.java, object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            super.afterHookedMethod(param)
                            if(hookStatus){
                                return
                            }

                            val context = param.thisObject as Application

                            onCachedApplication(context)
                        }
                    })
            }
        }




    }


    fun initLoadPackage(classLoader: ClassLoader,application: Application){
        XposedBridge.log("[$TAG] Welcome to AutoAccounting")

        hookUtils = HookUtils(application, packPageName)

        if(!autoAdaption(application,classLoader)){
            XposedBridge.log("[AutoAccounting]自动适配失败，停止模块运行")
            return
        }

        hookLoadPackage(classLoader, application)

        for (hook in partHookers) {
            try {
                hookUtils.scope.launch {
                    hookUtils.logD(HookMainApp.getTag(appName,packPageName),"正在初始化Hook ${hook.hookName}")
                }
                 hook.onInit(classLoader,application)
            }catch (e:Exception){
                e.message?.let { Log.e("AutoAccountingError", it) }
                XposedBridge.log(e)
                hookUtils.scope.launch {
                    hookUtils.logD(HookMainApp.getTag(appName,packPageName),"正在初始化Hook ${hook.hookName}")

                }
                if(hookUtils.startAutoApp(e,application))return
                hookUtils.scope.launch {
                    hookUtils.log(HookMainApp.getTag(),"自动记账Hook异常..${e.message}.")

                }

            }
        }
        hookUtils.scope.launch {
            hookUtils.logD(HookMainApp.getTag(appName,packPageName),"欢迎使用自动记账...")
        }




    }



    @Throws(ClassNotFoundException::class)
    abstract fun hookLoadPackage(classLoader: ClassLoader,context: Context)
    override fun onLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        val pkg = lpparam?.packageName
        val processName = lpparam?.processName
        if (lpparam != null) {
            if (!lpparam.isFirstApplication) return
        }
        if (
            pkg != packPageName
            || processName != packPageName
            ) return
        hookMainInOtherAppContext(lpparam.classLoader)
    }
    open var clazz = HashMap<String,String>()

    open val rule = ArrayList<Clazz>()
    fun autoAdaption(context: Application, classLoader: ClassLoader):Boolean{
        val code = hookUtils.getVersionCode()
        if(rule.size==0){
            return true
        }
        val adaptationVersion  = hookUtils.readData("adaptation").toIntOrNull() ?: 0
        if(adaptationVersion == code){
            runCatching {
                clazz = Gson().fromJson(hookUtils.readData("clazz"),HashMap::class.java) as HashMap<String, String>
                if(clazz.size!=rule.size){
                    throw Exception("适配失败")
                }
            }.onFailure {
                hookUtils.writeData("adaptation","0")
                XposedBridge.log(it)
            }.onSuccess {
                return true
            }

        }
        XposedBridge.log("context? ${context.packageResourcePath}")
        hookUtils.toast("自动记账开始适配中...")
        val total = rule.size
        val hashMap = Dex.findClazz(context.packageResourcePath, classLoader, rule)
        if(hashMap.size==total){
            hookUtils.writeData("adaptation",code.toString())
            clazz = hashMap
            hookUtils.writeData("clazz", Gson().toJson(clazz))
            XposedBridge.log("适配成功:${hashMap}")
            hookUtils.toast("适配成功")
            return  true
        }else{
            XposedBridge.log("适配失败:${hashMap}")
            hookUtils.toast("适配失败")
            return false
        }


    }

}