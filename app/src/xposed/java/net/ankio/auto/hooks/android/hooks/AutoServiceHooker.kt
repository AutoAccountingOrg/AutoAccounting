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
import android.content.Intent
import android.os.Build
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.BuildConfig
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker

// 解除自动记账后台服务限制
class AutoServiceHooker : PartHooker() {
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
           hook34(classLoader)
       } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
           hook33(classLoader)
       } else {
           hook29(classLoader)
       }
    }

    fun hook34(classLoader: ClassLoader){
        // private ServiceLookupResult retrieveServiceLocked(Intent service,
        //4115             String instanceName, boolean isSdkSandboxService, int sdkSandboxClientAppUid,
        //4116             String sdkSandboxClientAppPackage, String resolvedType,
        //4117             String callingPackage, int callingPid, int callingUid, int userId,
        //4118             boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal,
        //4119             boolean allowInstant, ForegroundServiceDelegationOptions fgsDelegateOptions,
        //4120             boolean inSharedIsolatedProcess) {

        XposedHelpers.findAndHookMethod("com.android.server.am.ActiveServices",
            classLoader,
            "retrieveServiceLocked",
            Intent::class.java,//Intent service,
            //String instanceName, boolean isSdkSandboxService, int sdkSandboxClientAppUid,
            String::class.java,
            Boolean::class.java,
            Int::class.java,
            //String sdkSandboxClientAppPackage, String resolvedType,
            String::class.java,
            String::class.java,
            //String callingPackage, int callingPid, int callingUid, int userId,
            String::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            //boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal,
            Boolean::class.java,
            Boolean::class.java,
            Boolean::class.java,
            //boolean allowInstant, ForegroundServiceDelegationOptions fgsDelegateOptions,
            Boolean::class.java,
            "com.android.server.am.ForegroundServiceDelegationOptions",
            //boolean inSharedIsolatedProcess
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (param?.args?.get(6) == null) return
                    val packageName = param.args[6] as String
                    val serviceLookupResult = param.result ?: return
                    param.result = setServiceRecord(packageName,serviceLookupResult)
                    // param?.result = false
                }
            })

    }

    fun hook29(classLoader: ClassLoader){

        // Android 10
        // private ServiceLookupResult retrieveServiceLocked(Intent service,
        //2045             String instanceName, String resolvedType, String callingPackage,
        //2046             int callingPid, int callingUid, int userId,
        //2047             boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal,
        //2048             boolean allowInstant) {

        // Android 11
        //private ServiceLookupResult retrieveServiceLocked(Intent service,
        //2345              String instanceName, String resolvedType, String callingPackage,
        //2346              int callingPid, int callingUid, int userId,
        //2347              boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal,
        //2348              boolean allowInstant) {

        // Android 12
        // retrieveServiceLocked(Intent service,
        //3103             String instanceName, String resolvedType, String callingPackage,
        //3104             int callingPid, int callingUid, int userId,
        //3105             boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal,
        //3106             boolean allowInstant)
        XposedHelpers.findAndHookMethod("com.android.server.am.ActiveServices",
            classLoader,
            "retrieveServiceLocked",
            Intent::class.java,//Intent service,
            //String instanceName, String resolvedType, String callingPackage,
            String::class.java,
            String::class.java,
            String::class.java,
            //int callingPid, int callingUid, int userId,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            //boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal,
            Boolean::class.java,
            Boolean::class.java,
            Boolean::class.java,
            //boolean allowInstant
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (param?.args?.get(3) == null) return
                    val packageName = param.args[3] as String
                    val serviceLookupResult = param.result ?: return
                    param.result = setServiceRecord(packageName,serviceLookupResult)
                    // param?.result = false
                }
            })
    }


    fun hook33(classLoader: ClassLoader) {
        //   private ServiceLookupResult retrieveServiceLocked(Intent service,
        //3293              String instanceName, boolean isSdkSandboxService, int sdkSandboxClientAppUid,
        //3294              String sdkSandboxClientAppPackage, String resolvedType,
        //3295              String callingPackage, int callingPid, int callingUid, int userId,
        //3296              boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal,
        //3297              boolean allowInstant) {


        XposedHelpers.findAndHookMethod("com.android.server.am.ActiveServices",
            classLoader,
            "retrieveServiceLocked",
            Intent::class.java,//Intent service,
            //String instanceName, boolean isSdkSandboxService, int sdkSandboxClientAppUid,
            String::class.java,
            Boolean::class.java,
            Int::class.java,
            //String sdkSandboxClientAppPackage, String resolvedType,
            String::class.java,
            String::class.java,
            //String callingPackage, int callingPid, int callingUid, int userId,
            String::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            //boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal,
            Boolean::class.java,
            Boolean::class.java,
            Boolean::class.java,
            //boolean allowInstant
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (param?.args?.get(6) == null) return
                    val packageName = param.args[6] as String
                    val serviceLookupResult = param.result ?: return
                    param.result = setServiceRecord(packageName,serviceLookupResult)
                    // param?.result = false
                }
            })
    }

    private fun setServiceRecord(packageName: String,serviceLookupResult:Any):Any {
        if (packageName != BuildConfig.APPLICATION_ID) {
            return serviceLookupResult
        }
        val serviceRecord = XposedHelpers.getObjectField(serviceLookupResult, "record")

        XposedHelpers.setObjectField(serviceRecord, "mAllowStartForeground", 0)
        XposedHelpers.setObjectField(
            serviceRecord,
            "mAllowStartForegroundAtEntering",
            0
        )

        XposedHelpers.setObjectField(serviceLookupResult, "record", serviceRecord)

        return serviceLookupResult
    }
}