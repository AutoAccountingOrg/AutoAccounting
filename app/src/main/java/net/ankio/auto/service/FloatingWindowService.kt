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


import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.quickersilver.themeengine.ThemeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.utils.SpUtils
import kotlin.coroutines.CoroutineContext


class FloatingWindowService : Service(), CoroutineScope {
    private val windowManager: WindowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val floatingViews = mutableListOf<FloatTipBinding>()
    private val job = Job()


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("SetTextI18n")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val value = intent.getStringExtra("data") ?: return START_NOT_STICKY
        val timeCount: Int = SpUtils.getInt("float_timeout",10)
        val billInfo = BillInfo.fromJSON(value)
        val defaultTheme = ContextThemeWrapper(this,R.style.AppTheme)
        val themedContext = ContextThemeWrapper(defaultTheme, ThemeEngine.getInstance(applicationContext).getTheme())

        if (timeCount == 0) {
            callBillInfoEditor(billInfo,themedContext)
            // 显示编辑悬浮窗
            return START_NOT_STICKY
        }

        Log.e("启动悬浮窗服务", value)
         // 使用 ViewBinding 初始化悬浮窗视图
        val binding = FloatTipBinding.inflate(LayoutInflater.from(themedContext))
        binding.root.visibility = View.INVISIBLE
        binding.money.text = billInfo.money.toString()

        binding.time.text = timeCount.toString() + "s"

        val countDownTimer = object : CountDownTimer(timeCount * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.time.text = (millisUntilFinished / 1000).toString() + "s"
            }

            override fun onFinish() {
               //取消倒计时
                removeTips(binding)
                when(SpUtils.getInt("float_timeout_result",0)){
                    0 -> {
                        //打开
                        callBillInfoEditor(billInfo,themedContext)
                    }
                    1 -> {
                        //直接记账
                        recordBillInfo(billInfo)
                    }
                }
            }
        }
        countDownTimer.start()

        binding.root.setOnClickListener {
            countDownTimer.cancel()//定时器停止
            removeTips(binding)
            when (SpUtils.getInt("float_click", 0)) {
                0 -> {
                    callBillInfoEditor(billInfo,themedContext)
                }

                1 -> {
                    recordBillInfo(billInfo)
                }
            }
        }

        binding.root.setOnLongClickListener {
            //不记录
            removeTips(binding)
            false
        }

        // 设置 WindowManager.LayoutParams
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = 0 // 居中
            y = -120 // 居中偏上
            gravity = Gravity.CENTER or Gravity.END
        }

        // 将视图添加到 WindowManager
        windowManager.addView(binding.root, params)
        binding.root.post {
            val widthInner =
                binding.logo.width + binding.money.width + binding.time.width + 150 /*logo间隔*/
            // 更新悬浮窗的宽度和高度
            params.width = widthInner // 新宽度，单位：像素
            params.height = binding.logo.height + 60  // 新高度，单位：像素
            // 应用新的布局参数
            windowManager.updateViewLayout(binding.root, params)
            binding.root.visibility = View.VISIBLE
        }


        // 将绑定添加到列表中以便管理
        floatingViews.add(binding)

        // 可以使用 binding 访问视图元素，例如设置监听器
        // binding.someView.setOnClickListener { ... }

        return START_NOT_STICKY
    }

    private fun removeTips(binding: FloatTipBinding){
        windowManager.removeView(binding.root)
        floatingViews.remove(binding)
    }

    private fun recordBillInfo(billInfo:BillInfo){
        launch {
            BillUtils.groupBillInfo(billInfo)
            if(!SpUtils.getBoolean("float_no_disturb")){
                Toast.makeText(
                    App.context,getString(R.string.auto_success,billInfo.money.toString()),
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun callBillInfoEditor(billInfo: BillInfo, themedContext: Context){
       FloatEditorDialog(themedContext,billInfo).show(true)
    }

    override fun onDestroy() {
        // 清理所有悬浮窗
        for (binding in floatingViews) {
            windowManager.removeView(binding.root)
        }
        floatingViews.clear()
        job.cancel()
        super.onDestroy()
    }
}
