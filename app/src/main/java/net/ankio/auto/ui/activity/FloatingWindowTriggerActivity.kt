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
import net.ankio.auto.utils.ServiceManager

class FloatingWindowTriggerActivity : BaseActivity() {

    companion object {
        private const val WINDOW_SIZE = 1
        private const val INTENT_TYPE_KEY = "intentType"
        private const val TIMESTAMP_KEY = "t"
    }

    private val serviceManager = ServiceManager.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 先设置自定义密度，保证全程生效
        // DisplayUtils.setCustomDensity(this)

        super.onCreate(savedInstanceState)
        // 2. 透明主题，无布局
        setTheme(R.style.TransparentActivityTheme)
        // 3. 构造 1×1 悬浮窗
        createOnePxWindow()

        // 使用协程初始化ServiceManager，避免阻塞主线程
        serviceManager.initialize(
            caller = this@FloatingWindowTriggerActivity,
            onReady = {
                Logger.i("ServiceManager初始化完成，准备启动服务")
                serviceManager.startCoreService(
                    this@FloatingWindowTriggerActivity,
                    forceStart = true
                )
            },
            onDenied = {
                Logger.w("权限被拒绝，重新请求权限")
                // 权限被拒绝时重新请求
                serviceManager.requestProjectionPermission()
            }
        )
        serviceManager.requestProjectionPermission()
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
            finish()
            return
        }

        // 2. 检查悬浮窗权限（O+）
        if (!Settings.canDrawOverlays(this)
        ) {
            Logger.e("没有悬浮窗权限")
            // 跳转去申请权限
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
            return
        }


        lifecycleScope.launch {
            // 添加超时机制，避免无限等待
            var waitCount = 0
            val maxWaitCount = 50 // 最多等待10秒 (50 * 200ms)

            while (!serviceManager.isReady() && waitCount < maxWaitCount) {
                delay(200)
                waitCount++
            }

            if (waitCount >= maxWaitCount) {
                Logger.e("等待ServiceManager就绪超时")
                finish()
                return@launch
            }

            try {
                val targetIntent =
                    Intent(this@FloatingWindowTriggerActivity, CoreService::class.java).apply {
                        intent.extras?.let(::putExtras)
                    }
                startForegroundService(targetIntent)
                Logger.i("成功启动CoreService")

            } catch (e: Exception) {
                Logger.e("启动服务失败: ${e.message}", e)
            } finally {
                finish()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理ServiceManager资源
        try {
            serviceManager.release()
            Logger.i("ServiceManager资源已清理")
        } catch (e: Exception) {
            Logger.e("清理ServiceManager资源失败: ${e.message}", e)
        }
    }

    override fun finish() {
        super.finish()
        // 无动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+：新接口，第 3 个参数是背景色，传 0 表示不覆盖默认
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            // 旧接口：禁用入/出场动画
            overridePendingTransition(0, 0)
        }
    }

}
