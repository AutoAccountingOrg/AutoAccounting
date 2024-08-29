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

import android.content.BroadcastReceiver
import android.os.Bundle
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
import net.ankio.auto.broadcast.LocalBroadcastHelper
import net.ankio.auto.common.AccountingConfig
import net.ankio.auto.common.ActiveInfo
import net.ankio.auto.common.ServerInfo
import net.ankio.auto.databinding.AboutDialogBinding
import net.ankio.auto.databinding.FragmentHomeBinding
import net.ankio.auto.models.CategoryModel
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.AssetsSelectorDialog
import net.ankio.auto.ui.dialog.BookInfoDialog
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.update.RuleUpdate
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import rikka.html.text.toHtml

/**
 * 主页
 */
class HomeFragment : BaseFragment() {
    private lateinit var binding: FragmentHomeBinding
    override val menuList: ArrayList<MenuItem> =
        arrayListOf(
            MenuItem(R.string.title_log, R.drawable.menu_item_log) {
                it.navigate(R.id.logFragment)
            },
            MenuItem(R.string.title_setting, R.drawable.menu_item_setting) {
                it.navigate(R.id.setting2Fragment)
            },
            MenuItem(R.string.title_more, R.drawable.menu_item_more) {
                val binding =
                    AboutDialogBinding.inflate(LayoutInflater.from(requireContext()), null, false)
                binding.sourceCode.movementMethod = LinkMovementMethod.getInstance()
                binding.sourceCode.text =
                    getString(
                        R.string.about_view_source_code,
                        "<b><a href=\"https://github.com/AutoAccountingOrg/AutoAccounting\">GitHub</a></b>",
                    ).toHtml()
                binding.versionName.text = AppUtils.getVersionName()
                MaterialAlertDialogBuilder(requireContext())
                    .setView(binding.root)
                    .show()
            },
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        bindingActiveEvents()

        bindBookAppEvents()

        bindRuleEvents()

        bindingCommunicationEvents()

        // 卡片部分颜色设置

        val cards = listOf(
                binding.infoCard,
                binding.groupCard,
                binding.ruleCard,
            )
        val color = SurfaceColors.SURFACE_1.getColor(requireContext())
        cards.forEach { it.setCardBackgroundColor(color) }

        scrollView = binding.scrollView

        // 检查记账软件
        checkBookApp()

        // app启动时检查自动记账服务的连通性
        checkAutoService()

        // 检查软件和规则更新
        if (!BuildConfig.DEBUG) checkUpdate()

        return binding.root
    }

