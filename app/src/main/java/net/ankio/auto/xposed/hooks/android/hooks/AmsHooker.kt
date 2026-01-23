package net.ankio.auto.xposed.hooks.android.hooks

import android.content.pm.ApplicationInfo
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime


/**
 * AMS 进程优先级调整
 */
class AmsHooker : PartHooker() {

    private val targetPackage = BuildConfig.APPLICATION_ID

    private companion object {
        const val PROCESS_ADJ = -1000
        const val OOM_ADJUSTER = "com.android.server.am.OomAdjuster";
    }


    inner class ProcessRecord(pr: Any) {
        private var instance: Any? = null
        private var info: ApplicationInfo? = null
        private var processName: String? = null
        public var packageName: String? = null
        private var isolated = false
        private var isSdkSandbox = false
        private var uid = 0
        private var mState: Any? = null

        init {
            this.instance = pr
            this.processName = XposedHelpers.getObjectField(pr, "processName") as String?
            this.info = XposedHelpers.getObjectField(pr, "info") as ApplicationInfo?
            info?.let { this.packageName = it.packageName }

            this.uid = XposedHelpers.getIntField(pr, "uid")
            this.isolated = XposedHelpers.getBooleanField(pr, "isolated")
            this.isSdkSandbox = XposedHelpers.getBooleanField(pr, "isSdkSandbox")
            this.mState = XposedHelpers.getObjectField(pr, "mState")
        }


        fun setCurRawAdj(adj: Int) {
            XposedHelpers.callMethod(mState, "setCurRawAdj", adj)
        }

        fun setCurAdj(adj: Int) {
            XposedHelpers.callMethod(mState, "setCurAdj", adj)
        }
    }

    override fun hook() {
        Hooker.allMethodsEqBefore(Hooker.loader(OOM_ADJUSTER), "applyOomAdjLSP") { param, method ->
            val record = param.args[0] ?: return@allMethodsEqBefore null
            val pr = ProcessRecord(record)

            if (pr.packageName?.contains(targetPackage) != true) {
                return@allMethodsEqBefore null
            }

            pr.setCurAdj(PROCESS_ADJ);
            pr.setCurRawAdj(PROCESS_ADJ);

            AppRuntime.manifest.d("update the adj to $PROCESS_ADJ")
        }
    }


}