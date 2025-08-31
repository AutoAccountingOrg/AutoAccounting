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
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.tools.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.models.Category
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.db.model.SettingModel
import java.lang.reflect.Proxy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import net.ankio.auto.http.api.CategoryAPI
import net.ankio.auto.http.api.SettingAPI

object CateInitPresenterImpl {
    private var cateInitPresenterImplClazz: Class<*> =
        Hooker.loader(
            "com.mutangtech.qianji.bill.add.category.CateInitPresenterImpl",
        )

    private val proxyOnGetCategoryListClazz by lazy {
        AppRuntime.clazz("onGetCategoryList")
    }


    /**
     * 获取分类列表
     */
    private suspend fun getCategoryList(bookId: Long): HashMap<String, Any> =
        suspendCoroutine { continuation ->
            var resumed = false
            val proxyInstance =
                Proxy.newProxyInstance(
                    AppRuntime.classLoader,
                    arrayOf(proxyOnGetCategoryListClazz)
                ) { _, method, args ->
                    if (method.name == "onGetCategoryList") {
                        val list1 = args[0]
                        val list2 = args[1]

                        if (!resumed) {
                            resumed = true
                            runCatching {
                                continuation.resume(hashMapOf("list1" to list1, "list2" to list2))
                            }
                        }
                    }
                    null
                }
            val obj = XposedHelpers.newInstance(cateInitPresenterImplClazz, proxyInstance)
            XposedHelpers.callMethod(obj, "loadCategoryList", bookId, false)
        }

    suspend fun syncCategory(books: ArrayList<BookNameModel>) = withContext(Dispatchers.IO) {
        val arrayList = arrayListOf<CategoryModel>()
        for (book in books) {
            val hashMap =
                withContext(Dispatchers.Main) {
                    getCategoryList(book.remoteId.toLong())
                }

            convertCategoryToModel(
                hashMap["list1"] as List<*>,
                BillType.Expend, // 支出
            ).let {
                arrayList.addAll(it)
            }
            convertCategoryToModel(
                hashMap["list2"] as List<*>,
                BillType.Income, // 收入
            ).let {
                arrayList.addAll(it)
            }
        }
        val sync = Gson().toJson(arrayList)
        val md5 = MD5HashTable.md5(sync)
        val server = SettingAPI.get(Setting.HASH_CATEGORY, "")
        if (server == md5 && !AppRuntime.debug) {
            AppRuntime.log("No need to sync categories, Server md5:${server} local md5:${md5}")
            return@withContext
        }
        AppRuntime.logD("Sync categories:$sync")
        CategoryAPI.put(arrayList, md5)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步分类信息到自动记账")
        }

    }

    /**
     * 将分类转为自动记账的分类模型
     */
    private fun convertCategoryToModel(
        list: List<*>,
        type: BillType,
    ): ArrayList<CategoryModel> {
        val categories = arrayListOf<CategoryModel>()
        list.forEach {
            if (it == null) return@forEach
            val category = Category.fromObject(it)
            val model = CategoryModel()
            model.type = type
            model.name = category.getName()
            model.icon = category.getIcon()
            model.remoteId = category.getId().toString()
            model.remoteParentId = category.getParentId().toString()
            model.remoteBookId = category.getBookId().toString()
            model.sort = category.getSort()
            val subList = category.getSubList()
            if (!subList.isNullOrEmpty()) {
                categories.addAll(convertCategoryToModel(subList, type))
            }

            categories.add(model)
        }
        return categories
    }
}