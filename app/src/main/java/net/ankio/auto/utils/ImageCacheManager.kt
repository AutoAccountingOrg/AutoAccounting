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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ImageCacheManager private constructor(context: Context) {

    private val cacheDir: File = context.cacheDir // 使用应用的缓存目录

    fun saveImageToCache(key: String, drawable: Drawable) {
        val bitmap = drawableToBitmap(drawable)
        val file = File(cacheDir, getKeyName(key))
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
    }

    fun getKeyName(string: String): String {
        return "images/"+MD5Util.get(string)+".cache_image"
    }

    fun loadImageFromCache(key: String): Drawable? {
        val file = File(cacheDir, getKeyName(key))
        if (file.exists()) {
            val inputStream = FileInputStream(file)
            val bitmap = Bitmap.createBitmap(BitmapFactory.decodeStream(inputStream))
            return BitmapDrawable(null, bitmap)
        }
        return null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        private var instance: ImageCacheManager? = null

        fun getInstance(context: Context): ImageCacheManager {
            return instance ?: synchronized(this) {
                instance ?: ImageCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