    /**
     * 检查自动记账服务
     */
    private fun checkAutoService(){

        lifecycleScope.launch {
            if (!ServerInfo.isServerStart()){
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.title_cant_connect_service)
                    .setMessage(ServerInfo.getServerErrorMsg(requireContext()))
                    .show()
            }
        }
    }


    /**
     * 检查记账软件
     */
    private fun checkBookApp() {
        if (SpUtils.getString("bookApp", "").isEmpty()) {

            //从array.xml中获取数据
            val appList = resources.getStringArray(R.array.apps)
            for (app in appList) {

                if (App.isAppInstalled(app)) {
                    SpUtils.putString("bookApp", app)
                    break
                }
            }

            if (SpUtils.getString("bookApp", "").isEmpty()) {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.title_book_app)
                    .setMessage(R.string.msg_book_app)
                    .setPositiveButton(R.string.sure_book_app) { _, _ ->
                        CustomTabsHelper.launchUrlOrCopy(requireActivity(), getString(R.string.book_app_url))
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        // finish()
                    }
                    .show()
            }

        }
    }

    /**
     * 刷新UI
     */
    private fun refreshUI() {
        bindActiveUI()
        bindBookAppUI()
        bindRuleUI()
    }

    /**
     * 绑定记账软件数据部分的UI
     */
    private fun bindBookAppUI() {
        val config = AccountingConfig.get()
        binding.book.visibility = if (config.multiBooks) View.VISIBLE else View.GONE
        binding.assets.visibility = if (config.assetManagement) View.VISIBLE else View.GONE
        SpUtils.getString("bookApp", "").apply {
            if (this.isEmpty()) {
                binding.bookApp.text = getString(R.string.no_setting)
            } else {
                AppUtils.getAppInfoFromPackageName(this, AppUtils.getApplication())?.apply {
                    binding.bookApp.text = this.name
                }
            }
        }
        SpUtils.getString("defaultBook", "默认账本").let {
            binding.defaultBook.text = it
        }
    }

    /**
     * 绑定激活部分的UI
     */
    private fun bindActiveUI() {
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
        val ruleVersion = SpUtils.getString("rule_version", "None")
        binding.ruleVersion.text = ruleVersion


            // TODO 这里的规则需要重写入口



    }

    /**
     * 本地广播
     */
    private lateinit var broadcastReceiver: BroadcastReceiver

    /**
     * 绑定规则部分的事件
     */
    private fun bindRuleEvents() {

        broadcastReceiver = LocalBroadcastHelper.registerReceiver(LocalBroadcastHelper.ACTION_UPDATE_FINISH) {a,b->
            refreshUI()
        }

        binding.customCategory.setOnClickListener {
            findNavController().navigate(R.id.ruleFragment)
        }
        binding.checkRuleUpdate.setOnClickListener {
            ToastUtils.info(R.string.check_update)
            lifecycleScope.launch {
                checkRuleUpdate(true)
            }
        }

        binding.ruleLibrary.setOnClickListener {
            findNavController().navigate(R.id.dataRuleFragment)
        }


    }

    /**
     * 检查规则更新
     */
    private suspend fun checkRuleUpdate( showResult: Boolean) {
        val ruleUpdate = RuleUpdate(requireContext())
        if (ruleUpdate.check(showResult)){
            UpdateDialog(requireActivity(), ruleUpdate).show(false)
        }
    }


    /**
     * 销毁时注销广播
     */
    override fun onDestroy() {
        super.onDestroy()
        if (this::broadcastReceiver.isInitialized) {
            LocalBroadcastHelper.unregisterReceiver(broadcastReceiver)
        }
    }

    /**
     * 绑定记账软件数据部分的事件
     */
    private fun bindBookAppEvents() {
        /**
         * 获取主题Context，部分弹窗样式不含M3主题
         */
        val themeContext = App.getThemeContext(requireContext())
        binding.bookAppContainer.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.book_app_url))
        }
        // 资产映射
        binding.map.setOnClickListener {
            // 切换到MapFragment
            findNavController().navigate(R.id.mapFragment)
        }
        // 资产管理（只读）
        binding.readAssets.setOnClickListener {
            AssetsSelectorDialog(themeContext) {
                Logger.i("选择的资产是：${it.name}")
            }.show(cancel = true)
        }
        // 账本数据（只读）
        binding.defaultBook.setOnClickListener {
            BookSelectorDialog(themeContext) {
                Logger.d("选择的账本是：${it.name}")
                // defaultBook
                SpUtils.putString("defaultBook", it.name)
            }.show(cancel = true)
        }
        // 分类数据（只读）
        binding.readCategory.setOnClickListener {
            BookSelectorDialog(themeContext) {
                BookInfoDialog(themeContext, it) { type ->
                    CategorySelectorDialog(themeContext, it.id, type) { categoryModel1: CategoryModel?, categoryModel2: CategoryModel? ->
                        Logger.i("选择的分类是：${categoryModel1?.name ?: ""} - ${categoryModel2?.name ?: ""}")
                    }.show(cancel = true)
                }.show(cancel = true)
            }.show(cancel = true)
        }

    }

    /**
     * 激活页面的事件
     */
    private fun bindingActiveEvents() {
        binding.active.setOnClickListener {

            if (!ActiveInfo.isModuleActive()) {
              //TODO 跳转帮助文档
            }

            //  findNavController().navigate(R.id.serviceFragment)
        }
    }

    /**
     * 自动记账讨论社区
     */
    private fun bindingCommunicationEvents() {
        binding.msgGeekbar.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.geekbar_uri))
        }

        binding.msgTelegram.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.telegram_url))
        }

        binding.msgQq.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.qq_url))
        }
    }

    /**
     * onResume时刷新UI
     */
    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    /**
     * 检查更新
     */
    private  fun checkUpdate(showResult: Boolean = false) {
        if (SpUtils.getBoolean("setting_rule", true)) {
           lifecycleScope.launch {
               checkRuleUpdate(showResult)
           }
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
                requireActivity(),
                drawable,
            ),
        )
        val versionName = BuildConfig.VERSION_NAME
        val names = versionName.split(" - ")
        binding.msgLabel.text = names[0].trim()
        binding.msgLabel2.text = ActiveInfo.getFramework()
        binding.imageView.setColorFilter(textColor)
        binding.msgLabel.setTextColor(textColor)
        binding.msgLabel2.setTextColor(textColor)
    }
}
