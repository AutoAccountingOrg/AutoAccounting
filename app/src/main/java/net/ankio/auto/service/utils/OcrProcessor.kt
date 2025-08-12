package net.ankio.auto.service.utils

import android.content.Context
import android.graphics.Bitmap
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.storage.Logger

/**
 * OCR处理器对象，用于处理图像文字识别
 * 使用Google ML Kit的文本识别功能，支持中文识别
 */
class OcrProcessor(context: Context) {
    private var ocr = OCR(context)
    private val config = OcrConfig()

    init {
        // 配置

//config.labelPath = null

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
        config.isRunCls = false
        config.isRunRec = true

// 使用所有核心运行
        config.cpuPowerMode = CpuPowerMode.LITE_POWER_FULL

// 绘制文本位置
        config.isDrwwTextPositionBox = false

    }


    private var init = false

    /**
     * 识别图片中的文字
     *
     * @param bitmap 需要识别的图片
     * @return 识别出的文字内容，如果识别失败则返回空字符串
     */
    suspend fun recognize(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        if (!init) {
            val initResult = ocr.initModelSync(config)
            if (initResult.isFailure) {
                Logger.d("模型初始化失败....")
                return@withContext ""
            }
            init = true
        }
        Logger.d("截屏结果识别中....")
        val result = ocr.runSync(bitmap)
        if (result.isFailure) return@withContext ""
        else return@withContext result.getOrNull()?.simpleText ?: ""
    }
}
