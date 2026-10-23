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

/**
 * 搜索Presenter实现类，用于钩子钱迹应用的搜索功能
 * 该类负责获取最近10天的账单列表并同步到自动记账
 */
import com.google.gson.Gson
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.tools.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.impl.BxPresenterImpl.convert2Bill
import net.ankio.auto.xposed.hooks.qianji.filter.BillFlagFilter
import net.ankio.auto.xposed.hooks.qianji.filter.BookFilter
import net.ankio.auto.xposed.hooks.qianji.filter.DataFilter
import net.ankio.auto.xposed.hooks.qianji.filter.SortFilter
import net.ankio.auto.xposed.hooks.qianji.filter.TypesFilter
import org.ezbook.server.constant.Setting
import java.lang.reflect.Proxy
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import net.ankio.auto.http.api.BookBillAPI
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.xposed.core.logger.XposedLogger

object SearchPresenterImpl : HookerClazz() {
    private const val CLAZZ = "com.mutangtech.qianji.bill.search.SearchPresenterImpl"
    private val searchImpl by lazy { clazz() }
    override var rule = net.ankio.dex.model.Clazz(name = this::class.java.name, nameRule = CLAZZ)

    /**
     * 获取最近10天的账单列表
     * 通过钩子钱迹应用的SearchPresenterImpl类来搜索本地账单
     * @return 返回账单列表
     */
    suspend fun getLast10DayLists(bookName: String): List<*> =
        suspendCoroutine { continuation ->
            var resumed = false
            val constructor = searchImpl.constructors.first()
            //   public SearchPresenterImpl(p pVar) {
            //        super(pVar);
            //    }

            val param1Clazz = constructor.parameterTypes.first()
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
            // String, BookFilter, DateFilter, TypesFilter,
            // MoneyFilter, ImageFilter, PlatformFilter,
            // boolean, boolean, boolean,
            // SortFilter, BillFlagFilter,
            // AssetsFilter?, TagsFilter?, CategoriesFilter?,
            // boolean includeNotCount?, boolean includeNotBudget?  // 钱迹 4.5.4+
            // ) {
            val refreshMethod = searchImpl.declaredMethods.find { it.name == "searchLocal" }!!

            // BookFilter：指定账本名时仅匹配该账本；否则为全部账本
            val bookFilter = BookFilter.newInstance().also { wrapper ->
                val target = runBlocking { BookManagerImpl.getBookByName(bookName) }
                wrapper.set(target)
            }

            // DateFilter：最近 10 天
            val fromCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -10) }
            val toCalendar = Calendar.getInstance()
            val dateFilter = DataFilter.newInstance()
                .setTimeRangeFilter(fromCalendar, toCalendar)

            // TypesFilter：类型 0（支出）
            val typesFilter = TypesFilter.newInstance().apply { add(0) }

            // SortFilter：时间降序
            val sortFilter = SortFilter.newTimeDesc(false)

            // 按宿主 searchLocal 实际参数个数组装（钱迹 4.5.4 增至 17 参）
            val params = SearchLocalArgs.build(
                paramCount = refreshMethod.parameterTypes.size,
                bookFilter = bookFilter.toObject(),
                dateFilter = dateFilter.toObject(),
                typesFilter = typesFilter.toObject(),
                sortFilter = sortFilter.toObject(),
                billFlagFilter = BillFlagFilter.withFlag(0).toObject(),
            )

            // 触发搜索
            XposedHelpers.callMethod(
                XposedHelpers.newInstance(searchImpl, param1Object),
                "searchLocal",
                *params
            )

        }

    /**
     * 同步账单到自动记账服务器
     * 获取最近10天的账单列表，转换为标准格式，并上传到服务器
     * 会检查MD5哈希值以避免重复同步
     */
    suspend fun syncBills(bookName: String) = withContext(Dispatchers.IO) {
        // 报销账单
        val bxList =
            withContext(Dispatchers.Main) {
                runCatching {
                    getLast10DayLists(bookName)
                }.onFailure {
                    XposedLogger.e(" get bills failed", it)
                }.getOrDefault(emptyList<Any>())
            }

        val bills = convert2Bill(bxList, Setting.HASH_BILL)
        val sync = Gson().toJson(bills)
        val md5 = MD5HashTable.md5(sync)
        XposedLogger.d(" sync bills, count=${bills.size}")
        BookBillAPI.put(bills, md5, Setting.HASH_BILL)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步支出账单到自动记账")
        }
    }
}