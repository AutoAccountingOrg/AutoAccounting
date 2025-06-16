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

@Singleton      // 如果不用 Hilt，可删除此注解
object ProjectionGateway {

    /* ------------------ 公共 API ------------------ */

    /** 是否已获得 MediaProjection */
    fun isReady() = projection != null || ::resultData.isInitialized

    /** 拿到实例（未准备好会抛异常） */
    fun get(context: Context): MediaProjection {
        if (projection != null) return projection!!
        if (!::resultData.isInitialized) throw RuntimeException("未授权录屏权限")
        val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        projection = mgr.getMediaProjection(Activity.RESULT_OK, resultData)
        return projection!!
    }

    private lateinit var resultData: Intent

    /**
     * 在宿主 (ComponentActivity / Fragment) 的初始化阶段注册 launcher。
     *
     * @param caller   可以是 this@ComponentActivity 或 this@Fragment
     * @param context  用于解析回调中的 MediaProjection；传 `requireContext()` 即可
     * @param onReady  成功授权后的回调
     */
    fun register(
        caller: ActivityResultCaller,
        onReady: () -> Unit
    ): ActivityResultLauncher<Unit> {

        val contract = object : ActivityResultContract<Unit, Intent?>() {

            override fun createIntent(ctx: Context, input: Unit): Intent {
                val mgr = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                        as MediaProjectionManager
                return mgr.createScreenCaptureIntent()
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Intent? =
                if (resultCode == Activity.RESULT_OK) intent else null
        }

        return caller.registerForActivityResult(contract) { data ->
            if (data == null) {           // 用户取消
                Logger.e("MediaProjection denied by user")
                return@registerForActivityResult
            }
            resultData = data
            Logger.i("MediaProjection granted")
            onReady()
        }
    }


    /** 完全退出时调用 */
    fun release() {
        projection?.stop()
        projection = null
    }

    /* ------------------ 内部 ------------------ */
    @Volatile
    private var projection: MediaProjection? = null
}
