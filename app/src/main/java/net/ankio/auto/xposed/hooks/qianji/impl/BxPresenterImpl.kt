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

import io.github.oshai.kotlinlogging.KotlinLogging
import com.google.gson.Gson
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.tools.MD5HashTable
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.hooks.qianji.models.Bill
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BookBillModel
import org.ezbook.server.db.model.SettingModel
import java.lang.reflect.Proxy
import java.util.Calendar
import java.util.HashSet
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import net.ankio.auto.http.api.BookBillAPI
import net.ankio.auto.http.api.SettingAPI

object BxPresenterImpl {
    private val logger = KotlinLogging.logger(this::class.java.name)

    val baoXiaoImpl by lazy {
        Hooker.loader("com.mutangtech.qianji.bill.baoxiao.BxPresenterImpl")
    }


    private suspend fun getBaoXiaoList(all: Boolean = false, books: List<*>): List<*> =
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
            // public void refresh(t8.c cVar, BookFilter bookFilter, KeywordFilter keywordFilter) {
            val refreshMethod = baoXiaoImpl.declaredMethods.find { it.name == "refresh" }!!


            val clazzEnum = refreshMethod.parameters[0].type

            val enumValue =
                clazzEnum?.declaredFields?.firstOrNull { it.name == if (all) "ALL" else "NOT" }!!
                    .get(null)

            //BookFilter
            val bookFilter = XposedHelpers.newInstance(refreshMethod.parameters[1].type)
            books.forEach {
                XposedHelpers.callMethod(bookFilter, "add", it)
            }

            //KeywordFilter
            val keywordFilter = XposedHelpers.newInstance(refreshMethod.parameters[2].type, "")


            XposedHelpers.callMethod(
                XposedHelpers.newInstance(baoXiaoImpl, param1Object),
                "refresh",
                enumValue,
                bookFilter,
                keywordFilter
            )

        }


    suspend fun syncBaoXiao() = withContext(Dispatchers.IO) {
        val books = BookManagerImpl.getBooks()
        // 报销账单
        val bxList =
            withContext(Dispatchers.Main) {
                getBaoXiaoList(true, books)
            }

        val bills = convert2Bill(bxList, Setting.HASH_BAOXIAO_BILL)
        val sync = Gson().toJson(bills)
        val md5 = MD5HashTable.md5(sync)
        val server = SettingAPI.get(Setting.HASH_BAOXIAO_BILL, "")
        if (server == md5 && !AppRuntime.debug) {
            logger.info { "No need to sync BaoXiao, server md5:${server} local md5:${md5}" }
            return@withContext
        }
        logger.debug { "Sync BaoXiao:$sync" }
        BookBillAPI.put(bills, md5, Setting.HASH_BAOXIAO_BILL)
        withContext(Dispatchers.Main) {
            MessageUtils.toast("已同步报销账单到自动记账")
        }
    }

    suspend fun doBaoXiao(billModel: BillInfoModel) = withContext(Dispatchers.Main) {

        val books = BookManagerImpl.getBooks()

        val list = billModel.extendData.split(", ")
            .map { it.trim() }
            .distinct()
            .toMutableList()

        val billList =
            withContext(Dispatchers.Main) {
                getBaoXiaoList(true, books)
            }

        val selectBills =
            billList.filter {
                val bill = Bill.fromObject(it!!)
                val billId = bill.getBillid()
                logger.debug { "billId:$billId" }
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

    fun convert2Bill(anyBills: List<*>, type: String): ArrayList<BookBillModel> {
        val bills = arrayListOf<BookBillModel>()
        anyBills.forEach {
            if (it == null) {
                return@forEach
            }
            val bill = BookBillModel()
            val billModel = Bill.fromObject(it)
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