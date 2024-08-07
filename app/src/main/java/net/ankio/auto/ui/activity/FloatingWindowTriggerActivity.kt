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
import androidx.appcompat.app.AppCompatActivity
import net.ankio.auto.service.FloatingWindowService
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
            Logger.i("解析数据 => $data")
            // 将数据传递给悬浮窗服务
            val serviceIntent =
                Intent(this, FloatingWindowService::class.java).apply {
                    putExtra("id", data.getQueryParameter("id")?.toIntOrNull()?:0)
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
