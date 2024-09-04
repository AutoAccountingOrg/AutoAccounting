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

package net.ankio.auto.ui.dialog

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import net.ankio.auto.R
import net.ankio.auto.broadcast.LocalBroadcastHelper
import net.ankio.auto.databinding.DialogAppBinding
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.adapter.AppListAdapter
import net.ankio.auto.ui.models.AutoApp

/**
 * 记账软件选择对话框
 */
class AppDialog(private val context: Context) : BaseSheetDialog(context) {
    private lateinit var binding: DialogAppBinding

    private var apps = mutableListOf(
        AutoApp("钱迹", R.drawable.app_qianji, "com.mutangtech.qianji", "https://qianjiapp.com/", "钱迹，一款简洁纯粹的记账 App，是一个 “无广告、无开屏、无理财” 的 “三无” 产品，力求极简，专注个人记账，将每一笔收支都清晰记录，消费及资产随时了然于心。"),
    )

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogAppBinding.inflate(inflater)

        cardView = binding.cardView
        cardViewInner = binding.innerView

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = AppListAdapter(context,apps, SpUtils.getString("bookApp")) {
            SpUtils.putString("bookApp", it.packageName)
            LocalBroadcastHelper.sendBroadcast(LocalBroadcastHelper.ACTION_APP_CHANGED)
            dismiss()
        }


        return binding.root
    }

    //监听更新完成广播
    override fun show(float: Boolean, cancel: Boolean) {
        super.show(float, cancel)
    }

    override fun dismiss() {
        super.dismiss()
    }

}
