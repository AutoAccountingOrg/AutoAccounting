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

import android.view.View
import com.google.gson.Gson
import com.google.gson.JsonElement
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogDataEditorBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.ToastUtils

class DataEditorDialog(
    private val activity: BaseActivity,
    private val data: String,
    private val callback: (result: String) -> Unit,
) :
    BaseSheetDialog<DialogDataEditorBinding>(activity) {

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        binding.btnConfirm.setOnClickListener {
            callback(binding.etContent.text.toString())
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.etContent.setText(data)

        binding.btnReplace.setOnClickListener {
            val keyword = binding.etRaw.text.toString()
            val replaceData = binding.etTarget.text.toString()
            val editorData = binding.etContent.text.toString()

            if (keyword.isEmpty() || replaceData.isEmpty()) {
                ToastUtils.error(R.string.no_empty)
                return@setOnClickListener
            }

            if (!editorData.contains(keyword)) {
                ToastUtils.error(R.string.no_replace)
                return@setOnClickListener
            }
            binding.etContent.setText(editorData.replace(keyword, replaceData))
        }

        binding.btnMaskAll.setOnClickListener {
            val result = DesensitizerRegistry.maskAll(binding.etContent.text.toString())
            BottomSheetDialogBuilder(activity)
                .setTitle(activity.getString(R.string.replace_result))
                .setMessage(result.changes.joinToString(separator = "\n") { (from, to) -> "\"$from\" → \"$to\"" })
                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    binding.etContent.setText(result.masked)
                }.setNegativeButton(R.string.btn_cancel) { _, _ ->

                }.show()

        }

    }
}


/**
 * 脱敏结果：替换后的文本 + 替换日志
 */
data class DesensitizeResult(
    val masked: String,
    val changes: List<Pair<String, String>> // 原值 → 占位值
)

/**
 * 单条策略
 */
interface Desensitizer {
    fun mask(input: CharSequence, log: MutableList<Pair<String, String>>): CharSequence
}

/**
 * 基于正则 + 固定占位值的策略
 */
class RegexDesensitizer(
    private val pattern: Regex,
    private val placeholder: String
) : Desensitizer {

    override fun mask(input: CharSequence, log: MutableList<Pair<String, String>>): CharSequence =
        pattern.replace(input) { mr ->
            log += mr.value to placeholder
            placeholder
        }
}

/**
 * 全局注册表 —— 一键脱敏
 */
object DesensitizerRegistry {

    private val delegates = mutableListOf<Desensitizer>()

    init {
        // 1️⃣ 手机号
        register(RegexDesensitizer(Regex("\\b1[3-9]\\d{9}\\b"), "13800000000"))

        // 2️⃣ 邮箱
        register(
            RegexDesensitizer(
                Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}"),
                "example@example.com"
            )
        )

        // 3️⃣ 身份证号
        register(
            RegexDesensitizer(
                Regex("\\b\\d{6}(19|20)?\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]\\b"),
                "110101199001011234"
            )
        )

        // 4️⃣ 整串银行卡号
        register(RegexDesensitizer(Regex("\\b\\d{16,19}\\b"), "6222000000000000000"))


        // 6️⃣ 括号包裹尾号 4 位
        register(
            RegexDesensitizer(
                Regex("(?<=[(（])\\d{4}(?=[)）])"),
                "0000"
            )
        )

        // 7️⃣ IPv4
        register(
            RegexDesensitizer(
                Regex("(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}"),
                "192.0.2.1"
            )
        )

        // 8️⃣ 中文姓名
        register(RegexDesensitizer(Regex("\\b[\\u4E00-\\u9FA5]{2,4}\\b"), "张三"))

        // 9️⃣ 护照
        register(RegexDesensitizer(Regex("\\b[EGPSeqg]\\d{8}\\b"), "E12345678"))

        // 🔟 港澳台通行证
        register(RegexDesensitizer(Regex("\\b[HMhm]\\d{8,10}\\b"), "H123456789"))

        // 11️⃣ 支付金额
        register(
            RegexDesensitizer(
                Regex("(?<=\\b)(¥|￥)?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?\\b"),
                "100.00"
            )
        )
    }

    fun register(desensitizer: Desensitizer) {
        delegates += desensitizer
    }

    /**
     * 返回替换后的文本 + 替换日志
     */
    fun maskAll(src: String): DesensitizeResult {
        val log = mutableListOf<Pair<String, String>>()
        val masked = delegates.fold(src) { acc, d -> d.mask(acc, log).toString() }
        return DesensitizeResult(masked, log)
    }
}

