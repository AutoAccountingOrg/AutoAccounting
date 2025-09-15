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

import androidx.annotation.RequiresApi
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.XposedModule
import net.ankio.auto.xposed.core.api.HookerManifest
import java.lang.reflect.Method


class PermissionHooker30 (private val  manifest: HookerManifest, private val mClassLoader: ClassLoader) {


    // IMPORTANT: There are two types of permissions: install and runtime.
    // Android 11, API 30
    private val CLASS_PERMISSION_MANAGER_SERVICE: String = "com.android.server.pm.permission.PermissionManagerService"
    private val CLASS_ANDROID_PACKAGE: String = "com.android.server.pm.parsing.pkg.AndroidPackage"
    private val CLASS_PERMISSION_CALLBACK: String = "com.android.server.pm.permission.PermissionManagerServiceInternal.PermissionCallback"


    @RequiresApi(30)
    fun startHook() {
        try {
            hookGrantPermissions()
        } catch (e: Throwable) {
            manifest.d("Error in PermissionHooker30: $e")
            manifest.e(e)
        }
    }

    private fun hookGrantPermissions() {
        manifest.d("Hooking grantPermissions() for Android 30+")
        val method = findTargetMethod()
        if (method == null) {
            manifest.d("Cannot find the method to grant relevant permission")
        }
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                afterGrantPermissionsSinceAndroid11(param)
            }
        })
    }

    private fun findTargetMethod(): Method? {
        val pmsClass = XposedHelpers.findClass(CLASS_PERMISSION_MANAGER_SERVICE, mClassLoader)
        val androidPackageClass = XposedHelpers.findClass(CLASS_ANDROID_PACKAGE, mClassLoader)
        val callbackClass = XposedHelpers.findClassIfExists(CLASS_PERMISSION_CALLBACK, mClassLoader)

        var method = XposedHelpers.findMethodExactIfExists(
            pmsClass, "restorePermissionState",  /* AndroidPackage pkg   */
            androidPackageClass,  /* boolean replace             */
            Boolean::class.javaPrimitiveType,  /* String packageOfInterest    */
            String::class.java,  /* PermissionCallback callback */
            callbackClass
        )

        if (method == null) { // method restorePermissionState() not found
            val _methods = XposedHelpers.findMethodsByExactParameters(
                pmsClass, Void.TYPE,  /* AndroidPackage pkg   */
                androidPackageClass,  /* boolean replace             */
                Boolean::class.javaPrimitiveType,  /* String packageOfInterest    */
                String::class.java,  /* PermissionCallback callback */
                callbackClass
            )
            if (_methods != null && _methods.isNotEmpty()) {
                method = _methods[0]
            }
        }
        return method
    }

    private fun afterGrantPermissionsSinceAndroid11(param: MethodHookParam) {
        // com.android.server.pm.parsing.pkg.AndroidPackage 对象
        val pkg = param.args[0]

        // final String _packageName = (String) XposedHelpers.getObjectField(pkg, "packageName");
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

                // com.android.server.pm.permission.PermissionsState 对象
                val permissionsState = XposedHelpers.callMethod(ps, "getPermissionsState")

                // Manifest.xml 中声明的permission列表
                // List<String> requestPermissions = pkg.getRequestPermissions();
                @Suppress("UNCHECKED_CAST")
                val requestedPermissions =
                    XposedHelpers.callMethod(pkg, "getRequestedPermissions") as List<String>

                // com.android.server.pm.permission.PermissionSettings mSettings 对象
                val settings = XposedHelpers.getObjectField(permissionManagerService, "mSettings")
                // ArrayMap<String, com.android.server.pm.permission.BasePermission> mPermissions 对象
                val permissions = XposedHelpers.getObjectField(settings, "mPermissions")

                val permissionsToGrant: List<String> = appItems.permissions
                for (permissionToGrant in permissionsToGrant) {
                    if (!requestedPermissions.contains(permissionToGrant)) {
                        val granted = XposedHelpers.callMethod(
                            permissionsState, "hasInstallPermission", permissionToGrant
                        ) as Boolean
                        // grant permissions
                        if (!granted) {
                            // com.android.server.pm.permission.BasePermission bpToGrant
                            val bpToGrant =
                                XposedHelpers.callMethod(permissions, "get", permissionToGrant)
                            val result = XposedHelpers.callMethod(
                                permissionsState,
                                "grantInstallPermission",
                                bpToGrant
                            ) as Int
                            manifest.d("Add $bpToGrant; result = $result")
                        } else {
                            manifest.d("Already have $permissionToGrant permission")
                        }
                    }
                }
            }
        }
    }

}