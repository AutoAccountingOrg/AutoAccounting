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
 * 引导页 #3 – 权限申请
 */
class IntroPagePermissionFragment : BaseIntroPageFragment<FragmentIntroPagePermissionBinding>() {



    // ──────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnNext.setOnClickListener { handleNext() }
        binding.btnBack.setOnClickListener {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.MODE
        }

    }


    data class PermItem(
        @DrawableRes val iconRes: Int,
        @StringRes val titleRes: Int,
        @StringRes val descRes: Int,
        val checkGranted: () -> Boolean,        // 检查权限的方法
        val onClick: () -> Unit,                 // 点击跳转的方法
        var viewId: Int = View.NO_ID
    )

    private lateinit var perms: MutableList<PermItem>
    private val projLauncher: ActivityResultLauncher<Unit> by lazy {
        ProjectionGateway.register(
            caller = this
        ) {

        }
    }

    private fun setupCardsDynamic() {
        val container = binding.cardGroup

        val ctx = requireContext()
        val isXposed = PrefManager.workMode == WorkMode.Xposed

        // 构建你的权限列表
        perms = mutableListOf<PermItem>().apply {
            add(
                PermItem(
                    iconRes = R.drawable.ic_overlay,
                    titleRes = R.string.perm_overlay_title,
                    descRes = R.string.perm_overlay_desc,
                    checkGranted = { FloatingWindowService.hasPermission() },
                    onClick = { FloatingWindowService.startPermissionActivity(ctx) }
                )
            )
            if (!isXposed) {
                add(
                    PermItem(
                        iconRes = R.drawable.ic_sms,
                        titleRes = R.string.perm_sms_title,
                        descRes = R.string.perm_sms_desc,
                        checkGranted = { SmsReceiver.hasPermission() },
                        onClick = { SmsReceiver.startPermissionActivity(requireActivity()) }
                    )
                )
                add(
                    PermItem(
                        iconRes = R.drawable.ic_notifications,
                        titleRes = R.string.perm_notification_title,
                        descRes = R.string.perm_notification_desc,
                        checkGranted = { NotificationService.hasPermission() },
                        onClick = { NotificationService.startPermissionActivity(ctx) }
                    )
                )
                /* === ③ 截屏读取权限（替换原 perm_storage） === */
                add(
                    PermItem(
                    iconRes = R.drawable.ic_screenshot,                 // 请准备一个截图图标
                    titleRes = R.string.perm_screenshot_title,          // "Screenshot Access"
                    descRes = R.string.perm_screenshot_desc,           // "Read screenshots for OCR"
                    checkGranted = {
                        ProjectionGateway.isReady()
                    },
                    onClick = {
                        projLauncher.launch(Unit)
                    }
                ))

                /* === ④ 应用使用情况访问（PACKAGE_USAGE_STATS）=== */
                add(
                    PermItem(
                    iconRes = R.drawable.ic_usage,                      // 可自定义一个隐私图标
                    titleRes = R.string.perm_usage_title,               // "Usage Access"
                    descRes = R.string.perm_usage_desc,                // "Detect foreground app for shake-to-capture"
                    checkGranted = { OcrService.hasPermission() },
                    onClick = { OcrService.startPermissionActivity(ctx) }
                ))

            } else {
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


        container.removeAllViews()

        perms.forEach { p ->
            p.viewId = View.generateViewId()
            // 从你的布局文件里 inflate 单个卡片（可以做一个 item_expandable_card.xml）
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

        setupCardsDynamic()

        // 刷新当前权限状态
        perms.forEach {
            val card = binding.cardGroup.findViewById<ExpandableCardView>(it.viewId)
            setState(card, it.checkGranted())
        }
    }


    /** “下一步” 按钮逻辑 */
    private fun handleNext() {
        // 所有权限都授予了才允许下一步
        val requiredGranted = perms.all { it.checkGranted() }

        if (requiredGranted) {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.KEEP
        } else {
            ToastUtils.error(R.string.perm_not_complete)   // “还有权限未授权”
        }
    }


    private fun openXposedManager() {
        ToastUtils.info(R.string.perm_xposed_tip)
    }


    private fun setState(cardView: ExpandableCardView, granted: Boolean) {
        val icon = cardView.findViewById<ImageView>(R.id.stateIcon)
        val ctx = cardView.context
        if (granted) {
            icon.setImageResource(R.drawable.ic_success)
            icon.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimary, 0)
            )
        } else {
            icon.setImageResource(R.drawable.ic_error)
            icon.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorError, 0)
            )
        }
    }
}
