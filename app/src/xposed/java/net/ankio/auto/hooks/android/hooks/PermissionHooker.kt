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

import android.app.Application
import android.os.Build
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.Apps
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker

/**
 * PermissionHooker
 * 授权
 */
class PermissionHooker:PartHooker {
    /**
     * 挂钩PermissionManagerService，并自动向特定的应用程序授予特定权限。
     * @param hookerManifest HookerManifest
     * @param application Application
     */
    override fun hook(hookerManifest: HookerManifest,application: Application?,classLoader: ClassLoader) {
        val sdk: Int = Build.VERSION.SDK_INT
        try {
            // 根据SDK版本，找到PermissionManagerService类
            val permissionManagerService = XposedHelpers.findClass(
                if (sdk >= Build.VERSION_CODES.TIRAMISU /* Android 13+ */)
                    "com.android.server.pm.permission.PermissionManagerServiceImpl"
                else
                    "com.android.server.pm.permission.PermissionManagerService",
                classLoader
            )

            // com.android.server.pm.pkg.AndroidPackage
            // 找到AndroidPackage类
            val androidPackage = XposedHelpers.findClass(
                if (sdk >= Build.VERSION_CODES.TIRAMISU /* Android 14+ */)
                    "com.android.server.pm.pkg.AndroidPackage"
                else
                    "com.android.server.pm.parsing.pkg.AndroidPackage",
                classLoader
            )

            // 根据SDK版本，找到PermissionCallback类
            val permissionCallback = XposedHelpers.findClass(
                if (sdk >= Build.VERSION_CODES.TIRAMISU /* Android 13+ */)
                    "com.android.server.pm.permission.PermissionManagerServiceImpl\$PermissionCallback"
                else
                    "com.android.server.pm.permission.PermissionManagerService\$PermissionCallback",
                classLoader
            )

            // Hook PermissionManagerService(Impl) - restorePermissionState 方法
            XposedHelpers.findAndHookMethod(permissionManagerService, "restorePermissionState",
                androidPackage,
                Boolean::class.javaPrimitiveType,
                String::class.java, permissionCallback,
                Int::class.javaPrimitiveType, object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // 获取方法参数
                        val pkg = param.args[0]
                        val filterUserId = param.args[4] as Int

                        // 获取字段
                        val mState = XposedHelpers.getObjectField(param.thisObject, "mState")
                        val mRegistry = XposedHelpers.getObjectField(param.thisObject, "mRegistry")
                        val mPackageManagerInt =
                            XposedHelpers.getObjectField(param.thisObject, "mPackageManagerInt")

                        // 继续处理？
                        val packageName = XposedHelpers.callMethod(pkg, "getPackageName") as String
                        val ps = XposedHelpers.callMethod(
                            mPackageManagerInt,
                            if (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE /* Android 14+ */)
                                "getPackageStateInternal"
                            else if (sdk >= Build.VERSION_CODES.TIRAMISU /* Android 13+ */)
                                "getPackageStateInternal"
                            else
                                "getPackageSetting",
                            packageName
                        )
                        if (ps == null) return

                        // 获取所有用户ID
                        val getAllUserIds =
                            XposedHelpers.callMethod(param.thisObject, "getAllUserIds") as IntArray
                        val userHandle_USER_ALL = XposedHelpers.getStaticIntField(
                            Class.forName("android.os.UserHandle"),
                            "USER_ALL"
                        )
                        val userIds =
                            if (filterUserId == userHandle_USER_ALL) getAllUserIds else intArrayOf(
                                filterUserId
                            )

                        // 对每个用户ID进行处理
                        for (userId in userIds) {
                            var requestedPermissions: List<String?>
                            val userState =
                                XposedHelpers.callMethod(mState, "getOrCreateUserState", userId)
                            val appId = XposedHelpers.callMethod(ps, "getAppId") as Int
                            val uidState =
                                XposedHelpers.callMethod(userState, "getOrCreateUidState", appId)

                            // 遍历所有应用程序, 如果是指定的应用程序，则授予指定的权限
                            for (app in Apps.get()){
                                if (packageName == app.packageName){
                                    requestedPermissions = XposedHelpers.callMethod(
                                        pkg,
                                        "getRequestedPermissions"
                                    ) as List<String?>
                                    for (permission in app.permissions){
                                        grantInstallOrRuntimePermission(
                                            requestedPermissions, uidState, mRegistry,
                                            permission
                                        )
                                    }
                                }
                            }
                        }
                    }
                })
        } catch (e: Exception) {
            // 如果出现异常，记录日志
            hookerManifest.log("PermissionHooker: $e")
            hookerManifest.logE(e)
        }
    }
    /**
     * 授予安装或运行时权限
     *
     * @param requestedPermissions 请求的权限列表
     * @param uidState UID状态对象
     * @param registry 权限注册表对象
     * @param permission 要授予的权限
     */
    private fun grantInstallOrRuntimePermission(
        requestedPermissions: List<String?>, uidState: Any,
        registry: Any, permission: String
    ) {
        // 如果请求的权限列表中不包含该权限，则授予该权限
        if (!requestedPermissions.contains(permission)) XposedHelpers.callMethod(
            uidState, "grantPermission",
            XposedHelpers.callMethod(registry, "getPermission", permission)
        )
    }
}