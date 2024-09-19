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

package net.ankio.auto.ui.activity

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.BillState
import org.ezbook.server.db.model.BillInfoModel


class FloatingWindowTriggerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            //判断当前是横屏还是竖屏
            val orientation = resources.configuration.orientation
            if (orientation == 2) {
                //横屏切换为竖屏
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }

            lifecycleScope.launch {
                // 获取 Intent 数据
                val id = intent.getLongExtra("id",0L)
                if (id != 0L) {
                   val billInfo = BillInfoModel.get(id)?:return@launch
                   if (billInfo.state!==BillState.Wait2Edit) {
                       return@launch
                   }
                }
                // 将数据传递给悬浮窗服务
                val serviceIntent = Intent(this@FloatingWindowTriggerActivity, FloatingWindowService::class.java).apply {
                    intent.extras?.let { putExtras(it) } // 直接传递所有 extras
                }
                startService(serviceIntent)
                // 关闭 Activity
                exitActivity()
            }


        }.onFailure {
            Logger.e("解析数据失败", it)
            exitActivity()
        }
    }

    private fun exitActivity() {
        finishAffinity()
        overridePendingTransition(0, 0)
        val options = ActivityOptions.makeBasic()
        window.setWindowAnimations(0)
    }
}
