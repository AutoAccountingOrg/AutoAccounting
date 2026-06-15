/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 */

package net.ankio.tap

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import net.ankio.tap.columbus.BaseTapRT
import net.ankio.tap.columbus.sensors.TapRT
import net.ankio.tap.samsung.SamsungBackTapDetectionService

/**
 * 双击设备背部检测：自动按机型选择 TapTap 同款 Columbus 或三星 RegiStar 模型与运行时。
 *
 * 典型用法：`start()` 后开始监听，`stop()` 卸载传感器并可再次 `start()`（每次会话重建 TFLite 解释器）。
 *
 * @param forcedBuiltinModel 若为 null，则 [TapDeviceModelResolver] 根据厂商 / codename / 屏高自动选模
 * @param sensitivity Columbus 噪声底（与 TapTap 默认 `0.05f` 同量级）；三星路径会映射为 RegiStar 峰值乘子
 */
class TapBackDetector(
    context: Context,
    private val forcedBuiltinModel: TapBuiltinModel? = null,
    private val sensitivity: Float = 0.05f,
    private val sensorLooper: Looper? = null,
    private val callbackLooper: Looper = Looper.getMainLooper(),
    private val onDoubleBackTap: () -> Unit,
) {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var ownedSensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private val callbackHandler = Handler(callbackLooper)

    private var classifier: TapTfClassifier? = null
    private var tapRuntime: BaseTapRT? = null

    private var sensorEventCount = 0L
    private var loggedFirstAccel = false
    private var loggedFirstGyro = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val tap = tapRuntime ?: return
            if (event == null) return
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->
                    if (!loggedFirstAccel) {
                        loggedFirstAccel = true
                        TapLogger.d(SUB, "first accelerometer batch t=${event.timestamp}")
                    }
                Sensor.TYPE_GYROSCOPE ->
                    if (!loggedFirstGyro) {
                        loggedFirstGyro = true
                        TapLogger.d(SUB, "first gyroscope batch t=${event.timestamp}")
                    }
            }
            tap.updateData(
                event.sensor.type,
                event.values[0],
                event.values[1],
                event.values[2],
                event.timestamp,
                SAMPLING_INTERVAL_NS,
                isHeuristic = false,
            )
            sensorEventCount++
            if (sensorEventCount % SENSOR_DIAG_EVERY == 0L) {
                TapLogger.d(SUB, "sensor throttle #${sensorEventCount} ${tap.debugSummary()}")
            }
            val timing = tap.checkDoubleTapTiming(event.timestamp)
            if (timing == 2) {
                TapLogger.i(SUB, "checkDoubleTapTiming=2 -> posting onDoubleBackTap to callbackLooper")
                callbackHandler.post { onDoubleBackTap() }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private var started = false

    fun isStarted(): Boolean = started

    fun start() {
        if (started) return
        if (accelerometer == null || gyroscope == null) {
            TapLogger.w(SUB, "no accelerometer or gyroscope")
            return
        }
        sensorEventCount = 0L
        loggedFirstAccel = false
        loggedFirstGyro = false

        val looper = sensorLooper ?: HandlerThread("tap-back-sensor").also { t ->
            t.start()
            ownedSensorThread = t
        }.looper
        sensorHandler = Handler(looper)

        val model = forcedBuiltinModel ?: TapDeviceModelResolver.resolve(appContext)
        classifier = TapTfClassifier(appContext.assets, model)
        tapRuntime = if (model.isSamsungRegi()) {
            val samsungSens = mapColumbusNoiseToSamsungMultiplier(sensitivity)
            SamsungBackTapDetectionService(
                tripleTapEnabled = false,
                sensitivity = samsungSens,
                classifier = classifier!!,
            ).also { it.reset(false) }
        } else {
            ProjectTapRt(SIZE_WINDOW_NS, classifier!!, sensitivity).also { rt ->
                applyHybridColumbusInit(rt)
            }
        }

        runCatching {
            sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST,
                sensorHandler!!,
            )
            sensorManager.registerListener(
                listener,
                gyroscope,
                SensorManager.SENSOR_DELAY_FASTEST,
                sensorHandler!!,
            )
        }.onFailure { ex ->
            TapLogger.e(SUB, "registerListener failed", ex)
            tapRuntime = null
            classifier?.close()
            classifier = null
            ownedSensorThread?.quitSafely()
            ownedSensorThread = null
            sensorHandler = null
            return
        }

        started = true
        TapLogger.i(
            SUB,
            "started builtin=${model.id} path=${model.assetPath} samsung=${model.isSamsungRegi()} " +
                    "sensitivity=$sensitivity sensorThread=${looper.thread.name} callback=${callbackLooper.thread.name}",
        )
    }

    fun stop() {
        if (!started) return
        sensorManager.unregisterListener(listener)
        ownedSensorThread?.quitSafely()
        ownedSensorThread = null
        sensorHandler = null
        classifier?.close()
        classifier = null
        tapRuntime = null
        started = false
        TapLogger.i(SUB, "stopped (processed ~$sensorEventCount sensor events this session)")
    }

    /**
     * 与 TapTap `GestureSensorImpl.startListening(heuristicMode = true)` 滤波/峰值参数一致，
     * 再配合 [TapRT.updateData] 的 ML 分支（混合模式）。
     */
    private fun applyHybridColumbusInit(rt: ProjectTapRt) {
        rt.getLowpassKey().setPara(0.2f)
        rt.getHighpassKey().setPara(0.2f)
        rt.getPositivePeakDetector().setMinNoiseTolerate(0.05f)
        rt.getPositivePeakDetector().setWindowSize(0x40)
        rt.reset(false)
    }

    private fun BaseTapRT.debugSummary(): String = when (this) {
        is TapRT -> debugPipelineSummary()
        is SamsungBackTapDetectionService -> debugPipelineSummary()
        else -> "runtime=${this::class.java.simpleName}"
    }

    /**
     * 将 Columbus 噪声参数映射到三星峰值乘子区间（对齐 TapTap `SAMSUNG_SENSITIVITY_VALUES` 量级）。
     * Columbus 越小越灵敏 → 三星乘子略抬高。
     */
    private fun mapColumbusNoiseToSamsungMultiplier(columbusNoise: Float): Float {
        val ratio = (columbusNoise / 0.05f).coerceIn(0.25f, 4f)
        return (1f / ratio).coerceIn(0.25f, 1.5f)
    }

    private companion object {
        private const val SUB = "TapBack"
        private const val SIZE_WINDOW_NS = 160_000_000L
        private const val SAMPLING_INTERVAL_NS = 2_500_000L
        private const val SENSOR_DIAG_EVERY = 8_000L
    }
}
