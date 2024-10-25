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

package net.ankio.auto.xposed.hooks.alipay.hooks

import android.app.Application
import android.content.Context
import de.robv.android.xposed.XposedBridge
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker


/**
 * 支付宝安全检测bypass
 */
class SecurityHooker:PartHooker() {
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        val clazz = classLoader.loadClass("com.alipay.apmobilesecuritysdk.scanattack.common.ScanAttack")

        // 这个bypass只能过掉在xposed、root环境隐藏不好的情况，即弹出登录异常、环境不安全这些提示
        Hooker.allMethodsAfter(clazz){
            val result = it.result
            // 所有检测的全部返回false
            if (result is Boolean){
                it.result = false
            }
        }
        // TODO 支付宝在Native层应该还有检测，人脸无法过掉，需要进一步研究
        // TODO 如果要用人脸的话，建议用vmos虚拟机开一个干净的环境用支付宝（反正也不是天天用人脸）
        // TODO 推测在这里应该有检测:com.alipay.apmobilesecuritysdk.scanattack.bridge.ScanAttackNativeBridge.getAD104(Context p0,int p1,int p2,int p3,int p4,int p5,String p6);

        // Context p0,
        // int p1,
        // int p2,
        // boolean p3,
        // int p4,
        // int p5,
        // String p6

        //  public static int CHECK_ALL = 255;
        //    public static int CHECK_DEBUG = 2;
        //    public static int CHECK_HOOK = 1;
        //    public static int CHECK_VIRTUAL = 4;
        //    public static int MODE_DETAIL = 1;
        //    public static int MODE_SIMPLE;

        Hooker.before(clazz,"getAD104",
            Context::class.java,
            Int::class.java, // 检测模式
            Int::class.java, // 检测类型
            Boolean::class.java,
            Int::class.java,
            Int::class.java,
            String::class.java
        ) {
            it.args[2] = 4 //只检测虚拟机
            // 好像没啥用
        }
    }


}
