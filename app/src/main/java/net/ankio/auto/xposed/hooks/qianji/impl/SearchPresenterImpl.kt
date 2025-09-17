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
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.api.HookerClazz
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.tools.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.impl.BxPresenterImpl.convert2Bill
import net.ankio.auto.xposed.hooks.qianji.filter.AssetsFilter
import net.ankio.auto.xposed.hooks.qianji.filter.BillFlagFilter
import net.ankio.auto.xposed.hooks.qianji.filter.BookFilter
import net.ankio.auto.xposed.hooks.qianji.filter.DataFilter
import net.ankio.auto.xposed.hooks.qianji.filter.ImageFilter
import net.ankio.auto.xposed.hooks.qianji.filter.MoneyFilter
import net.ankio.auto.xposed.hooks.qianji.filter.PlatformFilter
import net.ankio.auto.xposed.hooks.qianji.filter.SortFilter
import net.ankio.auto.xposed.hooks.qianji.filter.TagsFilter
import net.ankio.auto.xposed.hooks.qianji.filter.TypesFilter
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookBillModel
import org.ezbook.server.db.model.SettingModel
import java.lang.reflect.Proxy
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import net.ankio.auto.http.api.BookBillAPI
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.xposed.core.logger.Logger

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

            // 基础参数准备（使用包装的 Filter 类构造宿主对象）
            val params = mutableListOf<Any?>().apply {
                // 0. 搜索字符串
                add("")

                // 1. BookFilter
                //    优化：当传入 bookName 非空时，仅匹配该账本；未命中将抛出异常；否则为“全部账本”
                val bookFilter = BookFilter.newInstance().also { wrapper ->
                    val target = runBlocking { BookManagerImpl.getBookByName(bookName) }
                    wrapper.set(target)
                }
                add(bookFilter.toObject())

                // 2. DateFilter（最近10天）
                val fromCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -10) }
                val toCalendar = Calendar.getInstance()
                val dateFilter = DataFilter.newInstance()
                    .setTimeRangeFilter(fromCalendar, toCalendar)

                add(dateFilter.toObject())

                // 3. TypesFilter（示例：添加类型0）
                val typesFilter = TypesFilter.newInstance().apply { add(0) }
                add(typesFilter.toObject())

                // 4. MoneyFilter
                add(null)

                // 5. ImageFilter
                add(null)

                // 6. PlatformFilter
                add(null)

                // 7-9. 布尔值参数
                add(false) // z10
                add(false) // z11
                add(true)  // z12

                // 10. SortFilter（时间降序）
                val sortFilter = SortFilter.newTimeDesc(false)
                add(sortFilter.toObject())

                // 11. BillFlagFilter
                add(BillFlagFilter.withFlag(0).toObject())

                // 12. AssetsFilter（可选）
                if (refreshMethod.parameterTypes.size > 12) add(null)

                // 13. TagsFilter（可选）
                if (refreshMethod.parameterTypes.size > 13) add(null)
            }

            // 触发搜索
            XposedHelpers.callMethod(
                XposedHelpers.newInstance(searchImpl, param1Object),
                "searchLocal",
                *params.toTypedArray()
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
                    AppRuntime.manifest.e(it)
                }.getOrDefault(emptyList<Any>())
            }

        val bills = convert2Bill(bxList, Setting.HASH_BILL)
        val sync = Gson().toJson(bills)
        val md5 = MD5HashTable.md5(sync)
        val server = SettingAPI.get(Setting.HASH_BILL, "")
        if (server == md5 && !AppRuntime.debug) {
            AppRuntime.manifest.i("No need to sync bill Data, server md5:${server} local md5:${md5}")
            return@withContext
        }
        AppRuntime.manifest.d("Sync bills:$sync")
        BookBillAPI.put(bills, md5, Setting.HASH_BILL)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步支出账单到自动记账")
        }
    }
}