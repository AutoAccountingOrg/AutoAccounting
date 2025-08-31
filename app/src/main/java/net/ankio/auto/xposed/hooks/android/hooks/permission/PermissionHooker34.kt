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

package net.ankio.auto.xposed.hooks.android.hooks.permission

import android.os.Build
import android.os.UserHandle
import androidx.annotation.RequiresApi
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.XposedModule
import net.ankio.auto.xposed.core.api.HookerManifest
import java.lang.reflect.Method


class PermissionHooker34(private val  manifest: HookerManifest, private val mClassLoader: ClassLoader)  {


    // IMPORTANT: There are two types of permissions: install and runtime.
    // Android 14, API 34
    private val CLASS_PERMISSION_MANAGER_SERVICE: String = "com.android.server.pm.permission.PermissionManagerServiceImpl"

    private val CLASS_ANDROID_PACKAGE: String = "com.android.server.pm.pkg.AndroidPackage"
    private val CLASS_PERMISSION_CALLBACK: String = "$CLASS_PERMISSION_MANAGER_SERVICE.PermissionCallback"



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun startHook() {
        try {
            hookGrantPermissions()
        } catch (e: Throwable) {
            manifest.logD("Error in PermissionHooker34: $e")
            manifest.logE(e)
        }
    }

    private fun hookGrantPermissions() {
        manifest.logD("Hooking grantPermissions() for Android 34+")
        val method = findTargetMethod()
        if (method == null) {
            manifest.logD("Cannot find the method to grant relevant permission")
            return
        }
        XposedBridge.hookMethod(method, object :XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                afterRestorePermissionStateSinceAndroid14(param)
            }
        })
    }

    private fun findTargetMethod(): Method? {
        val pmsClass = XposedHelpers.findClass(CLASS_PERMISSION_MANAGER_SERVICE, mClassLoader)
        val androidPackageClass = XposedHelpers.findClass(CLASS_ANDROID_PACKAGE, mClassLoader)
        val callbackClass = XposedHelpers.findClassIfExists(CLASS_PERMISSION_CALLBACK, mClassLoader)

        // 精确匹配
        var method = XposedHelpers.findMethodExactIfExists(
            pmsClass, "restorePermissionState",  /* AndroidPackage pkg          */
            androidPackageClass,  /* boolean replace             */
            Boolean::class.javaPrimitiveType,  /* String packageOfInterest    */
            String::class.java,  /* PermissionCallback callback */
            callbackClass,  /* int filterUserId            */
            Int::class.javaPrimitiveType
        )

        if (method == null) { // method restorePermissionState() not found
            // 参数类型精确匹配
            val _methods = XposedHelpers.findMethodsByExactParameters(
                pmsClass, Void.TYPE,  /* AndroidPackage pkg          */
                androidPackageClass,  /* boolean replace             */
                Boolean::class.javaPrimitiveType,  /* String packageOfInterest    */
                String::class.java,  /* PermissionCallback callback */
                callbackClass,  /* int filterUserId            */
                Int::class.javaPrimitiveType
            )
            if (_methods != null && _methods.size > 0) {
                method = _methods[0]
            }
        }
        return method
    }

    private fun afterRestorePermissionStateSinceAndroid14(param: MethodHookParam) {
        // com.android.server.pm.pkg.AndroidPackage 对象
        val pkg = param.args[0]

        val _packageName = XposedHelpers.callMethod(pkg, "getPackageName") as String

        for (appItems in XposedModule.get()) {
            if (appItems.packageName == _packageName) {
                manifest.logD("PackageName: ${_packageName}")

                // PermissionManagerServiceImpl 对象
                val pmsImpl = param.thisObject

                // UserHandle.USER_ALL
                val filterUserId = param.args[4] as Int
                val USER_ALL = XposedHelpers.getStaticIntField(UserHandle::class.java, "USER_ALL")
                val userIds = if (filterUserId == USER_ALL
                ) XposedHelpers.callMethod(pmsImpl, "getAllUserIds") as IntArray
                else intArrayOf(filterUserId)

                val permissionsToGrant: List<String> = appItems.permissions

                // PackageManagerInternal 对象 mPackageManagerInt
                val mPackageManagerInt =
                    XposedHelpers.getObjectField(pmsImpl, "mPackageManagerInt")

                // PackageStateInternal 对象 ps
                // final PackageStateInternal ps = mPackageManagerInt.getPackageStateInternal(pkg.getPackageName());
                val ps = XposedHelpers.callMethod(
                    mPackageManagerInt,
                    "getPackageStateInternal",
                    appItems.packageName
                )

                // Manifest.xml 中声明的permission列表
                // List<String> requestPermissions = pkg.getRequestedPermissions();
                val requestedPermissions =
                    XposedHelpers.callMethod(pkg, "getRequestedPermissions") as List<String>

                // com.android.server.pm.permission.DevicePermissionState 对象
                val mState = XposedHelpers.getObjectField(pmsImpl, "mState")

                // com.android.server.pm.permission.PermissionRegistry 对象
                val mRegistry = XposedHelpers.getObjectField(pmsImpl, "mRegistry")

                for (userId in userIds) {
                    // com.android.server.pm.permission.UserPermissionState 对象
                    val userState =
                        XposedHelpers.callMethod(mState, "getOrCreateUserState", userId)
                    val appId = XposedHelpers.callMethod(ps, "getAppId") as Int
                    //  com.android.server.pm.permission.UidPermissionState 对象
                    val uidState =
                        XposedHelpers.callMethod(userState, "getOrCreateUidState", appId)

                    for (permissionToGrant in permissionsToGrant) {
                        if (!requestedPermissions.contains(permissionToGrant)) {
                            val granted = XposedHelpers.callMethod(
                                uidState,
                                "isPermissionGranted",
                                permissionToGrant
                            ) as Boolean
                            if (!granted) {
                                // permission not grant before
                                // final Permission bp = mRegistry.getPermission(permName);
                                val bpToGrant = XposedHelpers.callMethod(
                                    mRegistry,
                                    "getPermission",
                                    permissionToGrant
                                )
                                // uidState.grantPermission(bp)
                                val result = XposedHelpers.callMethod(
                                    uidState,
                                    "grantPermission",
                                    bpToGrant
                                ) as Boolean
                                manifest.logD("Add $permissionToGrant; result = $result")
                            } else {
                                // permission has been granted already
                                manifest.logD("Already have $permissionToGrant permission")
                            }
                        }
                    }
                }
            }
        }
    }
}