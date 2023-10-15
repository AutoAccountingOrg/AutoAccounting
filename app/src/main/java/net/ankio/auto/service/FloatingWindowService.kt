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
import android.content.Intent
import android.os.IBinder
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.EasyFloat.Companion.hide
import com.lzf.easyfloat.EasyFloat.Companion.show
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import net.ankio.auto.R
import net.ankio.auto.database.table.BillInfo

class FloatingWindowService:Service() {
    override fun onBind(p0: Intent?): IBinder? {
       return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intData  = intent?:return START_STICKY
        val value = intData.getStringExtra("data") ?: return START_STICKY
        val billInfo = BillInfo.fromJSON(value)
        EasyFloat.with(this)
            // 设置浮窗xml布局文件/自定义View，并可设置详细信息
            .setLayout(R.layout.float_tip) { }
            // 设置浮窗显示类型，默认只在当前Activity显示，可选一直显示、仅前台显示
            .setShowPattern(ShowPattern.BACKGROUND)
            // 设置吸附方式，共15种模式，详情参考SidePattern
            .setSidePattern(SidePattern.RIGHT)
            // 设置浮窗的标签，用于区分多个浮窗
            .setTag(billInfo.channel)
            // 设置浮窗是否可拖拽
            .setDragEnable(true)
            // 设置浮窗的出入动画，可自定义，实现相应接口即可（策略模式），无需动画直接设置为null
            .setAnimator(DefaultAnimator())
            .registerCallback {

            }
            // 创建浮窗（这是关键哦😂）
            .show()
        // 返回适当的标志位，例如 START_STICKY
        return START_STICKY
    }
}