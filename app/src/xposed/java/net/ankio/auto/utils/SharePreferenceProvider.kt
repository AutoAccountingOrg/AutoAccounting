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

package net.ankio.auto.utils

import com.crossbowffs.remotepreferences.RemotePreferenceProvider
//TODO 手动添加好像有点呆，全写到一个文件感觉又不合适
class SharePreferenceProvider:  RemotePreferenceProvider(
    "net.ankio.auto.utils.SharePreferenceProvider",
    arrayListOf(
        "net.ankio.auto.xposed",
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.eg.android.AlipayGphone",
        "com.android.phone",
        "com.synjones.xuepay.sdu",
    ).map { "AutoAccounting.$it" }.toTypedArray()
) {
}