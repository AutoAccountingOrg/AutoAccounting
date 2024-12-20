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

package net.ankio.auto.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import net.ankio.auto.databinding.DialogBottomSheetBinding
import net.ankio.auto.ui.api.BaseSheetDialog

class BottomSheetDialogBuilder(context: Context) : BaseSheetDialog(context) {
    // .setTitle(R.string.add_filter)
    //            .setView(inputBinding.root)
    //            .setPositiveButton(R.string.sure_msg) { dialog, which ->
    //                val input = inputBinding.input.text.toString()
    //                if (input.isNotEmpty()) {
    //                    if (chip != null) {
    //                        chip.text = input
    //                        setChip(text,input)
    //                    } else {
    //                        addChip(input,true)
    //                    }
    //                }
    //            }
    //            .setNegativeButton(R.string.cancel_msg, null)

    val binding = DialogBottomSheetBinding.inflate(LayoutInflater.from(context))

    init {
        binding.title.visibility = View.GONE
        binding.positiveButton.visibility = View.GONE
        binding.negativeButton.visibility = View.GONE
    }

    fun setTitle(title: String): BottomSheetDialogBuilder {
        binding.title.text = title
        binding.title.visibility = View.VISIBLE
        return this
    }

    fun setTitleInt(title: Int): BottomSheetDialogBuilder {
        binding.title.setText(title)
        binding.title.visibility = View.VISIBLE
        return this
    }

    fun setPositiveButton(
        text: Int,
        listener: ((dialog: BaseSheetDialog, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        val t = context.getString(text)
        return setPositiveButton(t, listener)
    }

    fun setPositiveButton(
        text: String,
        listener: ((dialog: BaseSheetDialog, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        binding.positiveButton.text = text
        binding.positiveButton.setOnClickListener {

            if (listener != null)
                listener(this, 0)
            dismiss()
        }
        binding.positiveButton.visibility = View.VISIBLE
        return this
    }

    fun setNegativeButton(
        text: Int,
        listener: ((dialog: BaseSheetDialog, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        val t = context.getString(text)
        return setNegativeButton(t, listener)
    }

    fun setNegativeButton(
        text: String,
        listener: ((dialog: BaseSheetDialog, which: Int) -> Unit)?
    ): BottomSheetDialogBuilder {
        binding.negativeButton.text = text
        binding.negativeButton.setOnClickListener {

            if (listener != null)
                listener(this, 0)
            dismiss()
        }
        binding.negativeButton.visibility = View.VISIBLE
        return this
    }


    fun setView(view: View): BottomSheetDialogBuilder {
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        view.layoutParams = layoutParams
        binding.container.addView(view)
        return this
    }

    override fun onCreateView(inflater: LayoutInflater) = binding.root
    fun setMessage(string: String): BottomSheetDialogBuilder {
        val textView = TextView(context)
        textView.text = string
        textView.setPadding(0, 0, 0, 0)
        textView.textSize = 16f
        setView(textView)
        return this
    }
}