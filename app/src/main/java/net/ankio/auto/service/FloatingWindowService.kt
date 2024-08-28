/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.BadTokenException
import androidx.core.content.ContextCompat
import com.hjq.toast.Toaster
import com.quickersilver.themeengine.ThemeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.models.BillInfoModel
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.SpUtils
import kotlin.system.exitProcess

class FloatingWindowService : Service() {
    private val windowManager: WindowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val floatingViews = mutableListOf<FloatTipBinding>()
    private lateinit var themedContext: Context
    private val list = ArrayDeque<BillInfoModel>()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private var processBillInfo = false
    private var timeCount: Int = 0

    private var billInfoModel: BillInfoModel? = null


    override fun onCreate() {
        super.onCreate()
        timeCount = runCatching { SpUtils.getString("setting_float_time", "10").toInt() }.getOrNull() ?: 0
        list.clear()
        val defaultTheme = ContextThemeWrapper(applicationContext, R.style.AppTheme)

        themedContext =
            ContextThemeWrapper(
                defaultTheme,
                ThemeEngine.getInstance(applicationContext).getTheme(),
            )
        AppUtils.getScope().launch {
            withContext(Dispatchers.IO) {
                var hasProcessOneData = false
                while (true) {
                    Logger.i("list size:${list.size}，processBillInfo=${processBillInfo},hasProcessOneData=$hasProcessOneData, list.isEmpty()=${list.isEmpty()}, !hasProcessOneData && list.isEmpty() = ${!hasProcessOneData && list.isEmpty()}")
                    if (processBillInfo || (!hasProcessOneData && list.isEmpty())) {
                        delay(1000)
                        continue
                    }

                    if(list.isEmpty()){
                        break
                    }

                    billInfoModel = list.removeFirst()
                    hasProcessOneData = true
                    runCatching {
                        processBillInfo = true
                        processBillInfo()
                    }.onFailure {
                        processBillInfo = false
                      onError(it)
                        Logger.e("记账失败", it)

                    }
                }
            }
        }
    }

    private fun onError(it:Throwable){
        if (it is BadTokenException) {
            if (it.message != null && it.message!!.contains("permission denied")) {
                Toaster.show(R.string.floatTip)
                exitProcess(0)
            }
        }
    }


    private suspend fun addAndCheckBill(id:Int) = withContext(Dispatchers.IO){
     /*   val billArray = BillInfoModel.getBillByIds(id.toString())
        if(billArray.isEmpty()){
            return@withContext
        }
        val bill = billArray[0]
        *//**
         * 因为原始账单全部是没有处理过的，所以这里根据处理之前的结果判断重复
         *//*
        if(list.isEmpty()){
            val bills = BillInfoModel.getNoEditBills()

            list.addAll(bills)
            list.remove(bill)
        }
        if(BillUtils.noNeedFilter(bill)){
            Logger.i("不需要过滤的账单:$bill, list count: ${list.size}")
            list.add(bill)
            return@withContext
        }

        if(billInfoModel != null){
            if(checkRepeat(bill,billInfoModel!!)){
                mergeBillAndUpdate(bill,billInfoModel!!)
                EventBus.post(BillUpdateEvent(billInfoModel!!))
                return@withContext
            }
        }

        if(checkBills(bill)){
            return@withContext
        }

        list.add(bill)*/
    }

    private suspend fun checkBills(bill: BillInfoModel, remove:Boolean = false):Boolean{
       /* list.forEach { bill2 ->
            if(checkRepeat(bill,bill2)){
                mergeBillAndUpdate(bill,bill2)
                if(remove){
                    BillInfoModel.remove(bill.id)
                }
                return true
            }
        }
        //从历史记录中判断是否有重复账单
        val history = BillInfoModel.getEditBills()

        history.forEach { bill2 ->
            if(checkRepeat(bill,bill2)){
                mergeBillAndUpdate(bill,bill2)
                if(remove){
                    BillInfoModel.remove(bill.id)
                }
                return true
            }
        }*/

        return false
    }

