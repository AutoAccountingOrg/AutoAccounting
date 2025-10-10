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
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BookBillModel
import java.lang.reflect.Proxy
import java.util.Calendar
import java.util.HashSet
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import net.ankio.auto.http.api.BookBillAPI
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.xposed.core.logger.Logger

/**
 * 通过 Xposed 反射驱动钱迹的报销模块：
 * - 获取可报销账单列表（监听宿主回调 `onGetList`）
 * - 触发报销动作（调用宿主 `doBaoXiao(...)`）
 * - 同步报销数据到自动记账服务端
 *
 * 设计要点：
 * - 所有跨进程对象均由宿主类加载器创建/传递，避免类型不匹配
 * - 通过代理对象接收列表回调，协程挂起直至回调触发
 * - 尽量减少侵入，遵从宿主签名与线程模型
 */
object BxPresenterImpl : HookerClazz() {
    private const val CLAZZ = "com.mutangtech.qianji.bill.baoxiao.BxPresenterImpl"
    private val baoXiaoImpl by lazy { clazz() }
    override var rule = net.ankio.dex.model.Clazz(name = this::class.java.name, nameRule = CLAZZ)


    /**
     * 获取可报销账单列表。
     *
     * 实现：反射构造宿主 `BxPresenterImpl`，以代理对象接收 `onGetList` 回调；
     * 调用宿主 `refresh(...)` 触发加载，回调到达时恢复协程并返回账单列表。
     *
     * @param books 账本筛选，须为宿主类加载器下的 Book 实例
     * @return 宿主返回的账单列表（元素类型由宿主定义）
     */
    private suspend fun getBaoXiaoList(books: List<*>): List<*> =
        suspendCoroutine { continuation ->
            var resumed = false
            val constructor = baoXiaoImpl.constructors.first()!!
            // public BxPresenterImpl(t8.b bVar) {
            val param1Clazz = constructor.parameterTypes.first()!!
            val param1Object = Proxy.newProxyInstance(
                AppRuntime.classLoader,
                arrayOf(param1Clazz)
            ) { proxy, method, args ->
                if (method.name == "onGetList") {
                    if (!resumed) {
                        resumed = true
                        val billList = args[0]
                        continuation.resume(billList as List<*>)
                    }
                }
            }
            // public void refresh(t8.c cVar, BookFilter bookFilter, KeywordFilter keywordFilter[, boolean force])
            // 兼容不同版本：优先选择4参重载；否则回退到3参重载
            val refreshMethods = baoXiaoImpl.declaredMethods.filter { it.name == "refresh" }
            val refreshMethod = refreshMethods.find { it.parameterTypes.size == 4 }
                ?: refreshMethods.find { it.parameterTypes.size == 3 }
                ?: throw NoSuchMethodException("BxPresenterImpl.refresh not found")

            val clazzEnum = refreshMethod.parameterTypes[0]

            val enumValue =
                clazzEnum?.declaredFields?.firstOrNull { it.name == "NOT" }!!
                    .get(null)

            //BookFilter
            val bookFilter = XposedHelpers.newInstance(refreshMethod.parameterTypes[1])
            books.forEach {
                XposedHelpers.callMethod(bookFilter, "add", it)
            }

            //KeywordFilter
            val keywordFilter = XposedHelpers.newInstance(refreshMethod.parameterTypes[2], "")


            val instance = XposedHelpers.newInstance(baoXiaoImpl, param1Object)
            if (refreshMethod.parameterTypes.size == 4) {
                XposedHelpers.callMethod(
                    instance,
                    "refresh",
                    enumValue,
                    bookFilter,
                    keywordFilter,
                    true
                )
            } else {
                XposedHelpers.callMethod(
                    instance,
                    "refresh",
                    enumValue,
                    bookFilter,
                    keywordFilter
                )
            }

        }


