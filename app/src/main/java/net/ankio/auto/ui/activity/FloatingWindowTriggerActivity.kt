package net.ankio.auto.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import net.ankio.auto.R
import net.ankio.auto.service.CoreService
import net.ankio.auto.storage.Constants
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity

class FloatingWindowTriggerActivity : BaseActivity() {

    companion object {
        private const val WINDOW_SIZE = 1
        private const val INTENT_TYPE_KEY = "intentType"
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
        val type = intent.getStringExtra(INTENT_TYPE_KEY)

        Logger.i("处理Intent: 类型=$type")


        // https://stackoverflow.com/questions/46173460/why-does-settings-candrawoverlays-method-in-android-8-returns-false-when-use
        // 应该是安卓的历史遗留问题
        // 取消悬浮窗权限检查，直接在服务里面catch权限异常进行跳转

        // 3. 确保服务就绪后启动服务
        Logger.i("服务就绪，启动CoreService")
        try {
            CoreService.start(this, intent)
            Logger.i("成功启动CoreService")
        } catch (e: Exception) {
            Logger.e("启动服务失败: ${e.message}", e)
        } finally {
            window.decorView.postDelayed({
                exitActivity()
            }, 1000)
        }
    }

    private fun exitActivity() {
        finish()
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
