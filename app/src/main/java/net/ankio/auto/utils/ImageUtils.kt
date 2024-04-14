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

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.utils.request.RequestsUtils

object ImageUtils {
    suspend fun get(
        context: Context,
        uriString: String,
        default: Int,
    ): Drawable =
        withContext(Dispatchers.IO) {
            Logger.i("加载图片：$uriString")
            val result =
                if (uriString.startsWith("data:image")) {
                    getFromBase64(context, uriString)
                } else if (uriString.startsWith("http")) {
                    getFromWeb(context, uriString)
                } else {
                    null
                }
            (result ?: ResourcesCompat.getDrawable(context.resources, default, context.theme))!!
        }

    private fun getFromBase64(
        context: Context,
        base64String: String,
    ): Drawable {
        val base64Image = base64String.substring(base64String.indexOf(",") + 1)
        val decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        return BitmapDrawable(context.resources, decodedByte)
    }

    private suspend fun getFromWeb(
        context: Context,
        uriString: String,
    ): Drawable? =
        withContext(Dispatchers.IO) {
            runCatching {
                val result =
                    RequestsUtils(context).get(
                        url = uriString,
                        cacheTime = 60 * 24 * 180, // 图片缓存180天
                    )
                val bitmap = BitmapFactory.decodeStream(result.byteArray.inputStream())
                BitmapDrawable(null, bitmap)
            }.onFailure {
                Logger.e("加载图片失败：${it.message}", it)
            }.getOrNull()
        }
}
