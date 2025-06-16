package net.ankio.auto.service.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import net.ankio.auto.storage.Logger

object OcrProcessor {

    suspend fun recognize(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        Logger.d("截屏结果识别中....")
        val client = TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        )
        return@withContext try {
            client.process(InputImage.fromBitmap(bitmap, 0))
                .await()
                .text
        } catch (e: Exception) {
            Logger.e("OCR error: ${e.message}", e)
            ""
        } finally {
            client.close()
        }
    }
}
