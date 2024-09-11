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

package net.ankio.auto.hooks.qianji.sync

import com.google.gson.Gson
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.db.model.SettingModel
import java.lang.reflect.Proxy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 将钱迹的资产数据同步给自动记账
 */
class CategoryUtils(
    private val manifest: HookerManifest,
    private val classLoader: ClassLoader,
    private val books: List<BookNameModel>
) {

    private var cateInitPresenterImplClazz: Class<*> =
        classLoader.loadClass(
            "com.mutangtech.qianji.bill.add.category.CateInitPresenterImpl",
        )

    private val proxyOnGetCategoryListClazz by lazy {
        manifest.clazz("onGetCategoryList", classLoader)
    }


    /**
     * 获取分类列表
     */
    private suspend fun getCategoryList(bookId: Long): HashMap<String, Any> =
        suspendCoroutine { continuation ->
            var resumed = false
            val proxyInstance =
                Proxy.newProxyInstance(
                    classLoader,
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

    suspend fun syncCategory() = withContext(Dispatchers.IO) {
        val arrayList = arrayListOf<CategoryModel>()
        for (book in books) {
            val hashMap =
                withContext(Dispatchers.Main) {
                    getCategoryList(book.remoteId.toLong())
                }

            convertCategoryToModel(
                hashMap["list1"] as List<Any>,
                BillType.Expend, // 支出
            ).let {
                arrayList.addAll(it)
            }
            convertCategoryToModel(
                hashMap["list2"] as List<Any>,
                BillType.Income, // 收入
            ).let {
                arrayList.addAll(it)
            }
        }
        val sync = Gson().toJson(arrayList)
        val md5 = App.md5(sync)
        val server = SettingModel.get(Setting.HASH_CATEGORY, "")
        if (server == md5) {
            manifest.log("分类信息未发生变化，无需同步, 服务端md5:${server} 本地md5:${md5}")
            return@withContext
        }
        manifest.log("同步分类信息:$sync")
        CategoryModel.put(arrayList, md5)
        withContext(Dispatchers.Main) {
            App.toast("已同步分类信息到自动记账")
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
            val category = it
            val model = CategoryModel()
            model.type = type
            val fields = category::class.java.declaredFields
            for (field in fields) {
                field.isAccessible = true
                val value = field.get(category) ?: continue
                /**
                 * [
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_gongzi.png",
                 *         "id": 20001,
                 *         "level": 1,
                 *         "name": "工资",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_shenghuofei.png",
                 *         "id": 20002,
                 *         "level": 1,
                 *         "name": "生活费",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_hongbao.png",
                 *         "id": 20003,
                 *         "level": 1,
                 *         "name": "收红包",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_waikuai.png",
                 *         "id": 20004,
                 *         "level": 1,
                 *         "name": "外快",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_gupiao.png",
                 *         "id": 20005,
                 *         "level": 1,
                 *         "name": "股票基金",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 0,
                 *         "icon": "http://res3.qianjiapp.com/cateic_other.png",
                 *         "id": 20006,
                 *         "level": 1,
                 *         "name": "其它",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     }
                 * ]
                 */
                try {
                    when (field.name) {
                        "name" -> model.name = value as String
                        "icon" -> model.icon = value as String
                        "id" -> model.remoteId = (value as Long).toString()
                        "parentId" -> model.remoteParentId = (value as Long).toString()
                        "bookId" -> model.remoteBookId = (value as Long).toString()
                        "sort" -> model.sort = value as Int
                        "subList" -> {
                            val subList = value as List<*>
                            categories.addAll(convertCategoryToModel(subList, type))
                        }
                    }
                } catch (e: Exception) {
                    manifest.log("分类转换异常:${e.message}")
                    manifest.logE(e)
                }
            }
            categories.add(model)
        }
        return categories
    }


}