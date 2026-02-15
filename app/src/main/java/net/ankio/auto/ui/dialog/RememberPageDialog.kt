/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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
import net.ankio.auto.R
import net.ankio.auto.service.ocr.PageSignature
import net.ankio.auto.service.ocr.PageSignatureManager
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.ToastUtils

/**
 * 记住页面弹窗
 *
 * 手动 OCR 识别成功后展示，询问用户是否记住当前页面特征。
 * 支持 Service 环境（自动使用悬浮窗模式）。
 */
object RememberPageDialog {

    /**
     * 展示记住页面弹窗
     *
     * @param context 上下文（Activity/Service 均可，Service 时自动悬浮窗）
     * @param packageName 包名
     * @param activityName Activity 类名
     * @param contentFingerprint 内容指纹
     * @param ruleName 匹配规则名
     */
    fun show(
        context: Context,
        packageName: String,
        activityName: String,
        contentFingerprint: String,
    ) {
        if (packageName.isBlank()) return

        val displayName =
            activityName.ifBlank { context.getString(R.string.ocr_remember_page_activity_unknown) }
        val message = context.getString(R.string.ocr_remember_page_message, displayName)

        BaseSheetDialog.create<BottomSheetDialogBuilder>(context)
            .setTitleInt(R.string.ocr_remember_page_title)
            .setMessage(message)
            .setPositiveButton(R.string.ocr_remember_page_confirm) { _, _ ->
                PageSignatureManager.add(
                    PageSignature(
                        packageName = packageName,
                        activityName = activityName,
                        contentFingerprint = contentFingerprint,
                    )
                )
                ToastUtils.info(context.getString(R.string.ocr_remember_page_success))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
