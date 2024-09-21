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
import de.robv.android.xposed.XposedBridge
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
                    }

                }
            }
        )


        //////////AppOpsManager的权限设置拦截不成功不知道为什么，换用下面的方法直接授权

     /*   // public int noteOpNoThrow(int op, int uid, @Nullable String packageName,
        //            @Nullable String attributionTag, @Nullable String message) {

        XposedHelpers.findAndHookMethod("android.app.AppOpsManager", classLoader, "noteOpNoThrow",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java, String::class.java, String::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                val op = param.args[0] as Int
                val uid = param.args[1] as Int
                val packageName = param.args[2] as String
                if (packageName == BuildConfig.APPLICATION_ID) {
                    hookerManifest.log("noteOpNoThrow($op):  $packageName,${param.result}")
                    // 允许悬浮窗权限
                    param.result = AppOpsManager.MODE_ALLOWED

                    hookerManifest.log("return noteOpNoThrow($op): $op, $uid, $packageName,${param.result}")

                    XposedBridge.log(Throwable())
                }
            }
        })

        //android.app.AppOpsManager
         //public int checkOpNoThrow(int op, int uid, String packageName) {
        XposedHelpers.findAndHookMethod("android.app.AppOpsManager", classLoader, "checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                val op = param.args[0] as Int
                val uid = param.args[1] as Int
                val packageName = param.args[2] as String


                if (packageName == BuildConfig.APPLICATION_ID) {
                    hookerManifest.log("checkOpNoThrow($op):  $packageName,${param.result}")
                    // 允许悬浮窗权限
                    param.result = AppOpsManager.MODE_ALLOWED

                    hookerManifest.log("return checkOpNoThrow($op): $op, $uid, $packageName,${param.result}")

                    XposedBridge.log(Throwable())
                }
            }
        })
        //android.provider.Settings
        //public static boolean isCallingPackageAllowedToPerformAppOpsProtectedOperation(Context context,
        //            int uid, String callingPackage, String callingAttributionTag, boolean throwException,
        //            int appOpsOpCode, String[] permissions, boolean makeNote) {
        XposedHelpers.findAndHookMethod("android.provider.Settings", classLoader, "isCallingPackageAllowedToPerformAppOpsProtectedOperation", Context::class.java,
            Int::class.javaPrimitiveType, String::class.java, String::class.java, Boolean::class.javaPrimitiveType,
            Int::class.javaPrimitiveType, Array<String>::class.java, Boolean::class.javaPrimitiveType, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                val uid = param.args[1] as Int
                val callingPackage = param.args[2] as String
                if (callingPackage == BuildConfig.APPLICATION_ID) {
                    hookerManifest.log("isCallingPackageAllowedToPerformAppOpsProtectedOperation:  $callingPackage,${param.result}")
                    // 允许悬浮窗权限
                    param.result = true

                    hookerManifest.log("return isCallingPackageAllowedToPerformAppOpsProtectedOperation: $uid, $callingPackage,${param.result}")

                    XposedBridge.log(Throwable())
                }
            }
        })

*/
        setOverlaysAllowed(BuildConfig.APPLICATION_ID,application!!.baseContext)

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