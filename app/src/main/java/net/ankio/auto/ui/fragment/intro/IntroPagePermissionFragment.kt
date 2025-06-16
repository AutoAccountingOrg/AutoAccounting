package net.ankio.auto.ui.fragment.intro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.FragmentIntroPagePermissionBinding
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.service.NotificationService
import net.ankio.auto.service.SmsReceiver
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.ui.components.ExpandableCardView
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.xposed.XposedModule
import net.ankio.auto.service.OcrService
import net.ankio.auto.service.utils.ProjectionGateway

/**
 * 引导页 #3 – 权限申请
 * 该Fragment负责处理应用所需的各种权限申请，包括悬浮窗、短信、通知、截图等权限
 * 根据不同的工作模式（Xposed/普通）显示不同的权限申请项
 */
class IntroPagePermissionFragment : BaseIntroPageFragment<FragmentIntroPagePermissionBinding>() {

    // ──────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 设置下一步按钮点击事件
        binding.btnNext.setOnClickListener { handleNext() }
        // 设置返回按钮点击事件，返回到模式选择页面
        binding.btnBack.setOnClickListener {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.MODE
        }
    }

    /**
     * 权限项数据类
     * @param iconRes 权限图标资源ID
     * @param titleRes 权限标题资源ID
     * @param descRes 权限描述资源ID
     * @param checkGranted 检查权限是否已授予的方法
     * @param onClick 点击权限项时的处理方法
     * @param viewId 权限项视图ID
     */
    data class PermItem(
        @DrawableRes val iconRes: Int,
        @StringRes val titleRes: Int,
        @StringRes val descRes: Int,
        val checkGranted: () -> Boolean,        // 检查权限的方法
        val onClick: () -> Unit,                 // 点击跳转的方法
        var viewId: Int = View.NO_ID
    )

    // 权限项列表
    private lateinit var perms: MutableList<PermItem>

    // 屏幕投影权限请求启动器
    private val projLauncher: ActivityResultLauncher<Unit> by lazy {
        ProjectionGateway.register(
            caller = this
        ) {
            // 投影权限回调处理
        }
    }

    /**
     * 动态设置权限卡片
     * 根据当前工作模式（Xposed/普通）创建不同的权限申请项
     */
    private fun setupCardsDynamic() {
        val container = binding.cardGroup
        val ctx = requireContext()
        val isXposed = PrefManager.workMode == WorkMode.Xposed

        // 构建权限列表
        perms = mutableListOf<PermItem>().apply {
            // 添加悬浮窗权限
            add(
                PermItem(
                    iconRes = R.drawable.ic_overlay,
                    titleRes = R.string.perm_overlay_title,
                    descRes = R.string.perm_overlay_desc,
                    checkGranted = { FloatingWindowService.hasPermission() },
                    onClick = { FloatingWindowService.startPermissionActivity(ctx) }
                )
            )

            // 非Xposed模式下添加额外权限
            if (!isXposed) {
                // 短信权限
                add(
                    PermItem(
                        iconRes = R.drawable.ic_sms,
                        titleRes = R.string.perm_sms_title,
                        descRes = R.string.perm_sms_desc,
                        checkGranted = { SmsReceiver.hasPermission() },
                        onClick = { SmsReceiver.startPermissionActivity(requireActivity()) }
                    )
                )
                // 通知权限
                add(
                    PermItem(
                        iconRes = R.drawable.ic_notifications,
                        titleRes = R.string.perm_notification_title,
                        descRes = R.string.perm_notification_desc,
                        checkGranted = { NotificationService.hasPermission() },
                        onClick = { NotificationService.startPermissionActivity(ctx) }
                    )
                )
                // 截屏权限（用于OCR功能）
                add(
                    PermItem(
                        iconRes = R.drawable.ic_screenshot,
                        titleRes = R.string.perm_screenshot_title,
                        descRes = R.string.perm_screenshot_desc,
                    checkGranted = {
                        ProjectionGateway.isReady()
                    },
                    onClick = {
                        projLauncher.launch(Unit)
                    }
                ))

                // 应用使用情况访问权限
                add(
                    PermItem(
                        iconRes = R.drawable.ic_usage,
                        titleRes = R.string.perm_usage_title,
                        descRes = R.string.perm_usage_desc,
                    checkGranted = { OcrService.hasPermission() },
                    onClick = { OcrService.startPermissionActivity(ctx) }
                ))

            } else {
                // Xposed模式下添加Xposed框架权限
                add(
                    PermItem(
                        iconRes = R.drawable.xposed_framework_icon,
                        titleRes = R.string.perm_xposed_title,
                        descRes = R.string.perm_xposed_desc,
                        checkGranted = { XposedModule.active() },
                        onClick = { openXposedManager() }
                    )
                )
            }

            // 网络权限（所有模式都需要）
            add(
                PermItem(
                    iconRes = R.drawable.ic_network,
                    titleRes = R.string.perm_network_title,
                    descRes = R.string.perm_network_desc,
                    checkGranted = {
                        ContextCompat.checkSelfPermission(
                            ctx, Manifest.permission.ACCESS_NETWORK_STATE
                        ) == PackageManager.PERMISSION_GRANTED
                    },
                    onClick = {
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", ctx.packageName, null)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            )
        }

        // 清空并重新创建所有权限卡片
        container.removeAllViews()

        perms.forEach { p ->
            p.viewId = View.generateViewId()
            val card = ExpandableCardView(requireContext()).apply {
                icon.setImageResource(p.iconRes)
                setTitle(context.getString(p.titleRes))
                setDescription(context.getString(p.descRes))
                setOnCardClickListener { p.onClick() }
                id = p.viewId
            }
            container.addView(card)
        }
    }

    override fun onResume() {
        super.onResume()
        // 重新设置权限卡片并刷新状态
        setupCardsDynamic()
        // 刷新所有权限的授予状态
        perms.forEach {
            val card = binding.cardGroup.findViewById<ExpandableCardView>(it.viewId)
            setState(card, it.checkGranted())
        }
    }

    /**
     * 处理"下一步"按钮点击事件
     * 检查所有必需权限是否已授予，只有全部授予才允许进入下一步
     */
    private fun handleNext() {
        val requiredGranted = perms.all { it.checkGranted() }

        if (requiredGranted) {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.KEEP
        } else {
            ToastUtils.error(R.string.perm_not_complete)
        }
    }

    /**
     * 打开Xposed管理器
     */
    private fun openXposedManager() {
        ToastUtils.info(R.string.perm_xposed_tip)
    }

    /**
     * 设置权限卡片的状态显示
     * @param cardView 权限卡片视图
     * @param granted 权限是否已授予
     */
    private fun setState(cardView: ExpandableCardView, granted: Boolean) {
        val icon = cardView.findViewById<ImageView>(R.id.stateIcon)
        val ctx = cardView.context
        if (granted) {
            // 已授予权限，显示成功图标
            icon.setImageResource(R.drawable.ic_success)
            icon.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimary, 0)
            )
        } else {
            // 未授予权限，显示错误图标
            icon.setImageResource(R.drawable.ic_error)
            icon.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorError, 0)
            )
        }
    }
}
