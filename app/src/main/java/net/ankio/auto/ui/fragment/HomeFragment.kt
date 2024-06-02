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

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.hjq.toast.Toaster
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.AboutDialogBinding
import net.ankio.auto.databinding.FragmentHomeBinding
import net.ankio.auto.events.UpdateFinishEvent
import net.ankio.auto.events.UpdateSuccessEvent
import net.ankio.auto.ui.dialog.AssetsSelectorDialog
import net.ankio.auto.ui.dialog.BookInfoDialog
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.event.EventBus
import net.ankio.auto.utils.server.model.Category
import net.ankio.auto.utils.update.UpdateUtils
import rikka.html.text.toHtml

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : BaseFragment() {
    private lateinit var binding: FragmentHomeBinding
    override val menuList: ArrayList<MenuItem> =
        arrayListOf(
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

        val cards =
            listOf(
                binding.infoCard,
                binding.groupCard,
                binding.ruleCard,
            )
        val color = SurfaceColors.SURFACE_1.getColor(requireContext())
        cards.forEach { it.setCardBackgroundColor(color) }

        scrollView = binding.scrollView

        checkBookApp()

        return binding.root
    }

    private fun refreshUI() {
        bindActiveUI()
        bindBookAppUI()
        bindRuleUI()
    }

    /**
     * 绑定记账软件数据部分的UI
     */
    private fun bindBookAppUI() {
        lifecycleScope.launch {
            val config = AppUtils.getService().config()
            binding.book.visibility = if (config.multiBooks) View.VISIBLE else View.GONE
            binding.assets.visibility = if (config.assetManagement) View.VISIBLE else View.GONE
        }
        SpUtils.getString("bookApp", "").apply {
            if (this.isEmpty()) {
                binding.bookApp.text = getString(R.string.no_setting)
            } else {
                AppUtils.getAppInfoFromPackageName(this, requireContext())?.apply {
                    binding.bookApp.text = this.name
                }
            }
        }
        SpUtils.getString("defaultBook", "默认账本").let {
            binding.defaultBook.text = it
        }
    }

    private fun bindActiveUI() {
        val colorPrimary =
            AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary)

        if (!ActiveUtils.getActiveAndSupportFramework()) {
            setActive(
                SurfaceColors.SURFACE_3.getColor(requireContext()),
                colorPrimary,
                R.drawable.home_active_error,
            )
        } else {
            setActive(
                colorPrimary,
                AppUtils.getThemeAttrColor(
                    com.google.android.material.R.attr.colorOnPrimary,
                ),
                R.drawable.home_active_success,
            )
        }
    }

    private fun checkBookApp() {
        // 判断是否设置了记账软件
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

    private fun bindRuleUI() {
        val ruleVersion = SpUtils.getString("ruleVersionName", "None")
        binding.ruleVersion.text = ruleVersion
    }

    private val onUpdateRule = { event: UpdateSuccessEvent ->
        Toaster.show(R.string.update_success)
        bindRuleUI()
    }

    private fun bindRuleEvents() {
        binding.customCategory.setOnClickListener {
            findNavController().navigate(R.id.ruleFragment)
        }
        EventBus.register(UpdateSuccessEvent::class.java, onUpdateRule)
        binding.checkRuleUpdate.setOnClickListener {
            Toaster.show(R.string.check_update)
            lifecycleScope.launch {
                checkUpdate(true)
            }
        }
        lifecycleScope.launch {
            checkUpdate(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.unregister(UpdateSuccessEvent::class.java, onUpdateRule)
    }

    /**
     * 绑定记账软件数据部分的事件
     */
    private fun bindBookAppEvents() {
        /**
         * 获取主题Context，部分弹窗样式不含M3主题
         */
        val themeContext = AppUtils.getThemeContext(requireContext())
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
                    CategorySelectorDialog(themeContext, it.id, type) { category1: Category?, category2: Category? ->
                        Logger.i("选择的分类是：${category1?.name ?: ""} - ${category2?.name ?: ""}")
                    }.show(cancel = true)
                }.show(cancel = true)
            }.show(cancel = true)
        }
    }

    /**
     * TODO 激活部分的事件，未激活跳转帮助文档
     */
    private fun bindingActiveEvents() {
        binding.active.setOnClickListener {
            //  findNavController().navigate(R.id.serviceFragment)
        }
    }

    private fun bindingCommunicationEvents() {
        binding.msgGithub.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.github_url))
        }

        binding.msgTelegram.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.telegram_url))
        }

        binding.msgQq.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.qq_url))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private suspend fun checkUpdate(showResult: Boolean = false) {
        val updateUtils = UpdateUtils(showResult)
        runCatching {
            updateUtils.checkAppUpdate()?.apply {
                UpdateDialog(
                    requireContext(),
                    hashMapOf("url" to file),
                    log,
                    version,
                    date,
                    0,
                    code,
                ).show(cancel = true)
            }
            updateUtils.checkRuleUpdate()?.apply {
                UpdateDialog(
                    requireContext(),
                    hashMapOf("category" to file + "category.js", "rule" to file + "rule.js"),
                    log,
                    version,
                    date,
                    1,
                    code,
                ).show(cancel = true)
            }
            val listener = { _: UpdateFinishEvent ->
                bindRuleUI()
            }
            EventBus.register(UpdateFinishEvent::class.java, listener)
        }.onFailure {
            Logger.e("更新异常", it)
        }
    }

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
        val versionName = AppUtils.getVersionName()
        val names = versionName.split(" - ")
        binding.msgLabel.text = names[0].trim()
        binding.msgLabel2.text = getString(R.string.releaseInfo)
        binding.imageView.setColorFilter(textColor)
        binding.msgLabel.setTextColor(textColor)
        binding.msgLabel2.setTextColor(textColor)
    }
}
