package net.ankio.auto.service.ocr

import android.content.Context
import android.graphics.Bitmap
import com.benjaminwan.ocrlibrary.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.storage.Logger

class OcrProcessor(private val context: Context) {

    private lateinit var ocrEngine: OcrEngine


    private suspend fun ensureOcrReady() = withContext(Dispatchers.IO) {
        if (!::ocrEngine.isInitialized) {
            // 这里做一次初始化
            ocrEngine = OcrEngine(context)
            // 如有其它参数设置也写这
            Logger.d("OcrLite OCR引擎初始化完成")
        }
    }
    /**
     * 识别图片中的文字
     * @param bitmap 需要识别的图片
     * @return 识别出的文字内容，如果识别失败则返回空字符串
     */
    suspend fun recognize(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val recognizeStartTime = System.currentTimeMillis()
        Logger.d("OCR 开始识别...")
        ensureOcrReady()
        return@withContext try {
            // 确保OCR引擎已初始化


            val engine = ocrEngine ?: throw IllegalStateException("OCR引擎初始化失败")
            val result = engine.detect(bitmap, bitmap, 0)
            val text = result.textBlocks.joinToString("\n") { it.text }
            val totalRecognizeTime = System.currentTimeMillis() - recognizeStartTime
            Logger.d("OCR识别耗时: ${totalRecognizeTime}ms")
            text
        } catch (e: Exception) {
            Logger.e("OCR识别失败: ${e.message}", e)
            ""
        }
    }


}
