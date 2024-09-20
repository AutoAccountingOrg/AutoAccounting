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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.quickersilver.themeengine.ThemeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.ErrorActivity
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.BillTool
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BillInfoModel


class FloatingWindowService : Service() {
    private val windowManager: WindowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val floatingViews = mutableListOf<FloatTipBinding>()
    private lateinit var themedContext: Context

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private var timeCount: Int = 0

    private var lastTheme = ThemeEngine.getInstance(App.app).getTheme()

    private fun notifyToolBar() {
        val CHANNEL_ID = "FloatingWindowServiceChannel"

        // 创建一个通知通道
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Floating Window Service Channel",
            NotificationManager.IMPORTANCE_MIN // 设置为最小重要性
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        // 创建最小化通知
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("")  // 无标题
            .setContentText("自动记账悬浮窗服务")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)  // 小图标
            .build()

        // 启动前台服务
        startForeground(1, notification)
    }



    override fun onCreate() {
        super.onCreate()
        notifyToolBar()
        timeCount = runCatching {
            ConfigUtils.getString(Setting.FLOAT_TIMEOUT_OFF, "10").toInt()
        }.getOrNull() ?: 0

        val appTheme = ContextThemeWrapper(App.app, R.style.AppTheme)
        themedContext = App.getThemeContext(appTheme)
        lastTheme = ThemeEngine.getInstance(App.app).getTheme()
    }


    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int,
    ): Int {
        notifyToolBar()
        if (lastTheme != ThemeEngine.getInstance(App.app).getTheme()) {
            lastTheme = ThemeEngine.getInstance(App.app).getTheme()
            val appTheme = ContextThemeWrapper(App.app, R.style.AppTheme)
            themedContext = App.getThemeContext(appTheme)
        }


        val billInfoModel =
            Gson().fromJson(intent.getStringExtra("billInfo"), BillInfoModel::class.java)
        val parent = runCatching {
            Gson().fromJson(
                intent.getStringExtra("parent"),
                BillInfoModel::class.java
            )
        }.getOrNull()

        val showWaitTip = intent.getBooleanExtra("showWaitTip", true)

        if (parent != null) {
            //说明是重复账单
            ToastUtils.info("检测到重复账单，已自动合并。")
           return START_REDELIVER_INTENT
        }

        runCatching {
            processBillInfo(billInfoModel, showWaitTip)
        }.onFailure {
            // 提醒用户报告错误
            Logger.e("记账失败", it)
            // 跳转错误页面
            val intent2 = Intent(App.app, ErrorActivity::class.java)
            intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val sb = StringBuilder()
            sb.append("自动记账未获取到悬浮窗权限，记账失败！\n")
            sb.append("请在设置中手动授予该权限！\n")
            sb.append(it.message).append("\n")
            it.stackTrace.forEach { message ->
                sb.append(message.toString())
                sb.append("\n")
            }
            intent2.putExtra("msg", sb.toString())
            startActivity(intent2)
        }

        return START_REDELIVER_INTENT
    }


    private fun processBillInfo(billInfoModel: BillInfoModel, showWaitTip: Boolean) {
        Logger.d("timeCount:$timeCount")


        if (timeCount == 0 || !showWaitTip) {
            callBillInfoEditor(Setting.FLOAT_TIMEOUT_ACTION, billInfoModel)
            // 显示编辑悬浮窗
            return
        }

        // 使用 ViewBinding 初始化悬浮窗视图
        val binding = FloatTipBinding.inflate(LayoutInflater.from(themedContext))
        binding.root.visibility = View.INVISIBLE
        binding.money.text = billInfoModel.money.toString()

        val colorRes = BillTool.getColor(billInfoModel.type)
        val color = ContextCompat.getColor(themedContext, colorRes)
        binding.money.setTextColor(color)
        binding.time.text = String.format("%ss", timeCount.toString())

        val countDownTimer =
            object : CountDownTimer(timeCount * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    binding.time.text =
                        String.format("%ss", (millisUntilFinished / 1000).toString())
                }

                override fun onFinish() {
                    // 取消倒计时
                    removeTips(binding)
                    callBillInfoEditor(Setting.FLOAT_TIMEOUT_ACTION, billInfoModel)
                }
            }
        countDownTimer.start()

        binding.root.setOnClickListener {
            countDownTimer.cancel() // 定时器停止
            removeTips(binding)
            callBillInfoEditor(Setting.FLOAT_CLICK, billInfoModel)
        }

        binding.root.setOnLongClickListener {
            countDownTimer.cancel() // 定时器停止
            removeTips(binding)
            // 不记录
            callBillInfoEditor(Setting.FLOAT_LONG_CLICK, billInfoModel)
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
            billInfoModel2.state = BillState.Edited
            App.launch {
                BillInfoModel.put(billInfoModel2)
            }
            if (ConfigUtils.getBoolean(Setting.SHOW_SUCCESS_POPUP, true)) {
                ToastUtils.info(
                    getString(
                        R.string.auto_success,
                        billInfoModel2.money.toString(),
                    ),
                )
            }

        }.onFailure {

        }
    }

    private fun callBillInfoEditor(key: String, billInfoModel: BillInfoModel) {
        when (ConfigUtils.getInt(key, FloatEvent.POP_EDIT_WINDOW.ordinal)) {
            FloatEvent.AUTO_ACCOUNT.ordinal -> {
                // 记账
                recordBillInfo(billInfoModel)
            }

            FloatEvent.POP_EDIT_WINDOW.ordinal -> {
                runCatching {
                    FloatEditorDialog(themedContext, billInfoModel, true, onCancelClick = {
                        App.launch {
                            BillInfoModel.remove(it.id)
                        }
                    }).show(true)
                }.onFailure {
                    Logger.e("记账失败", it)
                }

            }

            FloatEvent.NO_ACCOUNT.ordinal -> {
                App.launch {
                    BillInfoModel.remove(billInfoModel.id)
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
