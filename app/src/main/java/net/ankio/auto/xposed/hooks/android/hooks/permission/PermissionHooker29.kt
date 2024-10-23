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

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.Apps
import net.ankio.auto.xposed.core.api.HookerManifest
import java.lang.reflect.Method


class PermissionHooker29(private val  manifest: HookerManifest, private val mClassLoader: ClassLoader) {
    // for Android 28+
    private val CLASS_PERMISSION_MANAGER_SERVICE: String =
        "com.android.server.pm.permission.PermissionManagerService"
    private val CLASS_PERMISSION_CALLBACK: String = "com.android.server.pm.permission.PermissionManagerInternal.PermissionCallback"
    private val CLASS_PACKAGE_PARSER_PACKAGE: String = "android.content.pm.PackageParser.Package"

    // for MIUI 10 Android Q
    private val CLASS_PERMISSION_CALLBACK_Q: String = "com.android.server.pm.permission.PermissionManagerServiceInternal.PermissionCallback"

    fun startHook() {
        try {
            hookGrantPermissions()
        } catch (e: Throwable) {
            manifest.log("Error in PermissionHooker29: $e")
            manifest.logE(e)
        }
    }

    private fun hookGrantPermissions() {
        manifest.logD("Hooking grantPermissions() for Android 28+")
        val method = findTargetMethod()
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                afterGrantPermissionsSinceP(param)
            }
        })
    }

    private fun findTargetMethod(): Method? {
        val pmsClass = XposedHelpers.findClass(CLASS_PERMISSION_MANAGER_SERVICE, mClassLoader)
        val packageClass = XposedHelpers.findClass(CLASS_PACKAGE_PARSER_PACKAGE, mClassLoader)
        var callbackClass = XposedHelpers.findClassIfExists(CLASS_PERMISSION_CALLBACK, mClassLoader)
        if (callbackClass == null) {
            // Android Q PermissionCallback 不一样
            callbackClass = XposedHelpers.findClass(CLASS_PERMISSION_CALLBACK_Q, mClassLoader)
        }

        var method = XposedHelpers.findMethodExactIfExists(
            pmsClass, "grantPermissions",  /* PackageParser.Package pkg   */
            packageClass,  /* boolean replace             */
            Boolean::class.javaPrimitiveType,  /* String packageOfInterest    */
            String::class.java,  /* PermissionCallback callback */
            callbackClass
        )

        if (method == null) { // method grantPermissions() not found
            // Android Q
            method = XposedHelpers.findMethodExactIfExists(
                pmsClass, "restorePermissionState",  /* PackageParser.Package pkg   */
                packageClass,  /* boolean replace             */
                Boolean::class.javaPrimitiveType,  /* String packageOfInterest    */
                String::class.java,  /* PermissionCallback callback */
                callbackClass
            )
            if (method == null) { // method restorePermissionState() not found
                val _methods = XposedHelpers.findMethodsByExactParameters(
                    pmsClass, Void.TYPE,  /* PackageParser.Package pkg   */
                    packageClass,  /* boolean replace             */
                    Boolean::class.javaPrimitiveType,  /* String packageOfInterest    */
                    String::class.java,  /* PermissionCallback callback */
                    callbackClass
                )
                if (_methods != null && _methods.isNotEmpty()) {
                    method = _methods[0]
                }
            }
        }
        return method
    }

    private fun afterGrantPermissionsSinceP(param: MethodHookParam) {
        // android.content.pm.PackageParser.Package 对象
        val pkg = param.args[0]

        val _packageName = XposedHelpers.getObjectField(pkg, "packageName") as String

        for (appItems in Apps.get()) {
            if (appItems.packageName == _packageName) {
                manifest.logD("PackageName: ${_packageName}")
                // PackageParser$Package.mExtras 实际上是 com.android.server.pm.PackageSetting mExtras 对象
                val extras = XposedHelpers.getObjectField(pkg, "mExtras")
                // com.android.server.pm.permission.PermissionsState 对象
                val permissionsState = XposedHelpers.callMethod(extras, "getPermissionsState")

                // Manifest.xml 中声明的permission列表
                val requestedPermissions =
                    XposedHelpers.getObjectField(pkg, "requestedPermissions") as List<String>

                // com.android.server.pm.permission.PermissionSettings mSettings 对象
                val settings = XposedHelpers.getObjectField(param.thisObject, "mSettings")
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
                            manifest.logD("Add $bpToGrant; result = $result")
                        } else {
                            manifest.logD("Already have $permissionToGrant permission")
                        }
                    }
                }
            }
        }
    }
}