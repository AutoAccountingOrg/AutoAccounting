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
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
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
import net.ankio.auto.ui.vm.PermissionSharedVm
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.xposed.XposedModule

/**
 * 引导页 #3 – 权限申请
 */
class IntroPagePermissionFragment : BaseIntroPageFragment<FragmentIntroPagePermissionBinding>() {

    private val permissionVm: PermissionSharedVm by viewModels()

    // ──────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCards(view)
        binding.btnNext.setOnClickListener { handleNext() }
    }

    override fun onResume() {
        super.onResume()

        // 根据工作模式隐藏/显示卡片
        if (PrefManager.workMode == WorkMode.Xposed) {
            binding.cardSms.visibility = View.GONE
            binding.cardNotification.visibility = View.GONE
        } else {
            binding.cardXposedModule.visibility = View.GONE
        }

        // 刷新当前权限状态
        val ctx = requireContext()
        permissionVm.cardOverlay.value = FloatingWindowService.hasPermission()
        permissionVm.cardSms.value = SmsReceiver.hasPermission()
        permissionVm.cardNotification.value = NotificationService.hasPermission()
        permissionVm.cardNetwork.value =
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED
        permissionVm.cardXposedModel.value = XposedModule.active()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UI & Permissions
    // ──────────────────────────────────────────────────────────────────────────────

    /** 绑定卡片 -> LiveData 及点击动作 */
    private fun setupCards(root: View) {
        val cardMap = mapOf(
            binding.cardOverlay to permissionVm.cardOverlay,
            binding.cardSms to permissionVm.cardSms,
            binding.cardNotification to permissionVm.cardNotification,
            binding.cardNetwork to permissionVm.cardNetwork,
            binding.cardXposedModule to permissionVm.cardXposedModel
        )

        val actions = mapOf(
            R.id.cardOverlay to { FloatingWindowService.startPermissionActivity(requireContext()) },
            R.id.cardSms to { SmsReceiver.startPermissionActivity(requireActivity()) },
            R.id.cardNotification to { NotificationService.startPermissionActivity(requireContext()) },
            R.id.cardNetwork to {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().packageName, null)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            R.id.cardXposedModule to { openXposedManager() }
        )

        // 观察权限状态并更新 UI
        cardMap.forEach { (card, liveData) ->
            liveData.observe(viewLifecycleOwner) { granted -> setState(card, granted) }
        }

        // 绑定点击事件
        actions.forEach { (cardId, action) ->
            root.findViewById<ExpandableCardView>(cardId).setOnCardClickListener { action() }
        }
    }

    /** “下一步” 按钮逻辑 */
    private fun handleNext() {
        val requiredGranted = when (PrefManager.workMode) {
            WorkMode.Xposed -> listOf(
                permissionVm.cardOverlay.value,
                permissionVm.cardNetwork.value,
                permissionVm.cardXposedModel.value
            )

            else -> listOf(
                permissionVm.cardOverlay.value,
                permissionVm.cardSms.value,
                permissionVm.cardNotification.value,
                permissionVm.cardNetwork.value
            )
        }.all { it == true }

        if (requiredGranted) {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.KEEP
        } else {
            ToastUtils.error(R.string.perm_not_complete)   // “还有权限未授权”
        }
    }

    /** 打开 Xposed/LSPosed 管理器，兼容暗码 & 显式组件 */
    private fun openXposedManager() {
        ToastUtils.info(R.string.perm_xposed_tip)
    }

    /** 根据授权结果切换图标 & 颜色 */
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