    /**
     * 同步报销账单信息到自动记账服务端。
     * - 主进程取列表，转换为 `BookBillModel` 列表并序列化
     * - 使用 MD5 去重：若与服务端一致且非调试模式则跳过
     * - 成功后主线程提示
     */
    suspend fun syncBaoXiao() = withContext(Dispatchers.IO) {
        val books = BookManagerImpl.getBooks()
        // 报销账单
        val bxList = getBaoXiaoList(books)

        val bills = convert2Bill(bxList, Setting.HASH_BAOXIAO_BILL)
        val sync = Gson().toJson(bills)
        val md5 = MD5HashTable.md5(sync)
        val server = SettingAPI.get(Setting.HASH_BAOXIAO_BILL, "")
        if (server == md5 && !AppRuntime.debug) {
            AppRuntime.manifest.i("No need to sync BaoXiao, server md5:${server} local md5:${md5}")
            return@withContext
        }
        AppRuntime.manifest.d("Sync BaoXiao:$sync")
        BookBillAPI.put(bills, md5, Setting.HASH_BAOXIAO_BILL)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步报销账单到自动记账")
        }
    }

    /**
     * 触发报销。
     *
     * 来源数据：
     * - `billModel.extendData`：以逗号分隔的待报销账单 ID 列表（字符串）
     * - `billModel.accountNameFrom`：入账资产名称
     * - `billModel.money`：本次报销总金额（用于宿主 doBaoXiao 第三个参数）
     * - `billModel.time`：报销时间（毫秒）
     *
     * 流程：
     * 1) 拉取可报销列表并按 ID 过滤
     * 2) 组装宿主所需参数（Set<Bill>、AssetAccount、金额、时间、CurrencyExtra、备注、图片链接、标签）
     * 3) 反射调用宿主 `doBaoXiao(...)`
     */
    suspend fun doBaoXiao(billModel: BillInfoModel) = withContext(Dispatchers.Main) {

        val books = BookManagerImpl.getBooks()

        // 解析待报销账单 ID 列表，容错任意空白
        val list = billModel.extendData.split(Regex("\\s*,\\s*"))
            .map { it.trim() }
            .distinct()
            .toMutableList()

        val billList = getBaoXiaoList(books)

        val selectBills =
            billList.filter {
                val bill = QjBillModel.fromObject(it!!)
                val billId = bill.getBillid()
                AppRuntime.manifest.d("billId:$billId")
                // 判断billId是否在list中
                list.contains(billId.toString())
            }

        if (selectBills.isEmpty()) {
            throw RuntimeException("没有找到需要报销的账单")
        }

        val constructor = baoXiaoImpl.constructors.first()!!
        // public BxPresenterImpl(t8.b bVar) {
        val param1Clazz = constructor.parameterTypes.first()!!
        val param1Object = Proxy.newProxyInstance(
            AppRuntime.classLoader,
            arrayOf(param1Clazz)
        ) { _, _, _ ->

        }
        val baoXiaoInstance = XposedHelpers.newInstance(baoXiaoImpl, param1Object)

        //    public void doBaoXiao(
        //    java.util.Set<? extends com.mutangtech.qianji.data.model.Bill> r36,
        //    com.mutangtech.qianji.data.model.AssetAccount r37,
        //    double r38,
        //    java.util.Calendar r40,
        //    com.mutangtech.qianji.data.model.CurrencyExtra r41,
        //    java.lang.String r42,
        //    java.util.List<java.lang.String> r43,
        //    java.util.List<? extends com.mutangtech.qianji.data.model.Tag> r44) {


        // java.util.Set<? extends com.mutangtech.qianji.data.model.Bill> r36,
        val set = HashSet<Any>(selectBills)

        // com.mutangtech.qianji.data.model.AssetAccount r37,
        val asset =
            AssetPreviewPresenterImpl.getAssetByName(billModel.accountNameFrom)
                ?: throw RuntimeException("找不到资产 key=accountname;value=${billModel.accountNameFrom}")


        // double r38,
        val money = billModel.money

        // java.util.Calendar r40,
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = billModel.time

        //com.mutangtech.qianji.data.model.CurrencyExtra r41
        // 注：此处取首个账单的 CurrencyExtra；若业务为统一报销汇率，可按需要改为显式输入
        val currencyExtraInstance =
            XposedHelpers.callMethod(selectBills.first(), "getCurrencyExtra")

        //java.lang.String r42,
        val str = ""

        // java.util.List<java.lang.String> r43, 这是图片链接
        val listStr = arrayListOf<String>()


        // java.util.List<? extends com.mutangtech.qianji.data.model.Tag> r44
        val listTag = arrayListOf<Any>()

        XposedHelpers.callMethod(
            baoXiaoInstance,
            "doBaoXiao",
            set,
            asset.toObject(),
            money,
            calendar,
            currencyExtraInstance,
            str,
            listStr,
            listTag
        )

    }

    /**
     * 将宿主账单对象转换为服务端同步模型。
     *
     * @param anyBills 宿主账单对象列表
     * @param type 同步分类标识（例如 `Setting.HASH_BAOXIAO_BILL`）
     * @return 转换后的 `BookBillModel` 列表
     */
    fun convert2Bill(anyBills: List<*>, type: String): ArrayList<BookBillModel> {
        val bills = arrayListOf<BookBillModel>()
        AppRuntime.manifest.d("账单总数：${anyBills.size}")
        anyBills.forEach {
            AppRuntime.manifest.d("报销/支出：$it")
            if (it == null) {
                return@forEach
            }
            val bill = BookBillModel()
            val billModel = QjBillModel.fromObject(it)
            bill.money = billModel.getMoney()
            bill.remoteId = billModel.getBillid().toString()
            bill.remark = billModel.getRemark() ?: ""
            bill.time = billModel.getTimeInSec() * 1000
            bill.remoteBookId = billModel.getBookId().toString()
            bill.category = billModel.getCategory()?.getName() ?: ""
            bill.type = type
            bills.add(bill)

            // 债务账单
        }
        return bills
    }
}