package net.ankio.auto.service

import android.content.Intent
import net.ankio.auto.BuildConfig
import net.ankio.auto.autoApp
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.hooks.auto.AutoHooker
import net.ankio.auto.xposed.hooks.common.JsEngine
import net.ankio.auto.xposed.hooks.common.UnLockScreen
import org.ezbook.server.Server

class BackgroundHttpService : ICoreService() {


    private lateinit var httpService: Server

    override fun onCreate(coreService: CoreService) {
        super.onCreate(coreService)

        if (PrefManager.workMode === WorkMode.Ocr) {
            Logger.d("Initializing Xposed hooks for OCR mode")
            AppRuntime.manifest = AutoHooker()
            AppRuntime.modulePath = coreService.packageManager
                .getApplicationInfo(BuildConfig.APPLICATION_ID, 0)
                .sourceDir
            AppRuntime.classLoader = coreService.classLoader
            AppRuntime.application = autoApp

            Server.packageName = BuildConfig.APPLICATION_ID
            Server.versionName = BuildConfig.VERSION_NAME
            Server.debug = BuildConfig.DEBUG

            /**
             * js引擎
             */
            JsEngine.init()
            /**
             * 解锁屏幕
             */
            UnLockScreen.init()
            /**
             * 启动自动记账服务
             */
            httpService = Server(autoApp)

            httpService.startServer()

            Logger.d("Server start success")

        } else {
            Logger.d("Skipped hook initialization for workMode=${PrefManager.workMode}")
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {
        Logger.d("onStartCommand invoked - intent=$intent, flags=$flags, startId=$startId")
    }

    override fun onDestroy() {
        Logger.d("onDestroy invoked, cleaning up if necessary")
        if (::httpService.isInitialized) httpService.stopServer()
    }
}