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
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.Apps
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker

/**
 * PermissionHooker
 * 授权
 */
class PermissionHooker:PartHooker() {
    /**
     * hook PermissionManagerService，并自动向特定的应用程序授予特定权限。
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
                if (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE /* Android 14+ */)
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
            // /**
            //2548       * Restore the permission state for a package.
            //2549       *
            //2550       * <ul>
            //2551       *     <li>During boot the state gets restored from the disk</li>
            //2552       *     <li>During app update the state gets restored from the last version of the app</li>
            //2553       * </ul>
            //2554       *
            //2555       * @param pkg the package the permissions belong to
            //2556       * @param replace if the package is getting replaced (this might change the requested
            //2557       *                permissions of this package)
            //2558       * @param changingPackageName the name of the package that is changing
            //2559       * @param callback Result call back
            //2560       * @param filterUserId If not {@link UserHandle.USER_ALL}, only restore the permission state for
            //2561       *                     this particular user
            //2562       */
            //2563      private void restorePermissionState(@NonNull AndroidPackage pkg, boolean replace,
            //2564              @Nullable String changingPackageName, @Nullable PermissionCallback callback,
            //2565              @UserIdInt int filterUserId) {
            //2566          // IMPORTANT: There are two types of permissions: install and runtime.
            //2567          // Install time permissions are granted when the app is installed to
            //2568          // all device users and users added in the future. Runtime permissions
            //2569          // are granted at runtime explicitly to specific users. Normal and signature
            //2570          // protected permissions are install time permissions. Dangerous permissions
            //2571          // are install permissions if the app's target SDK is Lollipop MR1 or older,
            //2572          // otherwise they are runtime permissions. This function does not manage
            //2573          // runtime permissions except for the case an app targeting Lollipop MR1
            //2574          // being upgraded to target a newer SDK, in which case dangerous permissions
            //2575          // are transformed from install time to runtime ones.
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
                        val mPackageManagerInt = XposedHelpers.getObjectField(param.thisObject, "mPackageManagerInt")

                        val packageName = XposedHelpers.callMethod(pkg, "getPackageName") as String

                     //   XposedBridge.log("PermissionHooker: $packageName")

                        val ps = XposedHelpers.callMethod(
                            mPackageManagerInt,
                            if (sdk >= Build.VERSION_CODES.TIRAMISU /* Android 13+ */)
                                "getPackageStateInternal"
                            else
                                "getPackageSetting",
                            packageName
                        )
                        if (ps == null) {
                            XposedBridge.log("PermissionHooker: ps is null")
                            return
                        }

                        // 获取所有用户ID
                        val getAllUserIds =
                            XposedHelpers.callMethod(param.thisObject, "getAllUserIds") as IntArray
                        val userHandleAll = XposedHelpers.getStaticIntField(
                            Class.forName("android.os.UserHandle"),
                            "USER_ALL"
                        )
                        val userIds =
                            if (filterUserId == userHandleAll) getAllUserIds else intArrayOf(
                                filterUserId
                            )

                        // 对每个用户ID进行处理
                        for (userId in userIds) {
                            val userState = XposedHelpers.callMethod(mState, "getOrCreateUserState", userId)
                            val appId = XposedHelpers.callMethod(ps, "getAppId") as Int
                            val uidState = XposedHelpers.callMethod(userState, "getOrCreateUidState", appId)
                            // 遍历所有应用程序, 如果是指定的应用程序，则授予指定的权限
                            for (app in Apps.get()){

                               // XposedBridge.log("PermissionHooker: $packageName [$permissions]")
                                if (packageName == app.packageName){
                                    val permissions = XposedHelpers.callMethod(pkg,"getRequestedPermissions") as List<String>
                                    for (permission in app.permissions){
                                        if (!permissions.contains(permission)){
                                            val result = XposedHelpers.callMethod(
                                                uidState, "grantPermission",
                                                XposedHelpers.callMethod(mRegistry, "getPermission", permission)
                                            )
                                            XposedBridge.log("PermissionHooker: grantPermission $packageName [$permission]: $result")
                                        }

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

}