package net.ankio.auto

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import net.ankio.auto.api.HookBase
import net.ankio.auto.hooks.android.Android


class HookMainApp : IXposedHookLoadPackage {
    private var mHookList: MutableList<HookBase> = arrayListOf(
        Android()
    )


    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        for (hook in mHookList) {
           // try {
                hook.onLoadPackage(lpparam)
          //  }catch (e:Exception){
          //      e.message?.let { Log.e("AutoAccountingError", it) }
          //      println(e)
          //      XposedBridge.log(e.message)
          //  }
        }
    }

}