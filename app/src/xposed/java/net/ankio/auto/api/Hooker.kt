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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.HookMainApp
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.HookUtils
import net.ankio.dex.Dex
import net.ankio.dex.model.Clazz

abstract class Hooker : iHooker {
    abstract var partHookers: MutableList<PartHooker>
    private var TAG = "AutoAccounting"
    lateinit var hookUtils: HookUtils
    private fun hookMainInOtherAppContext() {
        var hookStatus = false
        XposedHelpers.findAndHookMethod(
            Application::class.java, "attach",
            Context::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    if(hookStatus){
                        return
                    }
                    hookStatus = true
                    val context = param.args[0] as Context
                    val application = param.thisObject as Application
                    runCatching {
                        initLoadPackage(context.classLoader,application)
                    }.onFailure {
                        XposedBridge.log("自动记账Hook异常..${it.message}.")
                        Log.e("AutoAccountingError", it.message.toString())
                        it.printStackTrace()
                    }
                }
        })
    }


    fun initLoadPackage(classLoader: ClassLoader?,application: Application){
        XposedBridge.log("[$TAG] Welcome to AutoAccounting")
        if(classLoader==null){
            XposedBridge.log("[AutoAccounting]"+this.appName+"hook失败: classloader 或 context = null")
            return
        }


        if(!autoAdaption(application,classLoader)){
            XposedBridge.log("[AutoAccounting]自动适配失败，停止模块运行")
            return
        }

        hookLoadPackage(classLoader, application)
        hookUtils = HookUtils(application, packPageName)
        hookUtils.scope.launch {
            for (hook in partHookers) {
                try {
                    hookUtils.logD(HookMainApp.getTag(appName,packPageName),"正在初始化Hook...")
                    withContext(Dispatchers.Main){
                        hook.onInit(classLoader,application)
                    }
                }catch (e:Exception){
                    e.message?.let { Log.e("AutoAccountingError", it) }
                    XposedBridge.log(e)
                    if(startAutoApp(e,application))return@launch
                    hookUtils.log(HookMainApp.getTag(),"自动记账Hook异常..${e.message}.")
                }
            }
        }

    }

   suspend fun startAutoApp(e:Throwable,application: Application): Boolean = withContext(Dispatchers.Main) {
       var result = false
       if(e is AutoServiceException){
            //不在自动记账跳转
            if(application.packageName!=BuildConfig.APPLICATION_ID){
                ActiveUtils.startApp(application)
            }
           result = true
        }
      result
    }

    @Throws(ClassNotFoundException::class)
    abstract fun hookLoadPackage(classLoader: ClassLoader,context: Context)
    override fun onLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        val pkg = lpparam?.packageName
        val processName = lpparam?.processName
        if (lpparam != null) {
            if (!lpparam.isFirstApplication) return
        }
        if (pkg != packPageName || processName != packPageName) return
        hookMainInOtherAppContext()
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