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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.events.AutoServiceErrorEvent
import net.ankio.auto.events.BillUpdateEvent
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.FloatPermissionUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.event.EventBus
import net.ankio.auto.utils.server.model.BillInfo

class FloatingWindowService : Service() {
    private val windowManager: WindowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val floatingViews = mutableListOf<FloatTipBinding>()
    private lateinit var themedContext: Context
    private val list = ArrayDeque<BillInfo>()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private var showWindow = false
    private var timeCount: Int = 0

    private var billInfo: BillInfo? = null


    override fun onCreate() {
        super.onCreate()
        timeCount = runCatching { SpUtils.getString("setting_float_time", "10").toInt() }.getOrNull() ?: 0

        val defaultTheme = ContextThemeWrapper(applicationContext, R.style.AppTheme)

        themedContext =
            ContextThemeWrapper(
                defaultTheme,
                ThemeEngine.getInstance(applicationContext).getTheme(),
            )
        AppUtils.getScope().launch {
            withContext(Dispatchers.IO) {
                var count = 0
                while (isActive || count < 600) {
                    if (list.isEmpty()  || showWindow) {
                        count++
                        delay(100)
                        continue
                    }
                    count = 0
                    billInfo = list.removeFirst()
                    runCatching {
                        processBillInfo()
                    }.onFailure {
                        if (it is BadTokenException) {
                            if (it.message != null && it.message!!.contains("permission denied")) {
                                Toaster.show(R.string.floatTip)
                                FloatPermissionUtils.requestPermission(themedContext)
                            }
                        }
                        Logger.e("记账失败", it)
                    }
                }
            }
        }
    }


    private suspend fun addAndCheckBill(id:Int) = withContext(Dispatchers.IO){
        val billArray = BillInfo.getBillByIds(id.toString())
        if(billArray.isEmpty()){
            return@withContext
        }
        val bill = billArray[0]
        /**
         * 因为原始账单全部是没有处理过的，所以这里根据处理之前的结果判断重复
         */
        if(list.isEmpty()){
            val bills = BillInfo.getNoEditBills()

            list.addAll(bills)
            list.remove(bill)
        }
        if(BillUtils.noNeedFilter(bill)){
            Logger.i("不需要过滤的账单:$bill")
            list.add(bill)
            return@withContext
        }

        if(billInfo != null){
            if(checkRepeat(bill,billInfo!!)){
                mergeBillAndUpdate(bill,billInfo!!)
                EventBus.post(BillUpdateEvent(billInfo!!))
                return@withContext
            }
        }

        if(checkBills(bill)){
            return@withContext
        }

        list.add(bill)
    }

    private suspend fun checkBills(bill:BillInfo, remove:Boolean = false):Boolean{
        list.forEach { bill2 ->
            if(checkRepeat(bill,bill2)){
                mergeBillAndUpdate(bill,bill2)
                if(remove){
                    BillInfo.remove(bill.id)
                }
                return true
            }
        }
        //从历史记录中判断是否有重复账单
        val history = BillInfo.getEditBills()

        history.forEach { bill2 ->
            if(checkRepeat(bill,bill2)){
                mergeBillAndUpdate(bill,bill2)
                if(remove){
                    BillInfo.remove(bill.id)
                }
                return true
            }
        }

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

    private suspend fun checkRepeat(bill: BillInfo, bill2: BillInfo): Boolean {
        //金额和时间完全一致的是重复
        if (bill2.time == bill.time && bill.money == bill2.money && bill.type == bill2.type) return true
        if (bill2.money == bill.money && bill.type == bill2.type) {
            if (bill2.channel != bill.channel) {
                return true
            }
        }
        return false
    }

    /**
     * 重复账单进行合并
     * bill是新来的账单，bill2是原始的账单
     */
    private suspend fun mergeRepeatBill(bill: BillInfo, bill2: BillInfo) {
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

    private suspend fun mergeBillAndUpdate(bill: BillInfo, bill2: BillInfo) {
        Logger.i("重复账单:$bill")
        bill.groupId = bill2.id
        mergeRepeatBill(bill, bill2)
        BillInfo.put(bill)
        BillInfo.put(bill2)

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
        if(checkBills(billInfo!!,true)){
            return@withContext
        }
        if(billInfo!!.shopItem.isEmpty()){
            billInfo!!.shopItem = billInfo!!.extendData
        }
       // billInfo!!.syncFromApp = 0
        showWindow = true
        val tpl = SpUtils.getString("setting_bill_remark", "【商户名称】 - 【商品名称】")
        billInfo!!.remark = BillUtils.getRemark(billInfo!!, tpl)
        BillUtils.setAccountMap(billInfo!!)
        if (timeCount == 0) {
            callBillInfoEditor("setting_float_on_badge_timeout")
            // 显示编辑悬浮窗
            return@withContext
        }

        // 使用 ViewBinding 初始化悬浮窗视图
        val binding = FloatTipBinding.inflate(LayoutInflater.from(themedContext))
        binding.root.visibility = View.INVISIBLE
        binding.money.text = billInfo!!.money.toString()

        val colorRes = BillUtils.getColor(billInfo!!.type)
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

    private fun recordBillInfo(billInfo2: BillInfo) {
        runCatching {
            billInfo2.syncFromApp = 0
            AppUtils.getScope().launch {
                BillInfo.put(billInfo2)
            }
            if (SpUtils.getBoolean("setting_book_success", true)) {
                Toaster.show(
                    getString(
                        R.string.auto_success,
                        billInfo2.money.toString(),
                    ),
                )
            }
            billInfo = null
        }.onFailure {
            if (it is AutoServiceException) {
                EventBus.post(AutoServiceErrorEvent(it))
            }
        }
    }

    private fun callBillInfoEditor(key: String) {
        when (SpUtils.getInt(key, FloatEvent.POP_EDIT_WINDOW.ordinal)) {
            FloatEvent.AUTO_ACCOUNT.ordinal -> {
                // 记账
                recordBillInfo(billInfo!!)
                showWindow = false
            }

            FloatEvent.POP_EDIT_WINDOW.ordinal -> {
                AppUtils.getScope().launch {
                    AppUtils.getService().config().let {
                        // 编辑
                        withContext(Dispatchers.Main) {
                           runCatching {
                               FloatEditorDialog(themedContext, billInfo!!, it, true, false, onCancelClick = {
                                   AppUtils.getScope().launch {
                                       BillInfo.remove(billInfo!!.id)
                                   }
                               }, onClose = {
                                   showWindow = false
                               }).show(true)
                           }.onFailure {
                               if (it is BadTokenException) {
                                   if (it.message != null && it.message!!.contains("permission denied")) {
                                       Toaster.show(R.string.floatTip)
                                       FloatPermissionUtils.requestPermission(themedContext)
                                   }
                               }
                               Logger.e("记账失败", it)
                           }
                        }
                    }
                }

            }

            FloatEvent.NO_ACCOUNT.ordinal -> {
                showWindow = false
                AppUtils.getScope().launch {
                    BillInfo.remove(billInfo!!.id)
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
