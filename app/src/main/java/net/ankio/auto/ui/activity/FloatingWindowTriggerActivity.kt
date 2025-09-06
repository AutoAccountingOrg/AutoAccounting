package net.ankio.auto.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.service.CoreService
import net.ankio.auto.storage.Constants
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity

class FloatingWindowTriggerActivity : BaseActivity() {

    companion object {
        private const val WINDOW_SIZE = 1
        private const val INTENT_TYPE_KEY = "intentType"
        private const val TIMESTAMP_KEY = "t"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.TransparentActivityTheme)
        createOnePxWindow()
    }

    private fun createOnePxWindow() {
        window.apply {
            setGravity(Gravity.START or Gravity.TOP)
            attributes = attributes.apply {
                x = 0
                y = 0
                width = WINDOW_SIZE
                height = WINDOW_SIZE
                type =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val t = intent.getLongExtra(TIMESTAMP_KEY, 0L)
        val type = intent.getStringExtra(INTENT_TYPE_KEY)

        Logger.i("处理Intent: 类型=$type, 时间戳=$t")

        // 1. 超时检查
        if (t < System.currentTimeMillis() - Constants.INTENT_TIMEOUT) {
            Logger.e("Intent已超时: 时间戳=$t")
            exitActivity()
            return
        }

        // 2. 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            Logger.e("没有悬浮窗权限")
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            exitActivity()
            return
        }

        // 3. 确保服务就绪后启动服务
        Logger.i("服务就绪，启动CoreService")
        try {
            CoreService.start(this, intent)
            Logger.i("成功启动CoreService")
        } catch (e: Exception) {
            Logger.e("启动服务失败: ${e.message}", e)
        } finally {
            exitActivity()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理ServiceManager资源
        try {
            Logger.i("ServiceManager资源已清理")
        } catch (e: Exception) {
            Logger.e("清理ServiceManager资源失败: ${e.message}", e)
        }
    }
    private fun exitActivity() {
        finishAffinity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+：新接口，第 3 个参数是背景色，传 0 表示不覆盖默认
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            // 旧接口：禁用入/出场动画
            overridePendingTransition(0, 0)
        }
        window.setWindowAnimations(0)
    }


}
