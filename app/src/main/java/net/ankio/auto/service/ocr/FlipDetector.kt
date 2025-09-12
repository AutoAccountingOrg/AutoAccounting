package net.ankio.auto.service.ocr

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import net.ankio.auto.storage.Logger
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 设备翻转检测器：检测设备从朝下翻转到朝上时触发回调
 * 主要用于OCR功能，当用户将手机从朝下翻转到朝上时触发屏幕识别
 *
 * @param manager     SensorManager
 * @param debounceMs  防抖时间间隔（ms），避免频繁触发
 * @param onFlipChange 翻转回调，当检测到从朝下翻转到朝上时调用
 */
class FlipDetector(
    private val manager: SensorManager,
    private val debounceMs: Long = 400L,
    private val onFlipChange: () -> Unit
) : SensorEventListener {

    private val logger = KotlinLogging.logger(this::class.java.name)

    private val sensor: Sensor? = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private enum class Face { UP, DOWN, UNKNOWN }

    private var lastFace = Face.UNKNOWN          // 最近一次稳定面
    private var lastTime = 0L                    // 最近一次稳定时间

    fun start(): Boolean = sensor?.let {
        manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        true
    } ?: false.also { logger.warn { "No gravity/accelerometer sensor" } }

    fun stop() = manager.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        val z = e.values[2]
        val now = SystemClock.uptimeMillis()

        // 当前朝向
        val face = when {
            z > 7f -> Face.UP
            z < -7f -> Face.DOWN
            else -> Face.UNKNOWN
        }

        // 跳过噪声、未变化的状态和防抖时间内的变化
        if (face == Face.UNKNOWN || face == lastFace || now - lastTime < debounceMs) return

        // 设备朝向发生变化，记录日志
        logger.debug { "Device orientation changed: $lastFace -> $face (z=$z)"}

        // 只有从朝下翻转到朝上时才触发回调（用于OCR功能）
        if (face == Face.UP && lastFace == Face.DOWN) {
            logger.debug { "Device flipped from face-down to face-up, triggering OCR" }
            onFlipChange()
        }



        lastFace = face
        lastTime = now
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
