package net.ankio.auto.service.ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.ankio.auto.storage.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 屏幕截图服务 - 统一管理屏幕录制权限和截图功能
 *
 * 设计原则：
 * 1. 单一职责：同时处理权限管理和截图操作，避免职责分散
 * 2. 简洁API：提供最简单的使用接口
 * 3. 资源安全：确保MediaProjection和相关资源正确释放
 */
object ScreenCapture {

    /**
     * 屏幕录制状态
     */
    private sealed class ProjectionState {
        /** 未授权 */
        object NotReady : ProjectionState()

        /** 已授权待创建 */
        data class Authorized(val resultData: Intent) : ProjectionState()

        /** 可用状态 */
        data class Ready(val projection: MediaProjection) : ProjectionState()
    }

    /** 当前投影状态 */
    @Volatile
    private var projectionState: ProjectionState = ProjectionState.NotReady

    /* ------------------ 权限管理 API ------------------ */

    /**
     * 检查屏幕录制权限状态
     */
    fun isReady(): Boolean = projectionState !is ProjectionState.NotReady

    /**
     * 注册屏幕录制权限请求
     *
     * @param caller Activity或Fragment实例
     * @param onReady 权限授予成功回调
     * @param onDenied 权限被拒绝回调
     * @return ActivityResultLauncher实例
     */
    fun registerPermission(
        caller: ActivityResultCaller,
        onReady: () -> Unit,
        onDenied: () -> Unit
    ): ActivityResultLauncher<Unit> {

        val contract = object : ActivityResultContract<Unit, Intent?>() {
            override fun createIntent(context: Context, input: Unit): Intent {
                val manager =
                    context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Android 14+: 使用新API，失败则降级
                    try {
                        val config = MediaProjectionConfig.createConfigForDefaultDisplay()
                        manager.createScreenCaptureIntent(config)
                    } catch (e: Exception) {
                        Logger.w("新API创建Intent失败，降级: ${e.message}")
                        manager.createScreenCaptureIntent()
                    }
                } else {
                    manager.createScreenCaptureIntent()
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Intent? =
                if (resultCode == Activity.RESULT_OK) intent else null
        }

        return caller.registerForActivityResult(contract) { data ->
            if (data == null) {
                Logger.e("用户拒绝了屏幕录制权限")
                updateState(ProjectionState.NotReady)
                onDenied()
            } else {
                Logger.i("屏幕录制权限已授予")
                updateState(ProjectionState.Authorized(data))
                onReady()
            }
        }
    }

    /* ------------------ 截图功能 API ------------------ */

    /**
     * 执行屏幕截图
     *
     * @param context 上下文，用于获取屏幕参数和创建MediaProjection
     * @return 截图Bitmap，失败时返回null
     */
    suspend fun captureScreen(context: Context): Bitmap? {
        val projection = getOrCreateProjection(context) ?: return null

        return CaptureHelper(context, projection).use { helper ->
            helper.capture()
        }
    }

    /**
     * 释放所有资源
     */
    fun release() {
        val currentState = projectionState
        if (currentState is ProjectionState.Ready) {
            currentState.projection.stop()
        }
        updateState(ProjectionState.NotReady)
        Logger.d("ScreenCapture资源已释放")
    }

    /* ------------------ 内部实现 ------------------ */

    /**
     * 获取或创建MediaProjection实例
     */
    private fun getOrCreateProjection(context: Context): MediaProjection? =
        when (val state = projectionState) {
            is ProjectionState.Ready -> state.projection
            is ProjectionState.Authorized -> createProjection(context, state.resultData)
            is ProjectionState.NotReady -> {
                Logger.e("屏幕录制权限未授予")
                null
            }
        }

    /**
     * 线程安全的状态更新
     */
    @Synchronized
    private fun updateState(newState: ProjectionState) {
        projectionState = newState
    }

    /**
     * 创建MediaProjection实例
     */
    @Synchronized
    private fun createProjection(context: Context, resultData: Intent): MediaProjection? {
        // 双重检查：避免重复创建
        val currentState = projectionState
        if (currentState is ProjectionState.Ready) {
            return currentState.projection
        }

        return try {
            val manager =
                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(Activity.RESULT_OK, resultData)

            updateState(ProjectionState.Ready(projection))
            Logger.i("MediaProjection创建成功")
            projection
        } catch (e: Exception) {
            Logger.e("MediaProjection创建失败: ${e.message}")
            updateState(ProjectionState.NotReady)
            null
        }
    }

    /**
     * 内部截图助手类
     * 负责具体的截图实现，使用完自动释放资源
     */
    private class CaptureHelper(
        context: Context,
        private val projection: MediaProjection
    ) {
        private val busy = AtomicBoolean(false)

        // 屏幕参数
        private val metrics = context.resources.displayMetrics
        private val width = metrics.widthPixels
        private val height = metrics.heightPixels
        private val dpi = metrics.densityDpi

        // 图像读取器
        private val imageReader = ImageReader.newInstance(
            width, height, PixelFormat.RGBA_8888, 2
        )

        // 虚拟显示
        private var virtualDisplay: android.hardware.display.VirtualDisplay? = null

        /**
         * 执行截图操作
         */
        suspend fun capture(): Bitmap? = withContext(Dispatchers.IO) {
            if (!busy.compareAndSet(false, true)) {
                Logger.d("截图操作正在进行中")
                return@withContext null
            }

            try {
                createVirtualDisplay()
                val image = acquireImageWithTimeout(5000)
                return@withContext processImage(image)
            } catch (e: Exception) {
                Logger.e("截图失败: ${e.message}", e)
                return@withContext null
            } finally {
                busy.set(false)
                release()
            }
        }

        /**
         * 创建虚拟显示
         */
        private fun createVirtualDisplay() {
            virtualDisplay = projection.createVirtualDisplay(
                "screen-capture",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null, null
            )
        }

        /**
         * 在超时时间内获取图像
         */
        private suspend fun acquireImageWithTimeout(timeoutMillis: Long): Image =
            withTimeout(timeoutMillis) {
                while (true) {
                    val image = imageReader.acquireLatestImage()
                    if (image != null) {
                        return@withTimeout image
                    }
                    delay(100)
                }
                throw IllegalStateException("获取图像超时")
            }

        /**
         * 将Image转换为Bitmap
         */
        private suspend fun processImage(image: Image): Bitmap = withContext(Dispatchers.IO) {
            val width = image.width
            val height = image.height
            val planes = image.planes
            val buffer = planes[0].buffer

            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = createBitmap(width + rowPadding / pixelStride, height)
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            bitmap
        }

        /**
         * 释放截图相关资源
         */
        private fun release() {
            virtualDisplay?.release()
            imageReader.close()
            virtualDisplay = null
        }

        /**
         * 实现AutoCloseable，确保资源释放
         */
        inline fun <R> use(block: (CaptureHelper) -> R): R {
            return try {
                block(this)
            } finally {
                release()
            }
        }
    }
}
