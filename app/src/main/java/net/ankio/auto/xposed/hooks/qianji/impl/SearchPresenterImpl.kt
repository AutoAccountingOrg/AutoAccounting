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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.tools.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.impl.BxPresenterImpl.convert2Bill
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookBillModel
import org.ezbook.server.db.model.SettingModel
import java.lang.reflect.Proxy
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import net.ankio.auto.http.api.BookBillAPI
import net.ankio.auto.http.api.SettingAPI

object SearchPresenterImpl {
    val CLAZZ = "com.mutangtech.qianji.bill.search.SearchPresenterImpl"
    val searchImpl = Hooker.loader(CLAZZ)
    suspend fun getLast10DayLists(): List<*> =
        suspendCoroutine { continuation ->
            var resumed = false
            val constructor = searchImpl.constructors.first()!!
            //   public SearchPresenterImpl(p pVar) {
            //        super(pVar);
            //    }

            val param1Clazz = constructor.parameterTypes.first()!!
            // /* loaded from: classes.dex */
            //public interface p extends d {
            //    void onGetListFromLocal(List<? extends Bill> list);
            //}
            val param1Object = Proxy.newProxyInstance(
                AppRuntime.classLoader,
                arrayOf(param1Clazz)
            ) { proxy, method, args ->
                if (method.name == "onGetListFromLocal") {
                    if (!resumed) {
                        resumed = true
                        val billList = args[0]
                        continuation.resume(billList as List<*>)
                    }
                }
            }
            // public void searchLocal(
            // String str, 0
            // BookFilter bookFilter, 1
            // DateFilter dateFilter, 2
            // TypesFilter typesFilter, 3
            // MoneyFilter moneyFilter, 4
            // ImageFilter imageFilter, 5
            // PlatformFilter platformFilter, 6
            // boolean z10, 7
            // boolean z11, 8
            // boolean z12, 9
            // SortFilter sortFilter, 10
            // BillFlagFilter billFlagFilter, 11
            // AssetsFilter assetsFilter, 12
            // TagsFilter tagsFilter 13
            // ) {
            val refreshMethod = searchImpl.declaredMethods.find { it.name == "searchLocal" }!!

            // 确保至少有12个参数
            if (refreshMethod.parameterTypes.size < 12) {
                throw IllegalStateException("searchLocal method must have at least 12 parameters")
            }

            // 基础参数准备
            val params = mutableListOf<Any?>().apply {
                // 0. 搜索字符串
                add("")

                // 1. BookFilter
                val bookFilter = XposedHelpers.newInstance(refreshMethod.parameterTypes[1])
                runBlocking {
                    BookManagerImpl.getBooks().forEach {
                        XposedHelpers.callMethod(bookFilter, "add", it)
                    }
                }
                add(bookFilter)

                // 2. DateFilter
                val dateFilter = XposedHelpers.newInstance(refreshMethod.parameterTypes[2])
                val fromCalendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -10)
                }
                val toCalendar = Calendar.getInstance()
                XposedHelpers.callMethod(
                    dateFilter,
                    "setTimeRangeFilter",
                    fromCalendar,
                    toCalendar,
                    103
                )
                add(dateFilter)

                // 4-7. 各种Filter
                // TypesFilter typesFilter, 3
                // MoneyFilter moneyFilter, 4
                // ImageFilter imageFilter, 5
                // PlatformFilter platformFilter, 6
                // 3 typesFilter
                val typesFilter = XposedHelpers.newInstance(refreshMethod.parameterTypes[3])
                XposedHelpers.callMethod(typesFilter, "add", 0)
                add(typesFilter)
                // 4 moneyFilter
                add(null)
                // 5 imageFilter
                add(null)
                // 6 platformFilter
                add(null)
                // 8-9. 布尔值参数
                add(false) // z10
                add(false) // z11
                add(true)  // z12

                // 10. SortFilter
                add(XposedHelpers.newInstance(refreshMethod.parameterTypes[10], 0, false))

                // 11. BillFlagFilter
                add(null)

                // 12. AssetsFilter
                if (refreshMethod.parameterTypes.size > 12) {
                    add(null)
                }

                // 13. TagsFilter (如果存在)
                if (refreshMethod.parameterTypes.size > 13) {
                    add(null)
                }
            }

            // 触发搜索
            XposedHelpers.callMethod(
                XposedHelpers.newInstance(searchImpl, param1Object),
                "searchLocal",
                *params.toTypedArray()
            )

        }

    suspend fun syncBills() = withContext(Dispatchers.IO) {
        // 报销账单
        val bxList =
            withContext(Dispatchers.Main) {
                runCatching {
                    getLast10DayLists()
                }.onFailure {
                    AppRuntime.logE(it)
                }.getOrDefault(emptyList<Any>())
            }

        val bills = convert2Bill(bxList, Setting.HASH_BILL)
        val sync = Gson().toJson(bills)
        val md5 = MD5HashTable.md5(sync)
        val server = SettingAPI.get(Setting.HASH_BILL, "")
        if (server == md5 && !AppRuntime.debug) {
            AppRuntime.log("No need to sync bill Data, server md5:${server} local md5:${md5}")
            return@withContext
        }
        AppRuntime.logD("Sync bills:$sync")
        BookBillAPI.put(bills, md5, Setting.HASH_BILL)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步支出账单到自动记账")
        }
    }
}