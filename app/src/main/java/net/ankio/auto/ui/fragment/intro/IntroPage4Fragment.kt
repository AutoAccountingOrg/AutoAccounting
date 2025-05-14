package net.ankio.auto.ui.fragment.intro

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.FragmentIntroPage4Binding
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.components.ExpandableCardView
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.vm.IntroSharedVm
import net.ankio.auto.utils.PrefManager

/**
 * 引导页 #4 – 后台保活相关权限
 */
class IntroPage4Fragment : BaseFragment<FragmentIntroPage4Binding>() {

    private val vm: IntroSharedVm by activityViewModels()

    // ──────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* 继续 → 下一页 */
        binding.btnContinue.setOnClickListener { vm.pageRequest.value = 4 }

        /* 卡片点击动作 */
        registerCardClick(binding.cardBatteryOpt) { openBatteryOptimizationPage() }
        registerCardClick(binding.cardAutostart) { openAutoStartPage() }
        registerCardClick(binding.cardTaskLock) { showTaskLockGuide() }
    }

    override fun onResume() {
        super.onResume()
        PrefManager.introIndex = 3

        /* Xposed 模式下无需电池优化/任务锁定 */
        if (PrefManager.workMode == WorkMode.Xposed) {
            binding.cardBatteryOpt.visibility = View.GONE
            binding.cardTaskLock.visibility = View.GONE
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * 为 [ExpandableCardView] 绑定点击回调
     */
    private inline fun registerCardClick(card: ExpandableCardView, crossinline action: () -> Unit) {
        card.setOnCardClickListener { action() }
    }

    @SuppressLint("BatteryLife")
    private fun openBatteryOptimizationPage() {
        val ctx = requireContext()


        // 已经在白名单 → 仅提示
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
            ToastUtils.info(R.string.keepalive_battery_already)
            return
        }

        // ① 系统“电池优化”总列表（无需额外权限）
        val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val canResolve = ctx.packageManager.resolveActivity(listIntent, 0) != null
        if (canResolve) {
            startActivity(listIntent)
            ToastUtils.info(R.string.keepalive_battery_tip_list)
            return
        }

        // ② 兜底 → 本应用详情页
        openAppDetailsFallback()
    }

    /** 打开“应用详情”并给出提示 */
    private fun openAppDetailsFallback() {
        val ctx = requireContext()
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${ctx.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            ToastUtils.info(R.string.keepalive_battery_tip_details)
        }.onFailure {
            ToastUtils.error(R.string.keepalive_battery_fail)
        }
    }

    /** 跳转到厂商的“自启动/后台弹性启动”设置界面；全部失败时退到本应用详情页 */
    private fun openAutoStartPage() {
        val ctx = requireContext()

        /* 不同 ROM 常见组件 */
        val candidateComponents = listOf(
            // MIUI
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),

            // EMUI / MagicOS
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),

            // OPPO / realme
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            ),
            ComponentName(
                "com.coloros.oppoguardelf",
                "com.coloros.powermanager.fuelgaugebattery.ui.PowerSaveSmart"
            ),

            // vivo
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        )

        /* 扩展函数：当前设备是否能解析该 Intent */
        fun Intent.isResolvable() =
            ctx.packageManager.resolveActivity(this, 0) != null

        var launched = false
        for (comp in candidateComponents) {
            val intent = Intent().apply {
                component = comp
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.isResolvable()) {
                runCatching { startActivity(intent) }
                    .onSuccess { launched = true }
                if (launched) break
            }
        }

        /* 全部方案未命中 → 打开应用详情页并提示用户手动设置 */
        if (!launched) {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${ctx.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                ToastUtils.info(R.string.keepalive_autostart_tip)
            }.onFailure {
                ToastUtils.error(R.string.keepalive_autostart_fail)
            }
        }
    }


    /** 任务管理器“锁定”功能无法通过 Intent 直接跳转 → 弹提示 */
    private fun showTaskLockGuide() {
        ToastUtils.info(R.string.keepalive_tasklock_tip)   // “请在最近任务中下拉/长按本应用以锁定，防止被系统清理”
    }
}
