package net.ankio.auto.ui.fragment.intro

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentIntroPageKeepBinding
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.ui.components.ExpandableCardView
import net.ankio.auto.ui.utils.ToastUtils
import androidx.core.net.toUri
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.constant.WorkMode

/**
 * 引导页 #4 – 后台保活相关权限
 */
class IntroPageKeepFragment : BaseIntroPageFragment<FragmentIntroPageKeepBinding>() {

    // ──────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* 继续 → 下一页 */
        binding.btnNext.setOnClickListener {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.APP
        }

        binding.btnBack.setOnClickListener {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.PERMISSION
        }
        setupCardsDynamic()
    }

    override fun onResume() {
        super.onResume()

    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Keep-alive Items
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * 保活项数据类
     * @param iconRes 图标资源ID
     * @param titleRes 标题资源ID
     * @param descRes 描述资源ID
     * @param onClick 点击处理方法
     * @param viewId 视图ID
     */
    data class KeepItem(
        val iconRes: Int,
        val titleRes: Int,
        val descRes: Int,
        val onClick: () -> Unit,
        var viewId: Int = View.NO_ID
    )

    // 保活项列表
    private lateinit var keepItems: MutableList<KeepItem>

    /**
     * 动态设置保活卡片
     */
    private fun setupCardsDynamic() {
        val container = binding.keepAliveGroup
        val ctx = requireContext()
        val isXposed = WorkMode.isXposed()

        // 构建保活项列表
        keepItems = mutableListOf<KeepItem>().apply {
            // 电池优化白名单
            if (!isXposed) {
                add(
                    KeepItem(
                        iconRes = R.drawable.ic_battery,
                        titleRes = R.string.keepalive_battery_title,
                        descRes = R.string.keepalive_battery_desc,

                        onClick = { openBatteryOptimizationPage() }
                    )
                )
            }

            // 自启动权限
            add(
                KeepItem(
                    iconRes = R.drawable.ic_autostart,
                    titleRes = R.string.keepalive_autostart_title,
                    descRes = R.string.keepalive_autostart_desc,
                    // 无法检测是否已授权
                    onClick = { openAutoStartPage() }
                )
            )

            // 任务锁定
            if (!isXposed) {
                add(
                    KeepItem(
                        iconRes = R.drawable.ic_lock,
                        titleRes = R.string.keepalive_tasklock_title,
                        descRes = R.string.keepalive_tasklock_desc,
                        // 无法检测是否已锁定
                        onClick = { showTaskLockGuide() }
                    )
                )

            }
// 通知权限
            add(
                KeepItem(
                    iconRes = R.drawable.ic_notifications,
                    titleRes = R.string.keepalive_notification_title,
                    descRes = R.string.keepalive_notification_desc,

                    onClick = { openNotificationSettings() }
                )
            )

        }

        // 清空并重新创建所有保活卡片
        container.removeAllViews()

        keepItems.forEach { item ->
            item.viewId = View.generateViewId()
            val card = ExpandableCardView(requireContext()).apply {
                icon.setImageResource(item.iconRes)
                setTitle(context.getString(item.titleRes))
                setDescription(context.getString(item.descRes))
                setOnCardClickListener { item.onClick() }
                id = item.viewId
            }
            container.addView(card)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────────

    @SuppressLint("BatteryLife")
    private fun openBatteryOptimizationPage() {
        val ctx = requireContext()

        // 已经在白名单 → 仅提示
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
            ToastUtils.info(R.string.keepalive_battery_already)
            return
        }

        // ① 系统"电池优化"总列表（无需额外权限）
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

    /** 打开"应用详情"并给出提示 */
    private fun openAppDetailsFallback() {
        val ctx = requireContext()
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${ctx.packageName}".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            ToastUtils.info(R.string.keepalive_battery_tip_details)
        }.onFailure {
            ToastUtils.error(R.string.keepalive_battery_fail)
        }
    }

    /** 跳转到厂商的"自启动/后台弹性启动"设置界面；全部失败时退到本应用详情页 */
    private fun openAutoStartPage() {
        val ctx = requireContext()

        /* 不同 ROM 常见组件 */
        val candidateComponents = listOf(
            // MIUI
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),

            // EMUI / MagicOS
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),

            // OPPO / realme
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
                        "package:${ctx.packageName}".toUri()
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                ToastUtils.info(R.string.keepalive_autostart_tip)
            }.onFailure {
                ToastUtils.error(R.string.keepalive_autostart_fail)
            }
        }
    }

    /** 任务管理器"锁定"功能无法通过 Intent 直接跳转 → 弹提示 */
    private fun showTaskLockGuide() {
        ToastUtils.info(R.string.keepalive_tasklock_tip)
    }

    /** 打开通知设置页面 */
    private fun openNotificationSettings() {
        val ctx = requireContext()
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            startActivity(intent)
        }.onFailure {
            // 如果无法打开通知设置，则打开应用详情页
            openAppDetailsFallback()
        }
    }
}
