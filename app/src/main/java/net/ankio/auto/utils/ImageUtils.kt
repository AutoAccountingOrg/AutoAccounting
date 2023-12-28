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
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import net.ankio.auto.R
import net.ankio.auto.exceptions.ImageLoadException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream


object ImageUtils {
    private var error = false
    fun init() {
        error = false
    }

    fun get(
        context: Context,
        uriString: String,
        callback: (Drawable) -> Unit,
        error: (String) -> Unit
    ) {
        val cache = ImageCacheManager.getInstance(context).loadImageFromCache(uriString)
        if(cache!=null){
            callback(cache)
            return
        }
        val uri = Uri.parse(uriString)
        if (isHttpUri(uri)) {
            getFromWeb(context,uriString, callback, error)
        } else if (isContentProviderUri(uri)) {
            getFromContentProvider(context, uriString, callback, error)
        }else{
            error("不支持的图片链接")
        }
    }

    private fun getFromContentProvider(
        context: Context,
        uriString: String,
        callback: (Drawable) -> Unit,
        error_: (String) -> Unit
    ) {

        runCatching {
            val contentResolver = context.contentResolver
            val uri = Uri.parse(uriString)
            var inputStream: InputStream? = null
            runCatching {
                inputStream = contentResolver.openInputStream(uri)
            }.onFailure {
                runCatching {
                    val intent = Intent("net.ankio.auto.ACTION_WAKE_UP")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }.onFailure {
                    throw ImageLoadException(context.getString(R.string.image_provider_error))
                }

            }

            if (inputStream == null) {
                inputStream = contentResolver.openInputStream(uri)
            }

            if (inputStream != null) {
                runCatching {
                    Drawable.createFromStream(inputStream, uriString)?.let { drawable ->
                        ImageCacheManager.getInstance(context).saveImageToCache(uriString, drawable)
                        callback(drawable)
                    }?: throw ImageLoadException(context.getString(R.string.image_provider_error))
                }.onFailure {
                    throw ImageLoadException(context.getString(R.string.image_provider_error))
                }
            } else {
                throw ImageLoadException(context.getString(R.string.image_provider_error))
            }
        }.onFailure {
            if(!error){
                error = true
                it.message?.let { it1 -> error_(it1) }
            }
        }


    }

    private fun getFromWeb(context: Context,uriString: String, callback: (Drawable) -> Unit,error_: (String) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(uriString)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val inputStream: InputStream? = response.body?.byteStream()
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val drawable = BitmapDrawable(null, bitmap)
                    ImageCacheManager.getInstance(context).saveImageToCache(uriString,drawable)
                    callback(drawable)
                    return
                }
            }
            throw ImageLoadException(context.getString(R.string.image_web_error))
        } catch (e: Exception) {
            if(!error){
                error = true
                e.message?.let { it1 -> error_(it1) }
            }
        }
    }

    private fun isHttpUri(uri: Uri): Boolean {
        val scheme = uri.scheme
        return scheme == "http" || scheme == "https"
    }

    private fun isContentProviderUri(uri: Uri): Boolean {
        val scheme = uri.scheme
        return scheme == "content"
    }

}
