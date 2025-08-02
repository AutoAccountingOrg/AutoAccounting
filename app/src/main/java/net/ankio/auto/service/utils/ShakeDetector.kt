package net.ankio.auto.service.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import net.ankio.auto.storage.Logger

/**
 * 朝向变化检测器：检测到朝向变化时立即触发回调
 *
 * @param manager     SensorManager
 * @param debounceMs  最短稳定间隔（ms）
 * @param onFlipChange 回调；参数为 true = Face-Down，false = Face-Up（即当前朝向）
 */
class ShakeDetector(
    private val manager: SensorManager,
    private val debounceMs: Long = 400L,
    private val onFlipChange: () -> Unit
) : SensorEventListener {

    private val sensor: Sensor? = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private enum class Face { UP, DOWN, UNKNOWN }

    private var lastFace = Face.UNKNOWN          // 最近一次稳定面
    private var lastTime = 0L                    // 最近一次稳定时间

    fun start(): Boolean = sensor?.let {
        manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        true
    } ?: false.also { Logger.w("No gravity/accelerometer sensor") }

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

        // 状态发生变化，立即触发回调
        Logger.d("Face changed: $lastFace -> $face (z=$z)")

        //只有to Up的才进行回调
        if (face == Face.UP && lastFace == Face.DOWN) {
            onFlipChange()
        }



        lastFace = face
        lastTime = now
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
