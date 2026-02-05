/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

import com.google.gson.Gson
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.http.api.TagAPI
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.models.QjTagGroupModel
import net.ankio.auto.xposed.hooks.qianji.models.QjTagModel
import net.ankio.dex.model.Clazz
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.TagModel
import org.ezbook.server.tools.MD5HashTable
import java.lang.reflect.Proxy

/**
 * 钱迹标签刷新 Presenter 的 Xposed 反射包装。
 *
 * 设计原则：
 * - 不复制宿主业务逻辑，只通过反射调用宿主实现；
 * - 用动态代理接收回调，必要时以协程方式等待首次结果；
 * - 避免破坏宿主线程模型，关键调用放在主线程执行。
 */
object TagRefreshPresenterImpl : HookerClazz() {

    /** 宿主类名：com.mutangtech.qianji.tag.TagRefreshPresenterImpl */
    private const val CLAZZ = "com.mutangtech.qianji.tag.TagRefreshPresenterImpl"

    /** 懒加载宿主 Presenter 类 */
    private val presenterClazz by lazy { clazz() }

    /** 规则：用于定位宿主类 */
    override var rule = Clazz(name = this::class.java.name, nameRule = CLAZZ)

    /**
     * 标签刷新结果的轻量封装。
     * @param list 标签分组列表（原始宿主对象）
     * @param fromRemote 是否来自远程刷新
     * @param success 刷新是否成功
     */
    data class TagRefreshResult(
        val list: List<*>,
        val fromRemote: Boolean,
        val success: Boolean
    )

    /**
     * 标签刷新回调集合（用于动态代理转发）。
     * @param onGetList 回调宿主 onGetList(list, fromRemote, success) 的结果
     */
    data class TagRefreshCallbacks(
        val onGetList: (TagRefreshResult) -> Unit = {}
    )

    /**
     * 创建宿主 View 接口的动态代理。
     * @param callbacks 回调集合
     * @return 代理实例，可直接传入宿主 Presenter 构造函数
     */
    private fun newViewProxy(callbacks: TagRefreshCallbacks): Any {
        // 取构造函数第一个参数作为 View 接口类型
        val constructor = presenterClazz.constructors.firstOrNull()
            ?: throw NoSuchMethodException("TagRefreshPresenterImpl constructor not found")
        val viewClazz = constructor.parameterTypes.firstOrNull()
            ?: throw NoSuchMethodException("TagRefreshPresenterImpl view type not found")

        // 创建动态代理并处理 onGetList 回调
        return Proxy.newProxyInstance(
            AppRuntime.classLoader,
            arrayOf(viewClazz)
        ) { _, method, args ->
            if (method.name == "onGetList") {
                val list = args?.getOrNull(0) as? List<*> ?: emptyList<Any>()
                val fromRemote = (args?.getOrNull(1) as? Boolean) ?: false
                val success = (args?.getOrNull(2) as? Boolean) ?: false
                callbacks.onGetList(TagRefreshResult(list, fromRemote, success))
            }
            null
        }
    }

    /**
     * 构造宿主 TagRefreshPresenterImpl 实例。
     * @param viewProxy 代理 View 对象
     * @param status 标签状态过滤（如：全部/默认/归档）
     * @param withGroup 宿主内部标记（原样透传）
     * @return 宿主 Presenter 实例
     */
    private fun newPresenter(viewProxy: Any, status: Int, withGroup: Boolean): Any {
        // 优先使用三参构造函数：TagRefreshPresenterImpl(view, status, withGroup)
        val directCtor = presenterClazz.constructors.firstOrNull { it.parameterTypes.size == 3 }
        if (directCtor != null) {
            return XposedHelpers.newInstance(presenterClazz, viewProxy, status, withGroup)
        }

        // 兼容 Kotlin 默认参数的合成构造：(..., int mask, DefaultConstructorMarker)
        val syntheticCtor = presenterClazz.constructors.firstOrNull { it.parameterTypes.size >= 5 }
            ?: throw NoSuchMethodException("TagRefreshPresenterImpl constructor not found")

        // 组装参数：固定前三项 + mask + marker，其他位置保持 null
        val params = arrayOfNulls<Any>(syntheticCtor.parameterTypes.size)
        params[0] = viewProxy
        params[1] = status
        params[2] = withGroup
        params[syntheticCtor.parameterTypes.size - 2] = 0 // mask：显式传参，不使用默认值
        params[syntheticCtor.parameterTypes.size - 1] = null // DefaultConstructorMarker

        return XposedHelpers.newInstance(presenterClazz, *params)
    }

