package net.ankio.auto.ui.fragment.intro

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.viewModels
import com.google.android.material.color.MaterialColors
import net.ankio.auto.R
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.FragmentIntroPage3Binding
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.service.NotificationService
import net.ankio.auto.service.SmsReceiver
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.components.ExpandableCardView
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.vm.PermissionSharedVm
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.xposed.XposedModule

class IntroPage3Fragment : BaseFragment<FragmentIntroPage3Binding>() {

    private val permissionVm: PermissionSharedVm by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) 准备卡片和对应的 LiveData、点击动作映射
        val cardToLiveData = mapOf(
            binding.cardOverlay to permissionVm.cardOverlay,
            binding.cardSms to permissionVm.cardSms,
            binding.cardNotification to permissionVm.cardNotification,
            binding.cardNetwork to permissionVm.cardNetwork,
            binding.cardXposedModule to permissionVm.cardXposedModel
        )
        val cardActions: Map<Int, () -> Unit> = mapOf(
            R.id.cardOverlay to { FloatingWindowService.startPermissionActivity(requireContext()) },
            R.id.cardSms to { SmsReceiver.startPermissionActivity(requireActivity()) },
            R.id.cardNotification to { NotificationService.startPermissionActivity(requireContext()) },
            R.id.cardNetwork to {
                requireContext().startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().packageName, null)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            R.id.cardXposedModule to { openXposedManager() }
        )

        // 2) 观察所有 LiveData，统一调用 setState
        cardToLiveData.forEach { (card, liveData) ->
            liveData.observe(viewLifecycleOwner) { granted ->
                setState(card, granted)
            }
        }

        // 3) 绑定卡片点击事件，展开时再触发授权逻辑
        cardActions.forEach { (cardId, action) ->
            view.findViewById<ExpandableCardView>(cardId).setOnCardClickListener {
                action()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PrefManager.introIndex = 2

        // 根据工作模式隐藏不必要的卡片
        if (PrefManager.workMode == WorkMode.Xposed) {
            binding.cardSms.visibility = View.GONE
            binding.cardNotification.visibility = View.GONE
        } else {
            binding.cardXposedModule.visibility = View.GONE
        }

        // 刷新所有权限状态到 ViewModel
        val ctx = requireContext()
        permissionVm.cardOverlay.value = FloatingWindowService.hasPermission()
        permissionVm.cardSms.value = SmsReceiver.hasPermission()
        permissionVm.cardNotification.value = NotificationService.hasPermission()
        permissionVm.cardNetwork.value = (ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_NETWORK_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED)
        permissionVm.cardXposedModel.value = XposedModule.active()
    }

    /** 授权／跳转相关方法 */
    private fun requestOverlayPermission() {
        val ctx = requireContext()
        if (!Settings.canDrawOverlays(ctx)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}")
                )
            )
            ToastUtils.info(R.string.perm_overlay_tip)
        }
    }

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS),
            REQUEST_SMS_PERM
        )
    }

    private fun requestNotificationAccess() {
        val ctx = requireContext()
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        )
    }

    private fun openXposedManager() {
        val secret = "*#*#5776733#*#"
        val uri = Uri.parse("tel:" + Uri.encode(secret))

        val dialIntent = Intent(Intent.ACTION_DIAL, uri)
        startActivity(dialIntent)
        ToastUtils.info(R.string.perm_xposed_tip)

    }

    /** 根据授权结果切换图标 & 颜色 */
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

    companion object {
        private const val REQUEST_SMS_PERM = 1001
    }
}
