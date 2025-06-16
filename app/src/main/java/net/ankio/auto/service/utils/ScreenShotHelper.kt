package net.ankio.auto.service.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.ankio.auto.storage.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 基于协程的屏幕截图工具
 */
class ScreenShotHelper(
    ctx: Context,
    private val projection: MediaProjection
) {
    private val busy = AtomicBoolean(false)

    // 确保宽高为偶数（编码器硬性要求）
    private val metrics = ctx.resources.displayMetrics
    private val width = metrics.widthPixels
    private val height = metrics.heightPixels
    private val dpi = metrics.densityDpi

    // 双缓冲 ImageReader
    private val imageReader = ImageReader.newInstance(
        width,
        height,
        PixelFormat.RGBA_8888,
        2
    )
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null

    init {
        createVirtualDisplay()
    }

    private fun createVirtualDisplay() {
        virtualDisplay = projection.createVirtualDisplay(
            "screen-mirror",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )
    }

    /**
     * 截图并返回 Bitmap，若失败则抛出异常。
     */
    suspend fun capture(): Bitmap? = withContext(Dispatchers.IO) {
        if (!busy.compareAndSet(false, true)) {
            Logger.d("截图操作正在进行中")
            return@withContext null
        }

        try {
            if (virtualDisplay == null) createVirtualDisplay()
            // 等待图像，超时500ms
            val image = acquireImageWithTimeout(5000)
            busy.set(false)
            return@withContext processImage(image)
        } catch (e: Exception) {
            busy.set(false)
            Logger.e("异常：$e", e)
        }
        return@withContext null
    }

    /**
     * 在指定超时时间内获取最新屏幕图像。
     */
    private suspend fun acquireImageWithTimeout(timeoutMillis: Long): Image =
        withTimeout(timeoutMillis) {
            while (true) {
                val img = imageReader.acquireLatestImage()
                if (img != null) {
                    return@withTimeout img // 这里一定要用 return@withTimeout
                }
                delay(100)
            }
            // 理论上不会走到这里，但加一行防止编译器报错
            throw IllegalStateException("Timeout waiting for image")
        }


    /**
     * 解析 Image 为 Bitmap。
     */
    private suspend fun processImage(image: Image): Bitmap =
        withContext(Dispatchers.IO) {
            val width = image.width
            val height = image.height
            val planes = image.planes
            val buffer = planes[0].buffer

            //每个像素的间距
            val pixelStride = planes[0].pixelStride

            //总的间距
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width
            val bitmap =
                createBitmap(width + rowPadding / pixelStride, height) //虽然这个色彩比较费内存但是 兼容性更好
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            bitmap
        }

    /**
     * 释放相关资源。
     */
    fun release() {
        virtualDisplay?.release()
        imageReader.close()
        projection.stop()
        virtualDisplay = null
    }
}
