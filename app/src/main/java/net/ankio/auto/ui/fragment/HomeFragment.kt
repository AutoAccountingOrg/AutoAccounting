/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.ui.fragment

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.AboutDialogBinding
import net.ankio.auto.databinding.FragmentHomeBinding
import net.ankio.auto.exceptions.ServiceCheckException
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.AppDialog
import net.ankio.auto.ui.dialog.AssetsSelectorDialog
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.viewBinding
import net.ankio.auto.update.AppUpdate
import net.ankio.auto.update.RuleUpdate
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.xposed.common.ActiveInfo
import net.ankio.auto.xposed.common.ServerInfo
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.SettingModel
import rikka.html.text.toHtml


/**
 * 主页
 */
class HomeFragment : BaseFragment() {
    override val binding: FragmentHomeBinding by viewBinding(FragmentHomeBinding::inflate)

    /**
     * 关于对话框
     */
    private var aboutDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = binding.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupEvents()
        checkInitialServices()
    }

    private fun setupUI() {
        setupCards()
        refreshUI()
    }

    private fun setupCards() {
        val surfaceColor = SurfaceColors.SURFACE_1.getColor(requireContext())
        listOf(binding.infoCard, binding.groupCard, binding.ruleCard, binding.donateCard)
            .forEach { it.setCardBackgroundColor(surfaceColor) }
    }

    private fun setupEvents() {
        setupMenuEvents()
        setupActiveEvents()
        setupBookAppEvents()
        setupRuleEvents()
        setupCommunicationEvents()
        setupDonateEvents()
        setupResourceEvents()
    }

    private fun setupResourceEvents() {
        binding.msgDocButton.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.msg_doc_url))
        }
        binding.msgPanButton.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.msg_pan_url))
        }
    }

    private fun setupDonateEvents() {
        binding.closeDonate.setOnClickListener {
            ConfigUtils.putLong(Setting.DONATE_TIME, System.currentTimeMillis())
            bindDonateUI()
        }

        binding.donateWechat.setOnClickListener {
            val uri =
                "https://pic.dreamn.cn/uPic/2023_04_23_00_41_49_1682181709_1682181709722_KGWAI6.jpg"
            CustomTabsHelper.launchUrlOrCopy(requireContext(), uri)
            ToastUtils.info(R.string.copy_donate_qr)
        }

        binding.donateAlipay.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://qr.alipay.com/fkx15657xcegbz5k9zxnd30")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun checkInitialServices() {
        lifecycleScope.launch {
            runCatching {
                checkServices()
            }.onFailure { e ->
                if (e is ServiceCheckException) {
                    showServiceErrorDialog(e)
                } else {
                    Logger.e("Error in check service", e)
                }
            }
        }
    }

    private fun showServiceErrorDialog(error: ServiceCheckException) {
        aboutDialog?.dismiss()
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(error.title)
            .setMessage(error.msg)
            .setPositiveButton(error.btn) { _, _ ->
                if (isAdded) {
                    lifecycleScope.launch {
                        error.doAction(requireActivity())
                    }
                }
            }
        //.show()
        if (error.dismissBtn != null) {
            builder.setNegativeButton(error.dismissBtn) { _, _ ->
                if (isAdded) {
                    lifecycleScope.launch {
                        error.dismissAction?.invoke(requireActivity())
                    }
                }
            }
        }
        aboutDialog = builder.show()
    }

    private fun setupMenuEvents() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.title_log -> {
                    findNavController().navigate(R.id.logFragment)
                    true
                }

                R.id.title_more -> {
                    showAboutDialog()
                    true
                }

                else -> false
            }
        }
    }

    private fun showAboutDialog() {
        aboutDialog?.dismiss()

        val dialogBinding = AboutDialogBinding.inflate(LayoutInflater.from(requireContext()))
        with(dialogBinding) {
            sourceCode.apply {
                movementMethod = LinkMovementMethod.getInstance()
                text = getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"https://github.com/AutoAccountingOrg/AutoAccounting\">GitHub</a></b>"
                ).toHtml()
            }
            versionName.text = BuildConfig.VERSION_NAME
        }

        aboutDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()
            .also { it.show() }
    }

    private fun setupActiveEvents() {
        binding.active.setOnClickListener {
            lifecycleScope.launch {
                checkAppUpdate(true)
            }
        }
    }

    private fun setupBookAppEvents() {
        val themeContext = App.getThemeContext(requireContext())

        with(binding) {
            bookAppContainer.setOnClickListener {
                showAppDialog()
            }

            map.setOnClickListener {
                findNavController().navigate(R.id.assetMapFragment)
            }

            readAssets.setOnClickListener {
                showAssetsDialog(themeContext)
            }

            book.setOnClickListener {
                showBookDialog(themeContext)
            }

            readCategory.setOnClickListener {
                showCategoryDialog(themeContext)
            }
        }
    }

    private fun showAppDialog() {
        AppDialog(requireContext()) {
            App.runOnUiThread {
                bindBookAppUI()
            }
        }.show(false)
    }

    private fun showAssetsDialog(themeContext: Context) {
        AssetsSelectorDialog(themeContext) { asset ->
            Logger.d("Choose Asset: ${asset.name}")
        }.showInFragment(this, cancel = true)
    }

    private fun showBookDialog(themeContext: Context) {
        BookSelectorDialog(themeContext) { book, _ ->
            Logger.d("Choose Book: ${book.name}")
            ConfigUtils.putString(Setting.DEFAULT_BOOK_NAME, book.name)
            App.runOnUiThread {
                bindBookAppUI()
            }
        }.showInFragment(this, cancel = true)
    }

    private fun showCategoryDialog(themeContext: Context) {
        BookSelectorDialog(themeContext, true) { book, type ->
            CategorySelectorDialog(
                themeContext,
                book.remoteId,
                type
            ) { category1, category2 ->
                Logger.d("Book: ${book.name}, Type: $type, Choose Category：${category1?.name ?: ""} - ${category2?.name ?: ""}")
            }.showInFragment(this, cancel = true)
        }.showInFragment(this, cancel = true)
    }

    private fun setupRuleEvents() {
        with(binding) {
            categoryMap.setOnClickListener {
                findNavController().navigate(R.id.categoryMapFragment)
            }

            categoryEdit.setOnClickListener {
                findNavController().navigate(R.id.categoryRuleFragment)
            }

            checkRuleUpdate.apply {
                setOnClickListener {
                    checkRuleUpdateWithToast()
                }

                setOnLongClickListener {
                    forceCheckRuleUpdate()
                    true
                }
            }
        }
    }

    private fun checkRuleUpdateWithToast() {
        ToastUtils.info(R.string.check_update)
        lifecycleScope.launch {
            checkRuleUpdate(true)
        }
    }

    private fun forceCheckRuleUpdate() {
        ConfigUtils.putString(Setting.RULE_VERSION, "")
        checkRuleUpdateWithToast()
    }

    private fun setupCommunicationEvents() {
        with(binding) {
            msgGeekbar.setOnClickListener {
                launchUrl(getString(R.string.geekbar_uri))
            }

            msgTelegram.setOnClickListener {
                launchUrl(getString(R.string.telegram_url))
            }

            msgQq.setOnClickListener {
                launchUrl(getString(R.string.qq_url))
            }
        }
    }

    private fun launchUrl(url: String) {
        CustomTabsHelper.launchUrlOrCopy(requireContext(), url)
    }

    /**
     * 检查服务
     */
    private suspend fun checkServices() {
        //if (!isUiReady()) return
        ServerInfo.isServerStart(requireContext())
        //悬浮窗权限
        checkFloatPermission()
        // 检查记账软件
        checkBookApp()
        // 检查软件和规则更新
        checkUpdate()
    }

    private suspend fun checkUpdate(showResult: Boolean = false) {
        if (ConfigUtils.getBoolean(Setting.CHECK_RULE_UPDATE, true)) {
            checkRuleUpdate(showResult)
        }
        if (ConfigUtils.getBoolean(Setting.CHECK_APP_UPDATE, true)) {
            checkAppUpdate()
        }
    }

    private fun checkFloatPermission() {
        if (!Settings.canDrawOverlays(context)) {
            throw ServiceCheckException(
                getString(R.string.title_no_permission),
                getString(R.string.no_permission_float_window),
                getString(R.string.btn_set_permission), action = {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                })
        }
    }


    /**
     * 检查记账软件
     */
    private fun checkBookApp() {
        if (ConfigUtils.getString(Setting.BOOK_APP_ID, "").isEmpty()) {
            throw ServiceCheckException(
                getString(R.string.title_no_book_app),
                getString(R.string.no_book_app),
                getString(R.string.btn_set_book_app), action = {
                    val appDialog = AppDialog(requireContext()) {
                        bindBookAppUI()
                    }
                    appDialog.showInFragment(this, cancel = true)
                })
        }
    }

    /**
     * 刷新UI
     */
    private fun refreshUI() {
        //   if (!isUiReady()) return
        lifecycleScope.launch {
            bindActiveUI()
        }
        bindBookAppUI()
        bindRuleUI()
        bindDonateUI()
    }

    private fun bindDonateUI() {
        val donateTime = ConfigUtils.getLong(Setting.DONATE_TIME, 0)
        val oneYearInMillis = 365L * 24 * 60 * 60 * 1000 // 每年的毫秒数
        val now = System.currentTimeMillis()

        // 检查条件：首次显示，或者已经过了一年
        val shouldShowDonate = donateTime == 0L || now - donateTime > oneYearInMillis

        binding.donateCard.visibility = if (shouldShowDonate) View.VISIBLE else View.GONE

    }


    /**
     * 绑定记账软件数据部分的UI
     */
    private fun bindBookAppUI() {
        binding.book.visibility =
            if (ConfigUtils.getBoolean(
                    Setting.SETTING_BOOK_MANAGER,
                    true
                )
            ) View.VISIBLE else View.GONE
        binding.assets.visibility =
            if (ConfigUtils.getBoolean(
                    Setting.SETTING_ASSET_MANAGER,
                    true
                )
            ) View.VISIBLE else View.GONE
        ConfigUtils.getString(Setting.BOOK_APP_ID, "").apply {
            if (this.isEmpty()) {
                binding.bookApp.text = getString(R.string.no_setting)
            } else {
                App.getAppInfoFromPackageName(this)?.apply {
                    binding.bookApp.text = this[0] as String
                }
            }
        }

        val bookName = ConfigUtils.getString(Setting.DEFAULT_BOOK_NAME, "")
        if (bookName.isEmpty()) {
            lifecycleScope.launch {
                val book = BookNameModel.getFirstBook()
                // if (!isUiReady()) return@launch
                ConfigUtils.putString(Setting.DEFAULT_BOOK_NAME, book.name)
                binding.defaultBook.text = book.name
            }
        } else {
            binding.defaultBook.text = bookName
        }
    }

    /**
     * 绑定激活部分的UI
     */
    private fun bindActiveUI() {
        //  if (!isUiReady()) return
        val colorPrimary =
            App.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary)

        if (!ActiveInfo.isModuleActive()) {
            setActive(
                SurfaceColors.SURFACE_3.getColor(requireContext()),
                colorPrimary,
                R.drawable.home_active_error,
            )
        } else {
            setActive(
                colorPrimary,
                App.getThemeAttrColor(
                    com.google.android.material.R.attr.colorOnPrimary,
                ),
                R.drawable.home_active_success,
            )
        }
    }


    /**
     * 绑定规则部分的UI
     */
    private fun bindRuleUI() {
        // if (!isUiReady()) return
        lifecycleScope.launch {
            SettingModel.get(Setting.RULE_VERSION, "None").let {
                //if (!isUiReady()) return@launch
                binding.ruleVersion.text = it
            }
        }
    }

    /**
     * 检查规则更新
     */
    private suspend fun checkRuleUpdate(showResult: Boolean) {
        //   if (!isUiReady()) return
        val ruleUpdate = RuleUpdate(requireContext())
        runCatching {
            if (ruleUpdate.check(showResult)) {

                val updateDialog = UpdateDialog(requireContext(), ruleUpdate) {
                    App.runOnUiThread {
                        bindRuleUI()
                    }
                }
                updateDialog.showInFragment(this, cancel = true)
                //     dialogs.add(updateDialog)
            }
        }.onFailure {
            Logger.e("checkRuleUpdate", it)
        }
    }

    /**
     * 检查应用更新
     */
    private suspend fun checkAppUpdate(showResult: Boolean = false) {
        //    if (!isUiReady()) return
        val appUpdate = AppUpdate(requireContext())
        runCatching {
            if (appUpdate.check(showResult)) {
                val updateDialog = UpdateDialog(requireContext(), appUpdate) {
                    App.runOnUiThread {
                        bindRuleUI()
                    }
                }
                updateDialog.showInFragment(this, cancel = true)
                //      dialogs.add(updateDialog)
            }
        }.onFailure {
            Logger.e("checkAppUpdate", it)
        }
    }

    /**
     * 设置激活状态
     */
    private fun setActive(
        @ColorInt backgroundColor: Int,
        @ColorInt textColor: Int,
        @DrawableRes drawable: Int,
    ) {
        binding.active.setBackgroundColor(backgroundColor)
        binding.imageView.setImageDrawable(
            AppCompatResources.getDrawable(
                requireContext(),
                drawable,
            ),
        )
        val versionName = BuildConfig.VERSION_NAME
        val names = versionName.split(" - ")
        binding.msgLabel.text = names[0].trim()
        lifecycleScope.launch {
            ActiveInfo.getFramework().let {
                binding.msgLabel2.text = it
            }
        }
        binding.imageView.setColorFilter(textColor)
        binding.msgLabel.setTextColor(textColor)
        binding.msgLabel2.setTextColor(textColor)
    }

    override fun beforeViewBindingDestroy() {
        binding.toolbar.setOnMenuItemClickListener(null)
    }

    override fun onDestroyView() {
        aboutDialog?.dismiss()
        aboutDialog = null
        // 关闭所有对话框

        super.onDestroyView()
    }
}


