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

package net.ankio.ocr

import android.content.Context
import android.graphics.Bitmap
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import android.util.Log

open class OcrProcessor {
    private val config = OcrConfig()
    private var ocr: OCR? = null

    init {
        config.modelPath =
            "models" // 不使用 "/" 开头的路径表示安装包中 assets 目录下的文件，例如当前表示 assets/models/ocr_v2_for_cpu
//config.modelPath = "/sdcard/Android/data/com.equationl.paddleocr4android.app/files/models" // 使用 "/" 表示手机储存路径，测试时请将下载的三个模型放置于该目录下
        config.clsModelFilename = "cls.nb" // cls 模型文件名
        config.detModelFilename = "det.nb" // det 模型文件名
        config.recModelFilename = "rec.nb" // rec 模型文件名

// 运行全部模型
// 请根据需要配置，三项全开识别率最高；如果只开识别几乎无法正确识别，至少需要搭配检测或分类其中之一
// 也可单独运行 检测模型 获取文本位置
        config.isRunDet = true
        config.isRunCls = true
        config.isRunRec = true

        config.cpuThreadNum = autoSelectCpuThreads()
        config.cpuPowerMode = autoSelectCpuPowerMode()

        config.isDrwwTextPositionBox = false
    }

    /**
     * 应用上下文，用于访问 assets 与缓存目录。
     */
    private lateinit var appCtx: Context

    /**
     * 绑定上下文（会持有 applicationContext 以避免泄露）。
     */
    fun attach(context: Context) = apply {
        this.appCtx = context.applicationContext
    }

    private var output: ((string: String, type: Int) -> Unit)? = null
    fun log(output: (string: String, type: Int) -> Unit) = apply {
        this.output = output
    }

    /**
     * 基于设备 CPU 可用核心数选择推理线程数。
     * 返回范围：[1, 4]，并为系统/前台线程预留至少 1 核。
     */
    private fun autoSelectCpuThreads(): Int {
        val cores = try {
            Runtime.getRuntime().availableProcessors()
        } catch (_: Throwable) {
            1
        }
        val reservedAware = cores - 1
        val candidate = if (reservedAware <= 0) 1 else reservedAware
        return minOf(4, maxOf(1, candidate))
    }

    /**
     * 基于设备 CPU 可用核心数选择功耗模式。
     * 简单规则，避免过度设计：多核偏高性能，少核偏低功耗。
     */
    private fun autoSelectCpuPowerMode(): CpuPowerMode {
        val cores = try {
            Runtime.getRuntime().availableProcessors()
        } catch (_: Throwable) {
            1
        }
        return if (cores >= 4) CpuPowerMode.LITE_POWER_HIGH else CpuPowerMode.LITE_POWER_LOW
    }


    private suspend fun ensureInit(): Boolean {
        if (ocr == null) {
            ocr = OCR(appCtx)
            val result = ocr!!.initModelSync(config)
            output?.let {
                it("init ocr ${result.isSuccess},cpu: ${config.cpuThreadNum}", Log.DEBUG)
                if (result.isFailure) {
                    it("init ocr error  ${result.exceptionOrNull()}", Log.ERROR)
                }
            }
            return result.isSuccess
        }
        return true
    }


    suspend fun startProcess(bitmap: Bitmap): String {
        if (!ensureInit()) {
            output?.let {
                it("ocr process error: not init", Log.DEBUG)
            }
            return ""
        }
        val result = ocr!!.runSync(bitmap)
        if (result.isFailure) {
            output?.let {
                it("ocr process error: ${result.exceptionOrNull()?.message ?: ""}", Log.ERROR)
            }
            return ""
        }
        return result.getOrNull()?.simpleText ?: ""
    }


}