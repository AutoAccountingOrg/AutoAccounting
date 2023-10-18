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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ankio.auto.R
import net.ankio.auto.databinding.UpdateDialogBinding
import net.ankio.auto.utils.UpdateInfo
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import rikka.html.text.HtmlCompat


class UpdateDialog(private val updateInfo: UpdateInfo): BottomSheetDialogFragment() {


    fun show(context: Activity,float: Boolean){
        // 创建 BottomSheetDialogFragment
        val dialogFragment = this

        if(float){
            dialog?.window?.setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }
        // 显示 BottomSheetDialogFragment
        dialogFragment.show((context as AppCompatActivity).supportFragmentManager, dialogFragment.tag)

    }
    private lateinit var binding:UpdateDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =  UpdateDialogBinding.inflate(inflater)

        val data = "# ${updateInfo.name} \n ### ⏰ ${updateInfo.date}\n"+updateInfo.log
        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(data)
        val html = HtmlGenerator(data, parsedTree, flavour).generateHtml()

        binding.updateInfo.text = HtmlCompat.fromHtml(html)


        binding.update.setOnClickListener {
            if(updateInfo.type=="Rule"){
                dismiss()
                return@setOnClickListener
            }
            val  intent  =  Intent(Intent.ACTION_VIEW,  Uri.parse(updateInfo.downloadUrl[0]))
            context?.startActivity(intent)
            dismiss()
        }


        binding.version.text = if(updateInfo.type=="Rule")getString(R.string.rule_update) else getString(R.string.app_update)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 禁止用户通过下滑手势关闭对话框
        dialog?.setCancelable(false)

        // 允许用户通过点击空白处关闭对话框
        dialog?.setCanceledOnTouchOutside(true)

        // Get the display metrics using the DisplayMetrics directly
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        // Calculate and set dialog height as a percentage of screen height
        val dialogHeight = (screenHeight * 0.7).toInt() // 50% height
        view.layoutParams.height = dialogHeight
    }

}