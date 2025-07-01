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
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogDataEditorBinding
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
            val result = Desensitizer.maskAll(binding.etContent.text.toString())
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
data class DesensitizeResult(
    val masked: String,
    val changes: List<Pair<String, String>>
)

private data class Rule(
    val regex: Regex,
    val replacer: (MatchResult) -> String
)

object Desensitizer {

    /** 数字 → 0；其余字符照抄，保持格式和长度 */
    private val zeroDigits: (MatchResult) -> String = { mr ->
        buildString {
            for (ch in mr.value) append(if (ch.isDigit()) '0' else ch)
        }
    }

    /** 仅当出现货币符号 / 单位时才匹配金额 */
    private val amountRegex =
        "(?xi)(?: [¥￥€]\\s*\\d+(?:,\\d{3})*(?:\\.\\d{1,2})? | \\d+(?:,\\d{3})*(?:\\.\\d{1,2})?\\s*(?:元|块|人民币|美元|USD|CNY|EUR) )".toRegex()


    private val rules = java.util.concurrent.CopyOnWriteArrayList(
        listOf(
            Rule("\\b1[3-9]\\d{9}\\b".toRegex()) { "13800000000" },              // 手机号
            Rule("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}".toRegex()) { "example@example.com" }, // 邮箱
            Rule("\\b\\d{6}(19|20)?\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]\\b".toRegex()) { "110101199001011234" }, // 身份证
            Rule("\\b\\d{16,19}\\b".toRegex()) { "6222000000000000000" },        // 银行卡
            Rule("(?<=[(（])\\d{4}(?=[)）])".toRegex()) { "0000" },               // (1234)
            Rule("(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}".toRegex()) { "192.0.2.1" }, // IPv4
            Rule("\\b[\\u4E00-\\u9FA5]{2,4}\\b".toRegex()) { "张三" },           // 中文姓名
            Rule("\\b[EGPSeqg]\\d{8}\\b".toRegex()) { "E12345678" },             // 护照
            Rule("\\b[HMhm]\\d{8,10}\\b".toRegex()) { "H123456789" },            // 港澳台通行证
            Rule(amountRegex, zeroDigits)                                        // 金额（改进版）
        )
    )

    /** 动态增加自定义规则（注意 replacer 返回占位值） */
    fun register(regex: Regex, replacer: (MatchResult) -> String) {
        rules += Rule(regex, replacer)
    }

    /** 主入口：只收 String，返回脱敏结果 */
    fun maskAll(src: String): DesensitizeResult {
        var out: String = src
        val log = mutableListOf<Pair<String, String>>()

        for (rule in rules) {
            out = rule.regex.replace(out) { mr ->
                val repl = rule.replacer(mr)
                log += mr.value to repl
                repl
            }
        }
        return DesensitizeResult(out, log)
    }
}

