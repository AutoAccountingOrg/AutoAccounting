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
import net.ankio.auto.xposed.core.utils.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.impl.BxPresenterImpl.convert2Bill
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookBillModel
import org.ezbook.server.db.model.SettingModel
import java.lang.reflect.Proxy
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

            val str = ""
            //BookFilter
            val bookFilter = XposedHelpers.newInstance(refreshMethod.parameters[1].type)


            //dateFilter


            val dateFilter = XposedHelpers.newInstance(refreshMethod.parameters[2].type)

            // 获取当前时间
            val fromCalendar = Calendar.getInstance()
            // 获取结束时间(当前时间)
            val toCalendar = Calendar.getInstance()
            // 设置开始时间为10天前
            fromCalendar.add(Calendar.DAY_OF_MONTH, -10)
            // 调用原方法设置时间范围过滤
            XposedHelpers.callMethod(
                dateFilter,
                "setTimeRangeFilter",
                fromCalendar,
                toCalendar,
                103
            )


            //TypesFilter
            val typesFilter = XposedHelpers.newInstance(refreshMethod.parameters[3].type)
            //MoneyFilter
            val moneyFilter = XposedHelpers.newInstance(refreshMethod.parameters[4].type)
            //ImageFilter
            val imageFilter = XposedHelpers.newInstance(refreshMethod.parameters[5].type)
            //PlatformFilter
            val platformFilter = XposedHelpers.newInstance(refreshMethod.parameters[6].type)
            //SortFilter
            val sortFilter = XposedHelpers.newInstance(refreshMethod.parameters[10].type, 0, false)
            //BillFlagFilter
            val billFlagFilter = XposedHelpers.newInstance(refreshMethod.parameters[11].type, 0)
            //AssetsFilter
            val assetsFilter = XposedHelpers.newInstance(refreshMethod.parameters[12].type)
            //TagsFilter
            val tagsFilter = XposedHelpers.newInstance(refreshMethod.parameters[13].type, null)


            //触发搜索
            XposedHelpers.callMethod(
                XposedHelpers.newInstance(searchImpl, param1Object),
                "searchLocal",
                str,
                bookFilter,
                dateFilter,
                typesFilter,
                moneyFilter,
                imageFilter,
                platformFilter,
                false,
                false,
                true,
                sortFilter,
                billFlagFilter,
                assetsFilter,
                tagsFilter
            )

        }

    suspend fun syncBills() = withContext(Dispatchers.IO) {
        // 报销账单
        val bxList =
            withContext(Dispatchers.Main) {
                getLast10DayLists()
            }

        val bills = convert2Bill(bxList, Setting.HASH_BILL)
        val sync = Gson().toJson(bills)
        val md5 = MD5HashTable.md5(sync)
        val server = SettingModel.get(Setting.HASH_BILL, "")
        if (server == md5 && !AppRuntime.debug) {
            AppRuntime.log("No need to sync bill Data, server md5:${server} local md5:${md5}")
            return@withContext
        }
        AppRuntime.logD("Sync bills:$sync")
        BookBillModel.put(bills, md5, Setting.HASH_BILL)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步报销账单到自动记账")
        }
    }
}