package net.ankio.auto

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import net.ankio.auto.api.Hooker
import net.ankio.auto.hooks.alipay.AlipayHooker
import net.ankio.auto.hooks.auto.AutoHooker


class HookMainApp : IXposedHookLoadPackage {

    companion object {
        val name = "自动记账"
        val pkg = BuildConfig.APPLICATION_ID
        val versionName = BuildConfig.VERSION_NAME.substringBefore(" - Xposed")
        val versionCode = BuildConfig.VERSION_CODE
        fun getTag(name:String? = null,clazz:String?=null): String {
            var tag: String = if(name===null){
                "[AutoAccounting]"
            }else{
                "[${name}]"
            }
            tag += if(clazz===null){
                "[None]"
            }else{
                "[${clazz}]"
            }
            return tag
        }

    }

    private var mHookList: MutableList<Hooker> = arrayListOf(
        AutoHooker(),//自动记账hook
        AlipayHooker() //支付宝hook
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

}