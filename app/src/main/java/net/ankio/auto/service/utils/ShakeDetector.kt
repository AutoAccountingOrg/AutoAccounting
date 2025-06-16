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

package net.ankio.auto.service.utils


import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import net.ankio.auto.storage.Logger

/**
 * 摇动检测器类，用于检测设备的摇动动作
 * @param manager 传感器管理器实例
 * @param thresholdG 摇动检测的阈值（重力加速度的倍数），默认为2.4f
 * @param debounceMs 两次检测之间的最小时间间隔（毫秒），默认为500ms
 * @param onShake 检测到摇动时的回调函数
 */
class ShakeDetector(
    private val manager: SensorManager,
    thresholdG: Float = 2.4f,      // 默认更灵敏
    private val debounceMs: Long = 500L,
    private val onShake: () -> Unit
) : SensorEventListener {

    /** 摇动检测阈值的平方值，用于优化计算 */
    private val threshold2 = thresholdG * thresholdG      // 比较平方更快

    /** 上次检测到摇动的时间戳 */
    private var last = 0L

    /** 加速度传感器实例 */
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /**
     * 开始监听设备摇动
     * @return 如果成功启动监听返回true，否则返回false
     */
    fun start(): Boolean = sensor?.let {
        manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        true
    } ?: false.also { Logger.w("No accelerometer found") }

    /**
     * 停止监听设备摇动
     */
    fun stop() = manager.unregisterListener(this)

    /**
     * 传感器数据变化时的回调方法
     * @param e 传感器事件，包含加速度数据
     */
    override fun onSensorChanged(e: SensorEvent) {
        // 将加速度值转换为重力加速度的倍数
        val gX = e.values[0] / SensorManager.GRAVITY_EARTH
        val gY = e.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = e.values[2] / SensorManager.GRAVITY_EARTH
        val gForce2 = gX * gX + gY * gY + gZ * gZ              // 平方和

        // 当加速度平方和超过阈值，且距离上次检测超过防抖时间时触发回调
        if (gForce2 >= threshold2) {
            val now = SystemClock.uptimeMillis()
            if (now - last >= debounceMs) {
                last = now
                Logger.d("Shake detected: g²=$gForce2")
                onShake()
            }
        }
    }

    /**
     * 传感器精度变化时的回调方法
     * @param s 传感器实例
     * @param i 新的精度值
     */
    override fun onAccuracyChanged(s: Sensor?, i: Int) = Unit
}
