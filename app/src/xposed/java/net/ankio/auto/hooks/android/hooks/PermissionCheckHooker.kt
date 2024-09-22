/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.hooks.android.hooks

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.BuildConfig
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker

class PermissionCheckHooker:PartHooker() {
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {

        hookCheckPermission(hookerManifest,classLoader)


        //////////AppOpsManager的权限设置拦截不成功不知道为什么，换用下面的方法直接授权
        setOverlaysAllowed(BuildConfig.APPLICATION_ID,application!!.baseContext)

    }

    private fun hookCheckPermission(hookerManifest: HookerManifest, classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod(
            "android.app.ContextImpl",
            classLoader,
            "checkPermission",
            String::class.java,
            Int::class.javaPrimitiveType,//pid
            Int::class.javaPrimitiveType,//uid
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    val permission = param.args[0] as String
                    val pid = param.args[1] as Int
                    val uid = param.args[2] as Int
                    val packageManager =  XposedHelpers.callMethod(param.thisObject, "getPackageManager")
                    val packages: Array<String> = runCatching {
                        XposedHelpers.callMethod(packageManager, "getPackagesForUid", uid) as Array<String>
                    }.getOrNull()?:return

                    if (packages.isEmpty()) return


                    val packageName = packages[0]

                    // 允许自动记账的所有权限
                    if (packageName == BuildConfig.APPLICATION_ID) {
                        param.result = PackageManager.PERMISSION_GRANTED

                        hookerManifest.log("return checkPermission($permission): $permission, $pid, $uid, $packageName,${param.result}")
                    }

                }
            }
        )
    }
    // 授权悬浮窗权限给自动记账
    private fun setOverlaysAllowed(packageName:String,mContext: Context) {
        val appOpsManager = mContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val pm = mContext.packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val alertWindow = XposedHelpers.getStaticIntField(AppOpsManager::class.java, "OP_SYSTEM_ALERT_WINDOW")
        XposedHelpers.callMethod(appOpsManager, "setMode", alertWindow,
            packageInfo.applicationInfo.uid, packageName, AppOpsManager.MODE_ALLOWED)


    }
}