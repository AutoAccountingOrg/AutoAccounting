/*
 * 源自 TapTap `samsung` 模块（三星 RegiStar backtap），包名改为 [net.ankio.tap.samsung] 以便合入 `:tap`。
 */

package net.ankio.tap.samsung

import android.os.SystemClock
import net.ankio.tap.TapLogger
import net.ankio.tap.TapTfClassifier
import net.ankio.tap.columbus.BaseTapRT
import java.util.ArrayDeque
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * 三星 RegiStar 双击背部检测管线（与 Columbus [TapRT] 并行存在）。
 *
 * @param tripleTapEnabled 本应用仅双击场景，传 `false` 即可。
 * @param sensitivity 峰值检测乘子，TapTap 中来自 [TapTapGestureSensorImpl.SAMSUNG_SENSITIVITY_VALUES] 档位映射。
 */
class SamsungBackTapDetectionService(
    private val tripleTapEnabled: Boolean,
    private val sensitivity: Float,
    private val classifier: TapTfClassifier,
) : BaseTapRT {

    companion object {
        private const val sSamplingIntervalInNano = 2500000L
        private const val mMinTimeGapNs = 100000000L
        private const val mMaxTimeGapNs = 600000000L
        private const val mMaxTimeGapTripleNs = 750000000L
        private const val SUB = "TapBack"
    }

    private val mSensorEventProcessor = SensorEventProcessor(sensitivity)
    private val _tBackTapTimestamps = ArrayDeque<Long>()
    private val mIsFlat = IntArray(400)
    private var mFlatIndex = 0
    private val mInput = Array(0x120) { FloatArray(1) }
    private val mOutput = Array(1) { FloatArray(6) }

    override fun updateData(
        type: Int,
        lastX: Float,
        lastY: Float,
        lastZ: Float,
        lastT: Long,
        interval: Long,
        isHeuristic: Boolean,
    ) {
        if (lastZ <= 60.0f && lastZ >= -60.0f) {
            mSensorEventProcessor.updateData(
                type,
                lastX,
                lastY,
                lastZ,
                lastT,
                sSamplingIntervalInNano,
            )
            val f = sqrt(lastX * lastX + lastY * lastY + lastZ * lastZ)
            mSensorEventProcessor.updateData(
                4,
                acos(lastX / f),
                acos(lastY / f),
                acos(lastZ / f),
                lastT,
                sSamplingIntervalInNano,
            )
            val v = Math.toDegrees(acos((lastZ / f).toDouble())).toInt()
            mIsFlat[mFlatIndex] = if (v < 10 || v > 170) 1 else 0
            val v1 = mFlatIndex + 1
            mFlatIndex = v1
            if (v1 == 400) {
                mFlatIndex = 0
            }
            if (SensorEventProcessor.az.checkIfNotFull()) {
                return
            }
            if (SensorEventProcessor.rightCount >= 24) {
                val flatRatio = mIsFlat.sum().toFloat() / 400.0f
                SensorEventProcessor.rightCount = 0
                SensorEventProcessor.hasMajorPeak = false
                if (SensorEventProcessor.nMinorPeaks >= 2 || lastT - SensorEventProcessor.lastWindowEnd <= 120000000L) {
                    // No-op
                } else if (flatRatio <= 0.5f) {
                    processSingleTap()
                }
                SensorEventProcessor.nMinorPeaks = 0
                SensorEventProcessor.lastWindowEnd = lastT
            }
        }
    }

    override fun checkDoubleTapTiming(timestamp: Long): Int =
        if (tripleTapEnabled) {
            checkTapTimingTripleTap(timestamp)
        } else {
            checkTapTimingDoubleTap(timestamp)
        }

    private fun checkTapTimingDoubleTap(timestamp: Long): Int {
        val v0 = _tBackTapTimestamps.iterator()
        while (v0.hasNext()) {
            val v1 = v0.next()
            if (timestamp - v1 <= mMaxTimeGapNs) {
                continue
            }
            v0.remove()
        }
        if (_tBackTapTimestamps.isEmpty()) {
            return 0
        }

        val v6 = _tBackTapTimestamps.iterator()
        while (v6.hasNext()) {
            val v0_1 = _tBackTapTimestamps.last()
            val v7 = v6.next()
            if (v0_1 - v7 <= mMinTimeGapNs) {
                continue
            }

            TapLogger.i(
                SUB,
                "Samsung double-tap window ok (${mMinTimeGapNs / 1_000_000}ms..${mMaxTimeGapNs / 1_000_000}ms)",
            )
            _tBackTapTimestamps.clear()
            return 2
        }

        return 1
    }

    private fun checkTapTimingTripleTap(timestamp: Long): Int {
        val firstPassIterator = _tBackTapTimestamps.iterator()
        while (firstPassIterator.hasNext()) {
            val pastTimestamp = firstPassIterator.next()
            if (timestamp - pastTimestamp <= mMaxTimeGapTripleNs) {
                continue
            }
            firstPassIterator.remove()
        }

        if (_tBackTapTimestamps.isEmpty()) {
            return 0
        }

        var tapCount = 0
        val secondPassIterator = _tBackTapTimestamps.iterator()
        val timeNow = SystemClock.elapsedRealtimeNanos()
        while (secondPassIterator.hasNext()) {
            val pastTimestamp = secondPassIterator.next()
            if (_tBackTapTimestamps.last() - pastTimestamp <= mMinTimeGapNs) {
                continue
            }
            tapCount++
        }

        if (tapCount >= 3 || timeNow - _tBackTapTimestamps.first() > mMaxTimeGapTripleNs) {
            _tBackTapTimestamps.clear()
            if (tapCount == 1) {
                return 2
            } else if (tapCount >= 2) {
                return 3
            }
        }

        return 1
    }

    private fun processSingleTap() {
        var f = 1.4E-45f
        var f1 = 1.4E-45f
        var f2 = 1.4E-45f
        var v = 0
        while (v < 0x30) {
            val v1 = v + 0x160
            val f3: Float = SensorEventProcessor.az.getValueAt(v1)
            val f4: Float = SensorEventProcessor.ay.getValueAt(v1)
            val f5: Float = SensorEventProcessor.ax.getValueAt(v1)
            mInput[v][0] = f3
            mInput[v + 0x30][0] = f4
            mInput[v + 0x60][0] = f5
            val arr_f = mInput[v + 0x90]
            arr_f[0] = SensorEventProcessor.gz.getValueAt(v1)
            val arr_f1 = mInput[v + 0xC0]
            arr_f1[0] = SensorEventProcessor.gy.getValueAt(v1)
            val arr_f2 = mInput[v + 0xF0]
            arr_f2[0] = SensorEventProcessor.gx.getValueAt(v1)
            f1 = f1.toDouble().coerceAtLeast(sqrt((f3 * f3).toDouble())).toFloat()
            f2 = f2.toDouble().coerceAtLeast(sqrt((f4 * f4).toDouble())).toFloat()
            f = f.toDouble().coerceAtLeast(sqrt((f5 * f5).toDouble())).toFloat()
            ++v
        }
        if (f.compareTo(f1) <= 0 && f2 <= f1) {
            classifier.predictArray(mInput, mOutput)
            var v2 = 1
            var v3 = 0
            while (v2 < 6) {
                if (mOutput[0][v2] > mOutput[0][v3]) {
                    v3 = v2
                }
                ++v2
            }
            when (v3) {
                0 -> {
                    _tBackTapTimestamps.add(SensorEventProcessor.timestamp)
                    return
                }
            }
            return
        }
    }

    /** 与 Columbus [TapRT.debugPipelineSummary] 对应，供 [TapBackDetector] 节流日志使用。 */
    fun debugPipelineSummary(): String =
        "samsung rightCount=${SensorEventProcessor.rightCount} backQ=${_tBackTapTimestamps.size} " +
                "flatIdx=$mFlatIndex minorPeaks=${SensorEventProcessor.nMinorPeaks}"

    override fun reset(justClearFv: Boolean) {
        mSensorEventProcessor.reset()
        mSensorEventProcessor.sensitivity = sensitivity
        _tBackTapTimestamps.clear()
        mFlatIndex = 0
        mIsFlat.fill(0)
    }
}
