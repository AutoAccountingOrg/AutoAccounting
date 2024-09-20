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
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.broadcast.LocalBroadcastHelper
import net.ankio.auto.databinding.DialogUpdateBinding
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.update.BaseUpdate
import net.ankio.auto.update.RuleUpdate
import rikka.html.text.toHtml

class UpdateDialog(
    private val context: Activity,
    private val baseUpdate: BaseUpdate
) : BaseSheetDialog(context) {
    private lateinit var binding: DialogUpdateBinding

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogUpdateBinding.inflate(inflater)

        cardView = binding.cardView
        cardViewInner = binding.cardViewInner

        binding.version.text = baseUpdate.version
        binding.updateInfo.text = baseUpdate.log.toHtml()
        binding.date.text = baseUpdate.date
        binding.name.text =
            if (baseUpdate is RuleUpdate) context.getString(R.string.rule) else context.getString(R.string.app)
        binding.update.setOnClickListener {
            lifecycleScope.launch {
                baseUpdate.update(context)
            }
        }

        //  EventBus.register(UpdateFinishEvent::class.java, listener)

        return binding.root
    }

    private lateinit var broadcastReceiver: BroadcastReceiver

    //监听更新完成广播
    override fun show(float: Boolean, cancel: Boolean) {
        super.show(float, cancel)
        broadcastReceiver =
            LocalBroadcastHelper.registerReceiver(LocalBroadcastHelper.ACTION_UPDATE_FINISH) { _, _ ->
                dismiss()
            }
    }

    override fun dismiss() {
        super.dismiss()
        if (this::broadcastReceiver.isInitialized) LocalBroadcastHelper.unregisterReceiver(
            broadcastReceiver
        )
    }

}
