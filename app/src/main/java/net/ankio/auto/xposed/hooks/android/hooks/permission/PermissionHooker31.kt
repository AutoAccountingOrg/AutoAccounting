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

import android.os.UserHandle
import androidx.annotation.RequiresApi
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.XposedModule
import net.ankio.auto.xposed.core.api.HookerManifest
import java.lang.reflect.Method


class PermissionHooker31(private val  manifest: HookerManifest, private val mClassLoader: ClassLoader) {


    // IMPORTANT: There are two types of permissions: install and runtime.
    // Android 12, API 31
    private val CLASS_PERMISSION_MANAGER_SERVICE: String = "com.android.server.pm.permission.PermissionManagerService"

    private val CLASS_ANDROID_PACKAGE: String = "com.android.server.pm.parsing.pkg.AndroidPackage"
    private val CLASS_PERMISSION_CALLBACK: String = "com.android.server.pm.permission.PermissionManagerService.PermissionCallback"

    
    @RequiresApi(31)
    fun startHook() {
        try {
            hookGrantPermissions()
        } catch (e: Throwable) {
            manifest.d("Error in PermissionHooker31: $e")
            manifest.e(e)
        }
    }

    private fun hookGrantPermissions() {
        manifest.d("Hooking grantPermissions() for Android 31+")
        val method = findTargetMethod()
        if (method == null) {
           manifest.d("Cannot find the method to grant relevant permission")
        }
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                afterGrantPermissionsSinceAndroid12(param)
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

    private fun afterGrantPermissionsSinceAndroid12(param: MethodHookParam) {
        // com.android.server.pm.parsing.pkg.AndroidPackage 对象
        val pkg = param.args[0]

        val _packageName = XposedHelpers.callMethod(pkg, "getPackageName") as String

        for (appItems in XposedModule.get()) {
            if (appItems.packageName == _packageName) {
                manifest.d("PackageName: ${_packageName}")

                // PermissionManagerService 对象
                val permissionManagerService = param.thisObject
                // PackageManagerInternal 对象 mPackageManagerInt
                val mPackageManagerInt =
                    XposedHelpers.getObjectField(permissionManagerService, "mPackageManagerInt")

                // PackageSetting 对象 ps
                // final PackageSetting ps = (PackageSetting) mPackageManagerInt.getPackageSetting(pkg.getPackageName());
                val ps =
                    XposedHelpers.callMethod(mPackageManagerInt, "getPackageSetting", appItems.packageName)

                // Manifest.xml 中声明的permission列表
                // List<String> requestPermissions = pkg.getRequestPermissions();
                @Suppress("UNCHECKED_CAST")
                val requestedPermissions =
                    XposedHelpers.callMethod(pkg, "getRequestedPermissions") as List<String>

                // com.android.server.pm.permission.DevicePermissionState 对象
                val mState = XposedHelpers.getObjectField(permissionManagerService, "mState")

                // UserHandle.USER_ALL
                val filterUserId = param.args[4] as Int
                val USER_ALL = XposedHelpers.getStaticIntField(UserHandle::class.java, "USER_ALL")
                val userIds = if (filterUserId == USER_ALL
                ) XposedHelpers.callMethod(permissionManagerService, "getAllUserIds") as IntArray
                else intArrayOf(filterUserId)

                val permissionsToGrant: List<String> = appItems.permissions

                for (userId in userIds) {
                    // com.android.server.pm.permission.UserPermissionState 对象
                    val userState =
                        XposedHelpers.callMethod(mState, "getOrCreateUserState", userId)
                    val appId = XposedHelpers.callMethod(ps, "getAppId") as Int
                    //  com.android.server.pm.permission.UidPermissionState 对象
                    val uidState =
                        XposedHelpers.callMethod(userState, "getOrCreateUidState", appId)

                    // com.android.server.pm.permission.PermissionRegistry 对象
                    val mRegistry =
                        XposedHelpers.getObjectField(permissionManagerService, "mRegistry")

                    for (permissionToGrant in permissionsToGrant) {
                        if (!requestedPermissions.contains(permissionToGrant)) {
                            val granted = XposedHelpers.callMethod(
                                uidState,
                                "isPermissionGranted",
                                permissionToGrant
                            ) as Boolean
                            if (!granted) {
                                // permission not grant before
                                val bpToGrant = XposedHelpers.callMethod(
                                    mRegistry,
                                    "getPermission",
                                    permissionToGrant
                                )
                                val result = XposedHelpers.callMethod(
                                    uidState,
                                    "grantPermission",
                                    bpToGrant
                                ) as Boolean
                                manifest.d("Add $permissionToGrant; result = $result")
                            } else {
                                // permission has been granted already
                                manifest.d("Already have $permissionToGrant permission")
                            }
                        }
                    }
                }
            }
        }
    }
}