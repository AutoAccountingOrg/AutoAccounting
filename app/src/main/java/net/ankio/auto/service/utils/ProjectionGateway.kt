package net.ankio.auto.service.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import net.ankio.auto.storage.Logger
import javax.inject.Singleton

/**
 * ProjectionGateway 是一个单例对象，用于管理 Android 的屏幕录制权限和 MediaProjection 实例。
 * 它提供了获取屏幕录制权限、创建 MediaProjection 实例以及管理其生命周期的功能。
 *
 * 主要功能：
 * 1. 请求和管理屏幕录制权限
 * 2. 创建和管理 MediaProjection 实例
 * 3. 提供权限状态检查
 * 4. 处理权限请求结果
 */
@Singleton      // 如果不用 Hilt，可删除此注解
object ProjectionGateway {

    /* ------------------ 公共 API ------------------ */

    /**
     * 检查是否已经获得 MediaProjection 权限
     * @return 如果已经获得权限返回 true，否则返回 false
     */
    fun isReady() = projection != null || ::resultData.isInitialized

    /**
     * 获取 MediaProjection 实例
     * @param context 上下文对象，用于获取 MediaProjectionManager 服务
     * @return MediaProjection 实例
     * @throws RuntimeException 如果未获得录屏权限
     */
    fun get(context: Context): MediaProjection {
        if (projection != null) return projection!!
        if (!::resultData.isInitialized) throw RuntimeException("未授权录屏权限")
        val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        projection = mgr.getMediaProjection(Activity.RESULT_OK, resultData)
        return projection!!
    }

    /** 存储权限请求结果的 Intent 数据 */
    private lateinit var resultData: Intent

    /**
     * 注册屏幕录制权限请求的 ActivityResultLauncher
     *
     * @param caller 可以是 ComponentActivity 或 Fragment 实例
     * @param onReady 权限授予成功后的回调函数
     * @return ActivityResultLauncher 实例，用于启动权限请求
     */
    fun register(
        caller: ActivityResultCaller,
        onReady: () -> Unit,
        onDenied: () -> Unit
    ): ActivityResultLauncher<Unit> {

        val contract = object : ActivityResultContract<Unit, Intent?>() {
            /**
             * 创建请求屏幕录制权限的 Intent
             */
            override fun createIntent(ctx: Context, input: Unit): Intent {
                val mgr = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                        as MediaProjectionManager
                return mgr.createScreenCaptureIntent()
            }

            /**
             * 解析权限请求结果
             * @return 如果用户授予权限返回 Intent，否则返回 null
             */
            override fun parseResult(resultCode: Int, intent: Intent?): Intent? =
                if (resultCode == Activity.RESULT_OK) intent else null
        }

        return caller.registerForActivityResult(contract) { data ->
            if (data == null) {           // 用户取消
                Logger.e("MediaProjection denied by user")
                onDenied()
                return@registerForActivityResult
            }
            resultData = data
            Logger.i("MediaProjection granted")
            onReady()
        }
    }

    /**
     * 释放 MediaProjection 资源
     * 在应用退出或不再需要屏幕录制时调用
     */
    fun release() {
        projection?.stop()
        projection = null
    }

    /* ------------------ 内部 ------------------ */
    /** MediaProjection 实例，使用 volatile 确保多线程可见性 */
    @Volatile
    private var projection: MediaProjection? = null
}
