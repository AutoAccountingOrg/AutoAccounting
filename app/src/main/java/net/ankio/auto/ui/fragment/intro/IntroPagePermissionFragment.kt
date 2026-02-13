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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.FragmentIntroPagePermissionBinding
import net.ankio.auto.service.NotificationService
import net.ankio.auto.service.OcrAccessibilityService
import net.ankio.auto.service.SmsReceiver
import net.ankio.auto.service.ocr.OcrTools
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.ui.components.ExpandableCardView
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.xposed.XposedModule
import net.ankio.shell.Shell
import net.ankio.auto.service.OverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 引导页 #3 – 权限申请
 * 该Fragment负责处理应用所需的各种权限申请，包括悬浮窗、短信、通知、截图等权限
 * 根据不同的工作模式（Xposed/普通）显示不同的权限申请项
 */
class IntroPagePermissionFragment : BaseIntroPageFragment<FragmentIntroPagePermissionBinding>() {

    // ──────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
     * @param isRequired 是否为必需权限，false表示可选权限
     * @param viewId 权限项视图ID
     */
    data class PermItem(
        @DrawableRes val iconRes: Int,
        @StringRes val titleRes: Int,
        @StringRes val descRes: Int,
        val checkGranted: () -> Boolean,        // 检查权限的方法
        val onClick: () -> Unit,                 // 点击跳转的方法
        val isRequired: Boolean = true,          // 是否为必需权限，默认为必需
        var viewId: Int = View.NO_ID
    )

    // 权限项列表：默认初始化为空，避免 lateinit 未初始化访问导致崩溃
    private var perms: MutableList<PermItem> = mutableListOf()

    /**
     * 动态设置权限卡片
     * 根据当前工作模式（Xposed/普通）创建不同的权限申请项
     */
    private fun setupCardsDynamic() {
        val container = binding.cardGroup
        val ctx = requireContext()

        // 构建权限列表
        perms = mutableListOf<PermItem>().apply {
            // 添加悬浮窗权限
            add(
                PermItem(
                    iconRes = R.drawable.ic_overlay,
                    titleRes = R.string.perm_overlay_title,
                    descRes = R.string.perm_overlay_desc,
                    checkGranted = { OverlayService.hasPermission() },
                    onClick = { OverlayService.startPermissionActivity(ctx) }
                )
            )

            // OCR识别（权限）：3种卡片，按模式只显示对应的一种
            // OCR 权限（可选）：用户可跳过，后续在设置中配置
            if (WorkMode.isXposed()) {
                // Xposed 模式：Root 卡片
                add(
                    PermItem(
                        iconRes = R.drawable.icon_proactive,
                        titleRes = R.string.perm_ocr_perm_root,
                        descRes = R.string.ocr_auth_root_description,
                        checkGranted = {
                            try {
                                Shell(ctx.packageName).use { it.rootPermission() }
                            } catch (_: Throwable) {
                                false
                            }
                        },
                        onClick = { OcrTools.reqRoot() },
                        isRequired = false
                    )
                )
            } else if (WorkMode.isLSPatch()) {
                // LSPatch 模式：Shizuku 卡片
                add(
                    PermItem(
                        iconRes = R.drawable.icon_proactive,
                        titleRes = R.string.perm_ocr_perm_shizuku,
                        descRes = R.string.ocr_auth_shizuku_description,
                        checkGranted = {
                            try {
                                Shell(ctx.packageName).use { it.shizukuPermission() }
                            } catch (_: Throwable) {
                                false
                            }
                        },
                        onClick = { OcrTools.reqShizuku() },
                        isRequired = false
                    )
                )
            } else {
                // OCR 模式：无障碍卡片
                add(
                    PermItem(
                        iconRes = R.drawable.icon_proactive,
                        titleRes = R.string.perm_ocr_perm_accessibility,
                        descRes = R.string.ocr_auth_accessibility_description,
                        checkGranted = { OcrAccessibilityService.instance != null },
                        onClick = { OcrTools.reqAccessibility() },
                        isRequired = false
                    )
                )
            }

            // 非Xposed模式下添加可选权限
            if (!WorkMode.isXposed()) {
                // 短信权限（可选）
                add(
                    PermItem(
                        iconRes = R.drawable.ic_sms,
                        titleRes = R.string.perm_sms_title,
                        descRes = R.string.perm_sms_desc_optional,
                        checkGranted = { SmsReceiver.hasPermission() },
                        onClick = { SmsReceiver.startPermissionActivity(requireActivity()) },
                        isRequired = false
                    )
                )

                // 通知权限（OCR模式下为可选）
                add(
                    PermItem(
                        iconRes = R.drawable.ic_notifications,
                        titleRes = R.string.perm_notification_title,
                        descRes = R.string.perm_notification_desc_optional,
                        checkGranted = { NotificationService.hasPermission() },
                        onClick = { NotificationService.startPermissionActivity(ctx) },
                        isRequired = false
                    )
                )

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

                // 为可选权限添加"（可选）"标识
                val titleText = if (p.isRequired) {
                    context.getString(p.titleRes)
                } else {
                    "${context.getString(p.titleRes)}（${context.getString(R.string.perm_optional)}）"
                }
                setTitle(titleText)
                setDescription(context.getString(p.descRes))
                setOnCardClickListener { p.onClick() }
                id = p.viewId
            }
            container.addView(card)
        }
    }

