package net.ankio.auto

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import net.ankio.auto.api.Hooker
import net.ankio.auto.hooks.alipay.AlipayHooker
import net.ankio.auto.hooks.auto.AutoHooker
import net.ankio.auto.hooks.setting.SettingHooker
import net.ankio.auto.hooks.wechat.WechatHooker

class HookMainApp : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        val name = "自动记账"
        val versionName = BuildConfig.VERSION_NAME.substringBefore(" - Xposed")

        var modulePath = ""
    }

    private var mHookList: MutableList<Hooker> =
        arrayListOf(
            AutoHooker(), // 自动记账hook
            AlipayHooker(), // 支付宝hook
            WechatHooker(), // 微信Hook
            SettingHooker() // Android Shell Hook
        )

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        for (hook in mHookList) {
            try {
                hook.onLoadPackage(lpparam)
            } catch (e: Exception) {
                e.message?.let { Log.e("AutoAccountingError", it) }
                println(e)
                XposedBridge.log(e.message)
            }
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        modulePath = startupParam?.modulePath ?: ""
    }
}
