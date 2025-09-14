/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.hooks.qianji.impl

import android.content.Context
import android.content.Intent
import android.view.View
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzMethod
import java.lang.reflect.Proxy

/**
 * 钱迹资产提交模块 View 接口包装（对应 com.mutangtech.qianji.asset.submit.mvp.l）
 *
 * 设计原则：
 * - 仅通过反射调用原接口方法，保持与上游实现一致，避免捏造逻辑。
 * - 提供最小必要方法集合，参数类型使用原生对象或 Any 透传。
 */
class ViewInterface(private val obj: Any) {

    companion object : HookerClazz() {
        /** 目标接口：通过特征匹配规则定位（不硬编码包名） */
        override var rule = Clazz(
            name = this::class.java.name,
            nameRule = "com.mutangtech.qianji.asset.submit.mvp\\..+",
            type = "interface",
            methods = listOf(
                ClazzMethod(name = "getContext", returnType = "android.content.Context"),
                ClazzMethod(name = "init", returnType = "void"),
                ClazzMethod(name = "onActivityResult", returnType = "void"),
                ClazzMethod(name = "onAdd", returnType = "void"),
                ClazzMethod(name = "onChangeBaseCurrency", returnType = "void"),
                ClazzMethod(name = "onEdit", returnType = "void"),
                ClazzMethod(name = "onSubmitFinished", returnType = "void"),
                ClazzMethod(name = "setPresenter", returnType = "void"),
            ),
        )

        /** 使用已有实现对象创建包装 */
        fun fromObject(origin: Any): ViewInterface = ViewInterface(origin)

        /**
         * 创建该接口的动态代理实例，并由回调处理方法。
         * 返回对象即实现了钱迹的目标接口，可直接传入对方代码调用。
         */
        fun newProxy(callbacks: Callbacks = Callbacks()): Any {
            val iface = clazz()
            return Proxy.newProxyInstance(
                AppRuntime.classLoader,
                arrayOf(iface)
            ) { _, method, args ->
                when (method.name) {
                    "onSubmitFinished" -> {
                        val success = (args?.getOrNull(0) as? Boolean) ?: false
                        val asset = args?.getOrNull(1)
                        callbacks.onSubmitFinished(success, asset)
                        null
                    }

                    else -> null
                }
            }
        }
    }

    /** 暴露原始对象 */
    fun toObject(): Any = obj

    /** 获取上下文（Deprecated，透传原实现） */
    fun getContext(): Context? = XposedHelpers.callMethod(obj, "getContext") as? Context

    /** 初始化视图（Deprecated，透传原实现） */
    fun init(view: View) {
        XposedHelpers.callMethod(obj, "init", view)
    }

    /** Activity 回调透传 */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        XposedHelpers.callMethod(obj, "onActivityResult", requestCode, resultCode, data)
    }

    /** 展示资产类型 */
    fun onAdd(assetType: Any) {
        XposedHelpers.callMethod(obj, "onAdd", assetType)
    }

    /** 基础币种变更通知 */
    fun onChangeBaseCurrency() {
        XposedHelpers.callMethod(obj, "onChangeBaseCurrency")
    }

    /** 编辑资产账户 */
    fun onEdit(assetAccount: Any) {
        XposedHelpers.callMethod(obj, "onEdit", assetAccount)
    }

    /** 提交完成回调 */
    fun onSubmitFinished(success: Boolean, assetAccount: Any?) {
        XposedHelpers.callMethod(obj, "onSubmitFinished", success, assetAccount)
    }

    /** 绑定 Presenter（Deprecated，透传原实现） */
    fun setPresenter(presenter: Any) {
        XposedHelpers.callMethod(obj, "setPresenter", presenter)
    }
}

/**
 * View 接口的回调集合，用于动态代理。
 * 提供默认空实现，按需覆写所需回调。
 */
data class Callbacks(
    val onSubmitFinished: (Boolean, Any?) -> Unit = { _, _ -> },
)