    override fun onResume() {
        super.onResume()
        // 构建权限卡片，确保 perms 已初始化，避免按钮点击过早导致崩溃
        setupCardsDynamic()
        // 权限检查可能阻塞（Shell 的 root/shizuku 探测会 spawn 进程或 IPC），放到 IO 协程执行，
        // 避免在主线程阻塞导致从模式页切换过来时卡顿
        viewLifecycleOwner.lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                perms.map { it.checkGranted() to it.isRequired }
            }
            if (!isAdded) return@launch
            perms.forEachIndexed { i, p ->
                val card = binding.cardGroup.findViewById<ExpandableCardView>(p.viewId)
                if (i < results.size && card != null) {
                    val (granted, required) = results[i]
                    setState(card, granted, required)
                }
            }
        }
    }

    /**
     * 处理"下一步"按钮点击事件
     * 检查所有必需权限是否已授予，只有必需权限全部授予才允许进入下一步
     * 可选权限不影响进入下一步
     */
    private fun handleNext() {
        val requiredPerms = perms.filter { it.isRequired }
        val requiredGranted = requiredPerms.all { it.checkGranted() }

        val optionalPerms = perms.filter { !it.isRequired }
        val optionalGrantedCount = optionalPerms.count { it.checkGranted() }

        if (requiredGranted) {
            // 如果有可选权限未授予，给出提示但仍允许继续
            if (optionalGrantedCount < optionalPerms.size) {
                val missingOptional = optionalPerms.size - optionalGrantedCount
                ToastUtils.info(getString(R.string.perm_optional_missing_hint, missingOptional))
            }
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.KEEP
        } else {
            ToastUtils.error(R.string.perm_required_not_complete)
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
     * @param isRequired 是否为必需权限
     */
    private fun setState(
        cardView: ExpandableCardView,
        granted: Boolean,
        isRequired: Boolean = true
    ) {
        val icon = cardView.findViewById<ImageView>(R.id.stateIcon)
        val ctx = cardView.context
        when {
            granted -> {
                // 已授予权限，显示成功图标
                icon.setImageResource(R.drawable.ic_success)
                icon.imageTintList = ColorStateList.valueOf(DynamicColors.Primary)
            }

            isRequired -> {
                // 必需权限未授予，显示错误图标
                icon.setImageResource(R.drawable.ic_error)
                icon.imageTintList = ColorStateList.valueOf(DynamicColors.Error)
            }

            else -> {
                // 可选权限未授予，显示警告图标
                icon.setImageResource(R.drawable.ic_warning)
                icon.imageTintList = ColorStateList.valueOf(DynamicColors.Tertiary)
            }
        }
    }
}
