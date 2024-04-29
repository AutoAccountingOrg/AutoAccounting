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

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.hjq.toast.Toaster
import net.ankio.auto.R
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.utils.FloatPermissionUtils
import net.ankio.auto.utils.Logger

class FloatingWindowTriggerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 获取启动此Activity的Intent
        val intent = intent
        // 获取Intent中的URI数据
        val data = intent.data
        if (data === null) {
            // 没数据耍流氓
            exitActivity()
            return
        }

        runCatching {
            val d = data.getQueryParameter("data")!!.replace(" ", "+")
            val dataValue = String(Base64.decode(d, Base64.NO_WRAP)) // 获取名为"data"的查询参数的值
            Logger.d("悬浮窗口启动 $dataValue")
            // 以下需要悬浮窗
            if (!FloatPermissionUtils.checkPermission(this)) {
                Toaster.show(R.string.floatTip)
                FloatPermissionUtils.requestPermission(this)
                exitActivity()
                return
            }

            // 将数据传递给悬浮窗服务
            val serviceIntent =
                Intent(this, FloatingWindowService::class.java).apply {
                    putExtra("data", dataValue)
                }
            startService(serviceIntent)
            // 关闭 Activity
            exitActivity()
        }.onFailure {
            Logger.e("解析数据失败", it)
            exitActivity()
        }
    }

    private fun exitActivity() {
        finishAffinity()
        overridePendingTransition(0, 0)
    }
}