    /**
     * 从宿主 TagGroup 对象提取 Tag 列表。
     * - 仅读取字段 tagList（TagGroup#tagList）；
     * - 字段不存在或异常时返回空列表，避免破坏宿主流程。
     * @param group 宿主 TagGroup 原始对象
     * @return 宿主 Tag 原始对象列表
     */
    private fun extractTagObjects(group: Any): List<Any> {
        // TagGroup: public List tagList;
        return runCatching {
            val fieldValue = XposedHelpers.getObjectField(group, "tagList") as? List<*>
            fieldValue?.filterNotNull() ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * 将 TagGroup 列表扁平化为 QjTagModel 列表。
     * @param groups 宿主 TagGroup 列表
     * @return 包装后的 QjTagModel 列表
     */
    private fun flattenTags(groups: List<*>): List<QjTagModel> {
        val result = arrayListOf<QjTagModel>()
        groups.forEach {
            if (it == null) return@forEach
            val group = QjTagGroupModel.fromObject(it)
            val tags = group.getTagList()
            tags.forEach { tag ->
                result.add(QjTagModel.fromObject(tag))
            }
        }
        return result
    }


    /**
     * 将 TagGroup 列表转换为 TagModel 列表（保留分组信息）。
     * @param groups 宿主 TagGroup 列表
     * @return 标签模型列表（name + group）
     */
    private fun buildTagModels(groups: List<*>): List<TagModel> {
        val result = arrayListOf<TagModel>()
        groups.forEach {
            if (it == null) return@forEach

            val group = QjTagGroupModel.fromObject(it)

            val groupName = group.getName() ?: ""
            val tags = group.getTagList()
            tags.forEach { tag ->
                val model = TagModel()
                model.name = tag.getName() ?: ""
                model.group = groupName
                result.add(model)
            }
        }
        // 以 name + group 去重，避免同名重复写入
        return result.distinctBy { "${it.name}#${it.group}" }
    }

    /**
     * 获取标签列表（包装为 QjTagModel）。
     * - 流程参考 BookManagerImpl：直接读取宿主数据，不修改宿主；
     * - 内部使用增量加载，确保本地可用且必要时触发宿主刷新。
     * @param status 标签状态过滤（如：默认/归档/全部）
     * @param withGroup 宿主内部标记（原样透传）
     * @param force 是否强制远程刷新（透传宿主）
     * @return 扁平化后的标签列表
     */
    suspend fun getTags(
        status: Int = QjTagModel.STATUS_DEFAULT,
        withGroup: Boolean = true,
        force: Boolean = false
    ): List<QjTagModel> {
        val result = loadByIncremental(status, withGroup, force)
        return flattenTags(result.list)
    }

    /**
     * 根据标签名称列表获取标签 ID 列表。
     * - 仅返回已匹配到的标签 ID；
     * - 保持输入顺序，忽略空名称与重复名称；
     * - 未命中则跳过，不返回空占位。
     * @param tagNames 标签名称列表
     * @return 标签 ID 列表（字符串）
     */
    suspend fun getTagIdsByNames(
        tagNames: List<String>
    ): List<String> {
        if (tagNames.isEmpty()) {
            return emptyList()
        }
        val tags = getTags()
        val tagMap = tags.associateBy { it.getName() ?: "" }
        val ids = arrayListOf<String>()
        val seen = hashSetOf<String>()
        tagNames.forEach { name ->
            val trimmedName = name.trim()
            if (trimmedName.isEmpty() || !seen.add(trimmedName)) {
                return@forEach
            }
            val tagId = tagMap[trimmedName]?.getTagId()
            if (!tagId.isNullOrBlank()) {
                ids.add(tagId)
            }
        }
        return ids
    }

    /**
     * 增量加载标签（宿主内部会先读本地，再视情况触发远程刷新）。
     * @param status 标签状态过滤（透传宿主）
     * @param withGroup 宿主内部标记（透传宿主）
     * @param force 是否强制远程刷新（透传宿主）
     * @param callbacks 回调集合（可选）
     * @return 首次回调结果（后续回调仍会触发 callbacks）
     */
    suspend fun loadByIncremental(
        status: Int,
        withGroup: Boolean,
        force: Boolean,
        callbacks: TagRefreshCallbacks = TagRefreshCallbacks()
    ): TagRefreshResult = withContext(Dispatchers.Main) {
        val firstResult = CompletableDeferred<TagRefreshResult>()
        val viewProxy = newViewProxy(
            TagRefreshCallbacks { result ->
                callbacks.onGetList(result)
                if (!firstResult.isCompleted) {
                    firstResult.complete(result)
                }
            }
        )
        val presenter = newPresenter(viewProxy, status, withGroup)
        XposedHelpers.callMethod(presenter, "loadByIncremental", force)
        firstResult.await()
    }

    /**
     * 同步标签信息到自动记账服务端。
     * - 从宿主拉取 TagGroup 列表并转换为 TagModel（name + group）；
     * - 使用 batchInsert 进行重置式写入，确保服务端与宿主一致。
     */
    suspend fun syncTag() = withContext(Dispatchers.IO) {
        val result = withContext(Dispatchers.Main) {
            loadByIncremental(
                status = QjTagModel.STATUS_ALL,
                withGroup = true,
                force = false
            )
        }
        val tagModels = buildTagModels(result.list)
        if (tagModels.isEmpty()) {
            AppRuntime.manifest.i("No tags to sync, skip.")
            return@withContext
        }
        // 幂等：内容哈希一致则跳过同步
        val sync = Gson().toJson(tagModels)
        val md5 = MD5HashTable.md5(sync)
        val server = SettingAPI.get(Setting.HASH_TAG, "")
        if (server == md5) {
            AppRuntime.manifest.i("Skip tag sync, MD5 matched (server=$server, local=$md5)")
            return@withContext
        }
        // 同步时携带 md5 供服务端写入设置项
        TagAPI.batchInsert(tagModels, md5)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步标签信息到自动记账")
        }
    }
}