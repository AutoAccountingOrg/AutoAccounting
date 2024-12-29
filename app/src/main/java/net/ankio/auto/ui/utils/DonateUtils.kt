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

import android.content.Context
import android.content.Intent
import android.net.Uri
import net.ankio.auto.R
import net.ankio.auto.utils.CustomTabsHelper

object DonateUtils {
    fun wechat(context: Context) {
        val uri =
            "https://pic.dreamn.cn/uPic/2023_04_23_00_41_49_1682181709_1682181709722_KGWAI6.jpg"
        CustomTabsHelper.launchUrlOrCopy(context, uri)
        ToastUtils.info(R.string.copy_donate_qr)
    }

    fun alipay(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://qr.alipay.com/fkx15657xcegbz5k9zxnd30")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}