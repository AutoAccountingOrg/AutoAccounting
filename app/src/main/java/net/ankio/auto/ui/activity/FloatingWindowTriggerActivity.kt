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
import net.ankio.auto.R
import net.ankio.auto.service.FloatingWindowService


class FloatingWindowTriggerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.TransparentActivityTheme)
        setContentView(R.layout.activity_transparent)
        // 启动服务,传递intent
        val intent = intent
        val serviceIntent = Intent(this, FloatingWindowService::class.java).apply {
            intent.extras?.let { putExtras(it) } // 直接传递所有 extras
        }
        startService(serviceIntent)
        // 关闭 Activity
        exitActivity()
    }




    private fun exitActivity() {
        finishAffinity()
        overridePendingTransition(0, 0)
        window.setWindowAnimations(0)
    }
}
