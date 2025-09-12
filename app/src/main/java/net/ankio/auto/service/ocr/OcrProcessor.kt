package net.ankio.auto.service.ocr

import android.content.Context
import android.graphics.Bitmap
import com.benjaminwan.ocrlibrary.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.storage.Logger
import io.github.oshai.kotlinlogging.KotlinLogging

class OcrProcessor(private val context: Context) {

    private val logger = KotlinLogging.logger(this::class.java.name)

    private lateinit var ocrEngine: OcrEngine


    private suspend fun ensureOcrReady() = withContext(Dispatchers.IO) {
        if (!::ocrEngine.isInitialized) {
            // 这里做一次初始化
            ocrEngine = OcrEngine(context)
            logger.debug { "OcrLite OCR引擎初始化完成" }
        }
    }
    /**
     * 识别图片中的文字
     * @param bitmap 需要识别的图片
     * @return 识别出的文字内容，如果识别失败则返回空字符串
     */
    suspend fun recognize(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val recognizeStartTime = System.currentTimeMillis()
        logger.debug { "OCR 开始识别..." }
        ensureOcrReady()
        return@withContext try {
            val result = ocrEngine.detect(bitmap, bitmap, 0)
            val text = result.textBlocks.joinToString("\n") { it.text }
            val totalRecognizeTime = System.currentTimeMillis() - recognizeStartTime
            logger.debug { "OCR识别耗时: ${totalRecognizeTime}ms" }
            text
        } catch (e: Exception) {
            logger.error(e) { "OCR识别失败: ${e.message}" }
            ""
        }
    }


}
