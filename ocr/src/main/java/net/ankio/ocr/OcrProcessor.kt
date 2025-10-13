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
import java.io.File
import java.io.FileOutputStream
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

    /**
     * 保存调试图片到缓存目录
     * @param bitmap 要保存的图片
     */
    private fun saveDebugImage(bitmap: Bitmap) {
        val dir = File(appCtx.cacheDir, "images_ocr")
        dir.mkdirs()
        val fileName = "ocr_${System.currentTimeMillis()}.png"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        output?.invoke("Debug image saved: ${file.absolutePath}", Log.DEBUG)

        // 清理旧文件，保持最多 100 张图片
        cleanupOldImages(dir, maxCount = 100)
    }

    /**
     * 清理目录下的旧文件，保持文件数量不超过指定限制
     * @param dir 目标目录
     * @param maxCount 最大文件数量
     */
    private fun cleanupOldImages(dir: File, maxCount: Int) {
        val files = dir.listFiles() ?: return
        if (files.size <= maxCount) return

        // 按修改时间排序，最旧的在前
        files.sortBy { it.lastModified() }

        // 删除超出限制的旧文件
        val deleteCount = files.size - maxCount
        files.take(deleteCount).forEach { file ->
            if (file.delete()) {
                output?.invoke("Deleted old debug image: ${file.name}", Log.DEBUG)
            }
        }
    }


    suspend fun startProcess(bitmap: Bitmap): String {
        ensureInit()
        output?.invoke("ocr process start", Log.DEBUG)
        val boxImg = createBitmap(bitmap.width, bitmap.height)
        val maxSize = max(bitmap.height, bitmap.width)
        val result = ocrEngine.detect(bitmap, boxImg, maxSize)

        // 调试模式下保存标注框图片
        if (debug) {
            saveDebugImage(boxImg)
        }

        output?.invoke(
            "ocr process detectTime : ${result.detectTime}ms, dbNetTime : ${result.dbNetTime}ms",
            Log.DEBUG
        )

        return result.strRes
    }


}