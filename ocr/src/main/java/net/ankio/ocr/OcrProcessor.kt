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
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

open class OcrProcessor : Closeable {

    /**
     * OCR处理器 - 线程安全的文字识别封装
     *
     * 关键设计：每次识别重建 OcrEngine，避免状态污染
     * OcrEngine 的 doAngle 功能会修改内部状态，重用实例会导致乱码
     */
    private var engine: OcrEngine? = null
    /**
     * 应用上下文，用于访问 assets 与缓存目录
     */
    private lateinit var appCtx: Context


    private var debug: Boolean = false

    /**
     * 绑定上下文（会持有 applicationContext 以避免泄露）
     */
    fun attach(context: Context) = apply {
        this.appCtx = context.applicationContext
        if (engine == null) {
            engine = OcrEngine(appCtx)
        }
    }

    /**
     * 释放引擎资源并清空引用，确保可重复调用且不会泄露。
     */
    fun release() {
        // 释放底层 native 资源，防止重复创建后累积占用。
        engine?.closeAndRelease()
        // 清空引用，保证后续使用时重新初始化，避免使用已释放实例。
        engine = null
    }

    fun debug(boolean: Boolean) = apply {
        this.debug = boolean
    }

    private var output: ((string: String, type: Int) -> Unit)? = null
    fun log(output: (string: String, type: Int) -> Unit) = apply {
        this.output = output
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


    /**
     * 执行OCR识别
     * 每次调用创建新引擎实例，避免状态污染导致的乱码问题
     * @param bitmap 输入图片
     * @return 识别的文本
     */
    suspend fun startProcess(bitmap: Bitmap): String {

        // 引擎未初始化时直接抛错，避免继续走到 native 调用
        if (engine == null) {
            throw IllegalStateException("OCR engine is not initialized, call attach() first")
        }

        output?.invoke("OCR开始, 输入尺寸: ${bitmap.width}x${bitmap.height}", Log.DEBUG)


        try {
            val boxImg = createBitmap(bitmap.width, bitmap.height)
            val maxSize = max(bitmap.height, bitmap.width)

            val result = engine!!.detect(bitmap, boxImg, maxSize)

            // 调试模式下保存标注框图片，否则立即回收
            if (debug) {
                saveDebugImage(boxImg)
            }

            boxImg.recycle()
            bitmap.recycle()

            output?.invoke(
                "OCR完成: detectTime=${result.detectTime}ms, dbNetTime=${result.dbNetTime}ms, strResLen=${result.strRes.length}",
                Log.DEBUG
            )

            return result.strRes

        } catch (e: Exception) {
            output?.invoke("OCR异常: ${e.message}", Log.ERROR)
            throw e
        }
    }

    /**
     * 让 Kotlin 的 use 自动调用释放逻辑，保证作用域结束后自动关闭。
     */
    override fun close() {
        // 统一走 release，保持关闭行为一致且可复用。
        release()
    }


}