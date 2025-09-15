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

import com.google.gson.Gson
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.tools.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.models.QjBookModel
import org.ezbook.server.constant.Setting
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.http.api.SettingAPI
import org.ezbook.server.db.model.BookNameModel
import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzMethod
import net.ankio.dex.model.ClazzField

/**
 * 钱迹账本管理适配器（Hook 端）。
 *
 * 职责：
 * - 通过反射定位宿主的 `BookManager`，读取账本列表；
 * - 将宿主账本对象转换为服务端可识别的 `BookNameModel`；
 * - 基于内容 MD5 做幂等同步，避免不必要的网络与写入。
 *
 * 约束与原则：
 * - Never break userspace：不修改宿主状态，仅读取；
 * - 简洁优先：仅保留同步所需字段（name/icon/remoteId）；
 * - 线程模型：反射与 IO 在 Dispatchers.IO 中执行；UI 提示在主线程。
 */
object BookManagerImpl : HookerClazz() {
    /**
     * 反射匹配规则：用于在宿主中定位 `BookManager` 类及关键方法。
     * 保持与 QianjiHooker.kt 中定义一致，以避免类名混淆导致的匹配失败。
     */
    override var rule = Clazz(
        name = this::class.java.name,
        nameRule = "^\\w{0,2}\\..+",
        type = "class",
        methods = listOf(
            ClazzMethod(
                name = "isFakeDefaultBook",
                returnType = "boolean",
                parameters = listOf(
                    ClazzField(type = QjBookModel.CLAZZ),
                ),
            ),
            ClazzMethod(
                name = "getAllBooks",
                returnType = "java.util.List",
            ),
        ),
    )

    /**
     * 宿主 `BookManager` 单例实例，通过 `getInstance()` 反射获取。
     */
    private val bookManagerInstance by lazy {
        XposedHelpers.callStaticMethod(
            clazz(),
            "getInstance",
        )
    }

    /**
     * 读取宿主所有账本原始对象列表。
     * - 在 IO 线程执行反射调用，避免阻塞主线程；
     * - 参数序列遵循宿主签名（application, includeHidden=true, scene=1）。
     * @return 宿主返回的账本对象列表（原始对象，未封装）。
     */
    suspend fun getBooks(): List<*> = withContext(Dispatchers.IO) {
        XposedHelpers.callMethod(
            bookManagerInstance,
            "getAllBooks",
            AppRuntime.application!!,
            true,
            1
        ) as List<*>
    }


    /**
     * 按名称查找账本，未命中则回退到首个账本。
     * - 名称重复时返回首个匹配；
     * @param name 账本名称
     * @return 转换后的 `BookModel`。
     */
    suspend fun getBookByName(name: String): QjBookModel {
        val list = getBooks()
        list.find { XposedHelpers.getObjectField(it, "name") == name }?.let {
            return QjBookModel.fromObject(it)
        }
        return QjBookModel.fromObject(list.first()!!)
    }

    /**
     * 同步账本名称清单到自动记账服务端。
     * 流程：
     * 1) 读取宿主账本并提取必要字段 -> `BookNameModel` 列表；
     * 2) 对 JSON 序列化结果计算 MD5，与服务端缓存值对比；
     * 3) 若一致且非调试模式则跳过，否则上传并在主线程提示完成。
     * @return 已构造并（可能）上传的账本名称列表。
     */
    suspend fun syncBooks(): ArrayList<BookNameModel> =
        withContext(Dispatchers.IO) {
            val list = getBooks()
            val bookList = arrayListOf<BookNameModel>()
            list.forEach {
                if (it == null) return@forEach
                val book = QjBookModel.fromObject(it)
                bookList.add(BookNameModel().apply {
                    name = book.getName()
                    icon = book.getCover() ?: ""
                    remoteId = book.getBookId().toString()
                })
            }

            // 幂等：内容哈希一致且非调试模式则跳过同步
            val sync = Gson().toJson(bookList)
            val md5 = MD5HashTable.md5(sync)
            val server = SettingAPI.get(Setting.HASH_BOOK, "")
            if (server == md5 && !AppRuntime.debug) {
                AppRuntime.manifest.i("跳过账本同步，MD5 一致（server=${server}, local=${md5}）")
                return@withContext bookList
            }
            AppRuntime.manifest.i("同步账本数据: $sync")
            BookNameAPI.put(bookList, md5)
            withContext(Dispatchers.Main) {
                MessageUtils.toast("已同步账本信息到自动记账")
            }
            bookList
        }
}