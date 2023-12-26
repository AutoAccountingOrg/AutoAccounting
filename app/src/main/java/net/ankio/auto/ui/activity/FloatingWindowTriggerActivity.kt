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
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.database.table.BillInfo
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.utils.FloatPermissionUtils
import net.ankio.auto.utils.SpUtils

class FloatingWindowTriggerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.getStringExtra("data")
        if (data === null) {
            //没数据耍流氓
            finish()
            return
        }
        val billInfo = BillInfo.fromJSON(data)
        // 如果是全自动记账，则不会显示这个窗口，直接弹出记账成功
        if(SpUtils.getBoolean("float_auto")){
            lifecycleScope.launch {
                BillUtils.groupBillInfo(billInfo)
                //免打扰不显示提示
                if(!SpUtils.getBoolean("float_no_disturb")){
                    Toast.makeText(App.context,getString(R.string.auto_success,billInfo.money.toString()),Toast.LENGTH_LONG).show()
                }
                finish()
            }
            return
        }

        //以下需要悬浮窗

        if(!FloatPermissionUtils.checkPermission(this)){
            Toast.makeText(this, R.string.floatTip,Toast.LENGTH_LONG).show()
            FloatPermissionUtils.requestPermission(this)
            finish()
            return
        }

        Log.e("悬浮窗口启动",data.toString())
        // 将数据传递给悬浮窗服务
        val serviceIntent = Intent(this, FloatingWindowService::class.java).apply {
            putExtra("data", data)
        }
        startService(serviceIntent)
        // 关闭 Activity
        finish()
    }
}