    /**
     * 重复账单的要素：
     * 1.金额一致
     * 2.来源平台不同  //这个逻辑不对，可能同一个平台可以获取到多个信息，例如多次转账同一金额给同一个人
     * 来源渠道不同（可以是同一个App，但是可以是不同的公众号
     * 3.账单时间不超过15分钟 //不能根据时间判断，微信消息的时间不准确
     * 4.账单的类型一致，只有收入或者支出需要进行区分
     * 5.账单的交易账户部分一致（有的交易无法获取完整的账户信息）
     * bill是新来的账单，bill2是原始的账单
     */

    private suspend fun checkRepeat(bill: BillInfoModel, bill2: BillInfoModel): Boolean {
        Logger.i("重复性比较")
        Logger.i("bill:$bill")
        Logger.i("bill2:$bill2")

        Logger.i("bill2.time == bill.time => ${bill2.time == bill.time}")
        Logger.i("bill.money == bill2.money => ${bill.money == bill2.money}")
        Logger.i("bill.type == bill2.type => ${bill.type == bill2.type}")
        Logger.i("bill2.channel != bill.channel => ${bill2.channel != bill.channel}")
        if (bill2.money != bill.money){
            return false
        }

        if (bill.type == bill2.type){
            if(bill2.time == bill.time) return true
            if (bill2.channel != bill.channel)return true
            if (bill2.accountNameFrom == bill.accountNameFrom) return true
            if(bill2.shopItem == bill.shopItem && bill.shopName == bill2.shopName) return true
        }
        return false
    }

    /**
     * 重复账单进行合并
     * bill是新来的账单，bill2是原始的账单
     */
    private suspend fun mergeRepeatBill(bill: BillInfoModel, bill2: BillInfoModel) {
        //合并支付方式
        if (bill2.accountNameFrom.length < bill.accountNameFrom.length) {
            bill2.accountNameFrom = bill.accountNameFrom
        }
        if (bill2.accountNameTo.length < bill.accountNameTo.length) {
            bill2.accountNameTo = bill.accountNameTo
        }
        //合并商户信息
        if (bill2.shopName.length < bill.shopName.length) {
            bill2.shopName = bill.shopName
        }
        //合并商品信息
        if (bill2.shopItem.length < bill.shopItem.length) {
            bill2.shopItem = bill.shopItem
        }

        //合并商品信息
        if (bill2.extendData.length < bill.extendData.length) {
            bill2.extendData = bill.extendData
        }

        if(bill2.shopItem.isEmpty()){
            bill2.shopItem = bill2.extendData
        }

        //最后重新生成备注
        bill2.remark = BillUtils.getRemark(
            bill2,
            SpUtils.getString("setting_bill_remark", "【商户名称】 - 【商品名称】")
        )
    }

