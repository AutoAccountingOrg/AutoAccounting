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

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.hooks.qianji.models.QjAssetAccountModel
import net.ankio.auto.xposed.hooks.qianji.models.QjBookModel
import net.ankio.dex.model.Clazz
import org.ezbook.server.db.model.BookNameModel
import org.json.JSONObject

/**
 * 钱迹资产提交 Presenter 包装（BaseSubmitAssetPresenterImpl 的轻量代理）
 *
 * 设计原则：
 * - 不复制业务逻辑，不伪造实现；仅通过反射调用原始方法，确保向后兼容。
 * - 提供必要的实例/静态入口，供上层以 Any 透传原始对象与参数。
 */
class BaseSubmitAssetPresenterImpl(private val obj: Any) {

    companion object : HookerClazz() {
        /** 原始类全名 */
        private const val CLAZZ =
            "com.mutangtech.qianji.asset.submit.mvp.BaseSubmitAssetPresenterImpl"

        override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

        /** 使用已有实例创建包装 */
        fun fromObject(origin: Any): BaseSubmitAssetPresenterImpl =
            BaseSubmitAssetPresenterImpl(origin)

        /** 通过无参构造创建新实例 */
        fun newInstance(): BaseSubmitAssetPresenterImpl =
            fromObject(XposedHelpers.newInstance(clazz()))
    }

    /** 暴露原始对象 */
    fun toObject(): Any = obj

    /**
     * 提交资产
     * @param book 原始类型：com.mutangtech.qianji.data.model.Book
     * @param assetAccount 原始类型：com.mutangtech.qianji.data.model.AssetAccount
     * @param jsonObject 原始类型：org.json.JSONObject
     * @param diffParams 原始类型：com.mutangtech.qianji.asset.diff.DiffParams
     */
    fun submitAsset(
        book: QjBookModel,
        assetAccount: QjAssetAccountModel,
        jsonObject: JSONObject,
        diffParams: Any?
    ) {
        XposedHelpers.callMethod(
            obj,
            "submitAsset",
            book.toObject(),
            assetAccount.toObject(),
            jsonObject,
            diffParams
        )
    }

    /**
     * 绑定视图（重载之一：l 类型）
     * @param view 原始类型：com.mutangtech.qianji.asset.submit.mvp.l 或 i8.c
     */
    fun setView(view: Callbacks) {
        XposedHelpers.callMethod(obj, "setView", ViewInterface.newProxy(view))
    }
}
