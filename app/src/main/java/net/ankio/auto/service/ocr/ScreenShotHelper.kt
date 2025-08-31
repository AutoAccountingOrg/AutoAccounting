package net.ankio.auto.service.ocr

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
 *
 * 该类提供了使用 MediaProjection API 进行屏幕截图的功能。主要特点：
 * - 使用协程进行异步操作
 * - 支持双缓冲的 ImageReader
 * - 自动处理图像格式转换
 * - 线程安全的截图操作
 *
 * @param ctx 应用上下文，用于获取屏幕参数
 * @param projection MediaProjection 实例，用于创建虚拟显示
 */
class ScreenShotHelper(
    ctx: Context,
    private val projection: MediaProjection
) {
    /** 用于确保同一时间只有一个截图操作在进行 */
    private val busy = AtomicBoolean(false)

    // 确保宽高为偶数（编码器硬性要求）
    private val metrics = ctx.resources.displayMetrics
    private val width = metrics.widthPixels
    private val height = metrics.heightPixels
    private val dpi = metrics.densityDpi

    /** 双缓冲 ImageReader，用于接收屏幕图像数据 */
    private val imageReader = ImageReader.newInstance(
        width,
        height,
        PixelFormat.RGBA_8888,
        2
    )

    /** 虚拟显示实例，用于捕获屏幕内容 */
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null

    init {
        createVirtualDisplay()
    }

    /**
     * 创建虚拟显示实例
     *
     * 使用 MediaProjection 创建虚拟显示，用于捕获屏幕内容。
     * 虚拟显示会将屏幕内容输出到 ImageReader 的 surface。
     */
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
     * 执行屏幕截图操作
     *
     * 该方法会异步捕获当前屏幕内容并转换为 Bitmap。
     * 如果已有截图操作正在进行，则返回 null。
     *
     * @return 包含屏幕内容的 Bitmap，如果截图失败则返回 null
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
            val result = processImage(image)
            // 每次截图后主动释放 VirtualDisplay，避免长时间持有导致 Surface/BLAST 队列失效
            runCatching { virtualDisplay?.release() }.onFailure { Logger.w("释放VirtualDisplay失败: ${it.message}") }
            virtualDisplay = null
            busy.set(false)
            return@withContext result
        } catch (e: Exception) {
            busy.set(false)
            Logger.e("异常：$e", e)
        }
        return@withContext null
    }

    /**
     * 在指定超时时间内获取最新屏幕图像
     *
     * 该方法会持续尝试获取最新的屏幕图像，直到成功或超时。
     *
     * @param timeoutMillis 超时时间（毫秒）
     * @return 捕获到的屏幕图像
     * @throws IllegalStateException 如果超时仍未获取到图像
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
     * 将 Image 对象转换为 Bitmap
     *
     * 处理图像数据，包括处理像素步长和行步长，确保正确转换图像格式。
     *
     * @param image 要处理的 Image 对象
     * @return 转换后的 Bitmap 对象
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
     * 释放所有相关资源
     *
     * 释放虚拟显示、ImageReader 和 MediaProjection 实例。
     * 在不再需要截图功能时调用此方法。
     */
    fun release() {
        virtualDisplay?.release()
        imageReader.close()
        projection.stop()
        virtualDisplay = null
    }
}
