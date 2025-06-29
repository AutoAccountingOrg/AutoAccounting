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
import android.app.Service
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogUpdateBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.update.RuleUpdate
import net.ankio.auto.utils.UpdateModel
import rikka.html.text.toHtml

class UpdateDialog
/**
 * 使用Activity上下文构造底部弹窗构建器
 * @param activity Activity实例
 */(private var updateModel: UpdateModel, activity: Activity) :
    BaseSheetDialog<DialogUpdateBinding>(activity) {


    lateinit var onClickUpdate: () -> Unit

    fun setOnClickUpdate(onClickUpdate: () -> Unit): UpdateDialog {
        this.onClickUpdate = onClickUpdate
        return this
    }

    private lateinit var ruleTitle: String

    fun setRuleTitle(title: String): UpdateDialog {
        ruleTitle = title
        return this
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        binding.version.text = updateModel.version
        binding.updateInfo.text = updateModel.log.toHtml()
        binding.date.text = updateModel.date
        if (::ruleTitle.isInitialized) {
            binding.name.text = ruleTitle
        }
        //  binding.name.text =  if (updateModel is RuleUpdate) context.getString(R.string.rule) else context.getString(R.string.app)
        binding.update.setOnClickListener {
            if (::onClickUpdate.isInitialized) {
                onClickUpdate()
            }
            dismiss()
        }
    }

    init {
        Logger.d("BottomSheetDialogBuilder created with Activity: ${activity.javaClass.simpleName}")
    }

}

