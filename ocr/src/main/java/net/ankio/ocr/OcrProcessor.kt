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

package net.ankio.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.createBitmap
import com.benjaminwan.ocrlibrary.OcrEngine
import kotlin.math.max

open class OcrProcessor {
    lateinit var ocrEngine: OcrEngine
    /**
     * 应用上下文，用于访问 assets 与缓存目录。
     */
    private lateinit var appCtx: Context

    private var debug: Boolean = false

    /**
     * 绑定上下文（会持有 applicationContext 以避免泄露）。
     */
    fun attach(context: Context) = apply {
        this.appCtx = context.applicationContext
    }

    fun debug(boolean: Boolean) = apply {
        this.debug = boolean
    }

    private var output: ((string: String, type: Int) -> Unit)? = null
    fun log(output: (string: String, type: Int) -> Unit) = apply {
        this.output = output
    }


    private suspend fun ensureInit() {
        if (::ocrEngine.isInitialized) return
        ocrEngine = OcrEngine(this.appCtx)
        ocrEngine.doAngle = true
    }


    suspend fun startProcess(bitmap: Bitmap): String {
        ensureInit()
        output?.let {
            it("ocr process start", Log.DEBUG)
        }
        val boxImg = createBitmap(bitmap.width, bitmap.height)
        val maxSize = max(bitmap.height, bitmap.width)
        val result = ocrEngine.detect(bitmap, boxImg, maxSize)

        output?.let {
            it(
                "ocr process detectTime : ${result.detectTime}ms, dbNetTime : ${result.dbNetTime}ms",
                Log.DEBUG
            )
        }

        return result.strRes
    }


}