    private suspend fun mergeBillAndUpdate(bill: BillInfoModel, bill2: BillInfoModel) {
        Logger.i("重复账单:$bill")
        bill.groupId = bill2.id
        mergeRepeatBill(bill, bill2)
        BillInfoModel.put(bill)
        BillInfoModel.put(bill2)

    }
    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int,
    ): Int {
        val id = intent.getIntExtra("id", 0)
        AppUtils.getScope().launch {
            addAndCheckBill(id)
        }

        return START_REDELIVER_INTENT
    }

    private suspend fun processBillInfo() = withContext(Dispatchers.Main) {
        if(checkBills(billInfoModel!!,true)){
            processBillInfo = false
            return@withContext
        }
        if(billInfoModel!!.shopItem.isEmpty()){
            billInfoModel!!.shopItem = billInfoModel!!.extendData
        }
       // billInfo!!.syncFromApp = 0
        val tpl = SpUtils.getString("setting_bill_remark", "【商户名称】 - 【商品名称】")
        billInfoModel!!.remark = BillUtils.getRemark(billInfoModel!!, tpl)


        BillUtils.setAccountMap(billInfoModel!!)


        Logger.i("timeCount:$timeCount")

        Logger.i("BillInfo:${billInfoModel!!.toJson()}")

        if (timeCount == 0) {
            callBillInfoEditor("setting_float_on_badge_timeout")
            // 显示编辑悬浮窗
            return@withContext
        }

        // 使用 ViewBinding 初始化悬浮窗视图
        val binding = FloatTipBinding.inflate(LayoutInflater.from(themedContext))
        binding.root.visibility = View.INVISIBLE
        binding.money.text = billInfoModel!!.money.toString()

        val colorRes = BillUtils.getColor(billInfoModel!!.type)
        val color = ContextCompat.getColor(themedContext, colorRes)
        binding.money.setTextColor(color)
        binding.time.text = String.format("%ss", timeCount.toString())

        val countDownTimer =
            object : CountDownTimer(timeCount * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    binding.time.text = String.format("%ss", (millisUntilFinished / 1000).toString())
                }

                override fun onFinish() {
                    // 取消倒计时
                    removeTips(binding)
                    callBillInfoEditor("setting_float_on_badge_timeout")
                }
            }
        countDownTimer.start()

        binding.root.setOnClickListener {
            countDownTimer.cancel() // 定时器停止
            removeTips(binding)
            callBillInfoEditor("setting_float_on_badge_click")
        }

        binding.root.setOnLongClickListener {
            countDownTimer.cancel() // 定时器停止
            removeTips(binding)
            // 不记录
            callBillInfoEditor("setting_float_on_badge_long_click")
            true
        }

        // 设置 WindowManager.LayoutParams
        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                x = 0 // 居中
                y = -120 // 居中偏上
                gravity = Gravity.CENTER or Gravity.END
            }

        // 将视图添加到 WindowManager
        windowManager.addView(binding.root, params)
        binding.root.post {
            val widthInner =
                binding.logo.width + binding.money.width + binding.time.width + 150 // logo间隔
            // 更新悬浮窗的宽度和高度
            params.width = widthInner // 新宽度，单位：像素
            params.height = binding.logo.height + 60 // 新高度，单位：像素
            // 应用新的布局参数
            windowManager.updateViewLayout(binding.root, params)
            binding.root.visibility = View.VISIBLE
        }

        // 将绑定添加到列表中以便管理
        floatingViews.add(binding)
    }

    private fun removeTips(binding: FloatTipBinding) {
        if (binding.root.isAttachedToWindow) windowManager.removeView(binding.root)
        floatingViews.remove(binding)
    }

    private fun recordBillInfo(billInfoModel2: BillInfoModel) {
        runCatching {
            billInfoModel2.syncFromApp = 0
            AppUtils.getScope().launch {
                BillInfoModel.put(billInfoModel2)
            }
            if (SpUtils.getBoolean("setting_book_success", true)) {
                Toaster.show(
                    getString(
                        R.string.auto_success,
                        billInfoModel2.money.toString(),
                    ),
                )
            }
            billInfoModel = null
        }.onFailure {

        }
    }

    private fun callBillInfoEditor(key: String) {
        when (SpUtils.getInt(key, FloatEvent.POP_EDIT_WINDOW.ordinal)) {
            FloatEvent.AUTO_ACCOUNT.ordinal -> {
                // 记账
                recordBillInfo(billInfoModel!!)
                processBillInfo = false
            }

            FloatEvent.POP_EDIT_WINDOW.ordinal -> {
                AppUtils.getScope().launch {
                    /*AppUtils.getService().config().let {
                        // 编辑
                        withContext(Dispatchers.Main) {
                           runCatching {
                               FloatEditorDialog(themedContext, billInfoModel!!, it, true, false, onCancelClick = {
                                   AppUtils.getScope().launch {
                                      *//* BillInfoModel.remove(billInfoModel!!.id)*//*
                                   }
                               }, onClose = {
                                   processBillInfo = false
                               }).show(true)
                           }.onFailure {
                               processBillInfo = false
                               onError(it)
                               Logger.e("记账失败", it)
                           }
                        }
                    }*/
                }

            }

            FloatEvent.NO_ACCOUNT.ordinal -> {
                processBillInfo = false
                AppUtils.getScope().launch {
                  /*  BillInfoModel.remove(billInfoModel!!.id)*/
                }
            }
        }
    }

    override fun onDestroy() {
        // 清理所有悬浮窗
        for (binding in floatingViews) {
            if (binding.root.isAttachedToWindow) windowManager.removeView(binding.root)
        }
        floatingViews.clear()
        super.onDestroy()
    }
}
