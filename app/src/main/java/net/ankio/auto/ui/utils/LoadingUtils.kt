/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.utils

import android.app.Activity
import android.app.Dialog
import android.view.View
import androidx.annotation.StringRes
import net.ankio.auto.databinding.DialogLoadingBinding
import net.ankio.auto.utils.Logger

class LoadingUtils(private val activity: Activity) {
    private lateinit var dialog: Dialog
    private lateinit var binding: DialogLoadingBinding

    fun show(
        @StringRes text: Int,
    ) {
        show(activity.getString(text))
    }

    fun show(text: String? = null) {
        if (::dialog.isInitialized && dialog.isShowing) {
            return
        }

        dialog = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.setCancelable(false)

        binding = DialogLoadingBinding.inflate(activity.layoutInflater)
        if (text.isNullOrEmpty()) {
            binding.loadingText.visibility = View.GONE
        } else {
            binding.loadingText.text = text
        }
        dialog.setContentView(binding.getRoot())
        dialog.show()
    }

    fun setText(
        @StringRes text: Int,
    ) {
        setText(activity.getString(text))
    }

    fun setText(text: String?) {
        Logger.d("setText: $text")
        activity.runOnUiThread {
            if (dialog.isShowing) {
                binding.loadingText.text = text
                binding.loadingText.visibility = View.VISIBLE
            }
        }
    }

    fun close() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
}
