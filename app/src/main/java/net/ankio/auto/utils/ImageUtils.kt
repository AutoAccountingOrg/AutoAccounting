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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//TODO 协程优化

object ImageUtils {

   suspend fun get(
        context: Context,
        uriString: String,
        callback: (Drawable) -> Unit,
        error: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        Logger.i("加载图片：${uriString}")
        if (uriString.startsWith("data:image")) {
            val drawable = getFromBase64(context, uriString)
            withContext(Dispatchers.Main) {
                callback(drawable)
            }
        } else if (uriString.startsWith("http")) {
            withContext(Dispatchers.Main) {
                getFromWeb(context, uriString, callback, error)
            }
        } else {
            error("不支持的图片链接")
        }
    }

    private fun getFromBase64(context: Context, base64String: String): Drawable {
        val base64Image = base64String.substring(base64String.indexOf(",") + 1)
        val decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        return BitmapDrawable(context.resources, decodedByte)
    }


    private fun getFromWeb(
        context: Context,
        uriString: String,
        callback: (Drawable) -> Unit,
        error: (String) -> Unit
    ) {
        RequestsUtils(context).get(
            url = uriString,
            onSuccess = { byteArray, _ ->
                val bitmap = BitmapFactory.decodeStream(byteArray.inputStream())
                val drawable = BitmapDrawable(null, bitmap)
                callback(drawable)
            },
            onError = error,
            cacheTime = 60 * 24 * 180,//图片缓存180天
        )
    }

}
