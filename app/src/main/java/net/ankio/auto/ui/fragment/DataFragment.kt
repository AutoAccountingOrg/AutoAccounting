/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import android.widget.ArrayAdapter
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentPluginDataBinding
import net.ankio.auto.http.api.AppDataAPI
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.AppDataAdapter
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.MaterialSearchView
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BillEditorDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.dialog.DataEditorDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.ui.models.RailMenuItem
import net.ankio.auto.http.Pastebin
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.http.license.RuleAPI
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.SystemUtils
import net.ankio.auto.utils.getAppInfoFromPackageName
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.AppDataModel
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 插件数据管理Fragment
 *
 * 该Fragment负责展示和管理应用数据，包括：
 * - 左侧应用列表展示
 * - 数据筛选（通知数据、应用数据、匹配状态）
 * - 搜索功能
 * - 数据清理功能
 *
 * @author ankio
 */
class DataFragment : BasePageFragment<AppDataModel, FragmentPluginDataBinding>(),
    Toolbar.OnMenuItemClickListener {

    /** 当前选中的应用包名 */
    var app: String = ""

    /** 数据类型筛选（NOTICE/DATA） */
    var type: String = ""

    /** 匹配状态：null=全部，true=已匹配，false=未匹配 */
    var match: Boolean? = null

    /** 搜索关键词 */
    var searchData = ""

    /** 左侧应用数据缓存 */
    private var leftData = JsonObject()

    companion object {
        private const val GITHUB_ISSUE_URL =
            "https://github.com/AutoAccountingOrg/AutoRuleSubmit/issues"
    }

    /**
     * 加载数据的主要方法
     * 根据当前筛选条件从API获取应用数据列表
     *
     * @return 应用数据模型列表
     */
    override suspend fun loadData(): List<AppDataModel> =
        AppDataAPI.list(app, type, match, page, pageSize, searchData)

    /**
     * 创建数据适配器
     * 配置RecyclerView的布局管理器和适配器，并设置事件监听器
     *
     * @return 配置好的AppDataAdapter实例
     */
    override fun onCreateAdapter(): AppDataAdapter {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())

        return AppDataAdapter().apply {
            onTestRuleClick = ::handleTestRuleClick
            onContentClick = ::handleContentClick
            onCreateRuleClick = ::handleCreateRuleClick
            onUploadDataClick = ::handleUploadDataClick
            onDeleteClick = ::handleDeleteClick
        }
    }

    /**
     * 处理测试规则点击事件
     */
    private fun handleTestRuleClick(item: AppDataModel) {
        launch {
            val loading = LoadingUtils(requireContext())
            loading.show(R.string.rule_testing)

            val billResultModel = JsAPI.analysis(item.type, item.data, item.app, true)

            billResultModel?.let {
                BaseSheetDialog.create<BillEditorDialog>(requireContext())
                    .setBillInfo(billResultModel.parentInfoModel ?: billResultModel.billInfoModel)
                    .setOnConfirm {

                    }
                    .setOnCancel { _ -> }
                    .show()
            } ?: ToastUtils.error(getString(R.string.no_rule_hint))

            loading.close()
        }
    }

    /**
     * 处理内容点击事件
     */
    private fun handleContentClick(item: AppDataModel) {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.content_title))
            .setMessage(item.data)
            .setNegativeButton(getString(R.string.cancel_msg)) { _, _ -> }
            .setPositiveButton(getString(R.string.copy)) { _, _ ->
                SystemUtils.copyToClipboard(item.data)
                ToastUtils.info(R.string.copy_command_success)
            }
            .show()
    }

    /**
     * 处理创建规则点击事件
     */
    private fun handleCreateRuleClick(item: AppDataModel) {
        val args = Bundle().apply {
            putString("data", Gson().toJson(item))
        }
        findNavController().navigate(R.id.ruleEditFragment, args)
    }

    /**
     * 处理上传数据点击事件
     */
    private fun handleUploadDataClick(item: AppDataModel) {
        BaseSheetDialog.create<DataEditorDialog>(requireContext())
            .setData(item.data)
            .setOnConfirm { result ->
                launch {
                    val loading = LoadingUtils(requireContext())
                    loading.show(R.string.upload_waiting)

                    // AI 生成视为未匹配：当规则为空、未匹配，或规则字符串标记为“生成”时，都走适配帮助
                    if (!item.hasValidMatch() || item.isAiGeneratedRule()) {
                        requestRuleHelp(item, result)
                    } else {
                        showRuleBugDialog(item, result)
                    }

                    loading.close()
                }
            }
            .show()
    }

    /**
     * 处理删除点击事件
     */
    private fun handleDeleteClick(item: AppDataModel) {
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.delete_title))
            .setMessage(getString(R.string.delete_data_message))
            .setPositiveButton(getString(R.string.btn_confirm)) { _, _ ->
                removeItem(item)  // 使用基类的辅助函数
                launch {
                    AppDataAPI.delete(item.id)
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { _, _ -> }
            .show()
    }

    /**
     * 显示规则Bug反馈对话框
     */
    private fun showRuleBugDialog(item: AppDataModel, result: String) {
        BaseSheetDialog.create<EditorDialogBuilder>(requireContext())
            .setTitleInt(R.string.rule_issue)
            .setMessage("")
            .setEditorPositiveButton(R.string.btn_confirm) { description ->
                launch {
                    requestRuleBug(item, result, description)
                }
            }
            .setNegativeButton(R.string.btn_cancel) { _, _ -> }
            .show()
    }

    /**
     * Fragment视图创建完成后的初始化
     * 设置左侧数据、芯片事件、工具栏菜单和搜索功能
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpLeftData()
        setupFilterDropdown()
        binding.toolbar.setOnMenuItemClickListener(this)
        setUpSearch()

    }

    /**
     * 设置左侧应用列表数据
     * 配置应用选择监听器和刷新数据
     */
    private fun setUpLeftData() {

        binding.leftList.setOnItemSelectedListener {
            val id = it.id
            page = 1
            app = leftData.keySet().elementAt(id - 1)
            logger.debug { "Selected app: $app (id: $id)"}
            reload()
        }
    }

    /**
     * Fragment恢复时的处理
     */
    override fun onResume() {
        super.onResume()
        refreshLeftData()
    }

    /**
     * 刷新左侧应用数据
     * 从API获取应用列表并更新UI
     */
    private fun refreshLeftData() {
        logger.debug { "Refreshing left data" }
        launch {
            try {
                // 1. 清空列表
                binding.leftList.clear()

                // 2. 拉取 app 数据
                val result = AppDataAPI.apps()
                leftData = result
                logger.debug { "Fetched ${result.size()} apps from API"}

                var index = 1
                // 3. 遍历所有 app 包名
                for (packageName in result.keySet()) {
                    val app = getAppInfoFromPackageName(packageName)
                    if (app == null) {
                        logger.warn { "Failed to get app info for package: $packageName" }
                        continue
                    }

                    binding.leftList.addMenuItem(
                        RailMenuItem(index, app.icon!!, app.name)
                    )
                    logger.debug { "Added app to left list: ${app.name} ($packageName)"}
                    index++
                }

                // 4. 若没有任何 app，展示空状态页
                if (!binding.leftList.performFirstItem()) {
                    logger.warn { "No apps available, showing empty state" }
                    statusPage.showEmpty()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error refreshing left data" }
                statusPage.showError()
            }
        }
    }

    /**
     * 设置下拉筛选（类型、匹配）
     */
    private fun setupFilterDropdown() {
        setupTypeFilter()
        setupMatchFilter()
    }

    /**
     * 设置数据类型过滤器（全部/通知/数据）
     */
    private fun setupTypeFilter() {
        // 选项复用规则页的数组，文本一致：全部/通知/数据
        val typeOptions = resources.getStringArray(R.array.rule_type_options)

        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            typeOptions
        )

        binding.typeDropdown.setAdapter(typeAdapter)
        binding.typeDropdown.setOnItemClickListener { _, _, position, _ ->
            type = when (position) {
                1 -> DataType.NOTICE.name
                2 -> DataType.DATA.name
                else -> ""
            }
            logger.debug { "Data type filter updated: type='$type'" }
            reload()
        }
        // 默认：全部
        binding.typeDropdown.setText(typeOptions[0], false)
    }

    /**
     * 设置匹配状态过滤器（全部/已匹配/未匹配）
     */
    private fun setupMatchFilter() {
        val matchOptions = resources.getStringArray(R.array.match_options)
        val matchAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            matchOptions
        )

        binding.matchDropdown.setAdapter(matchAdapter)
        binding.matchDropdown.setOnItemClickListener { _, _, position, _ ->
            match = when (position) {
                1 -> true  // 已匹配
                2 -> false // 未匹配
                else -> null // 全部
            }
            logger.debug { "Match filter updated: match=$match" }
            reload()
        }
        // 默认：全部
        binding.matchDropdown.setText(matchOptions[0], false)
    }

    /**
     * 设置搜索功能
     * 配置搜索视图的查询文本监听器
     */
    private fun setUpSearch() {


        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        if (searchItem != null) {
            val searchView = searchItem.actionView as MaterialSearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    logger.debug { "Search submitted: '$query'" }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchData = newText ?: ""
                    logger.debug { "Search text changed: '$searchData'" }
                    reload()
                    return true
                }
            })
        } else {
            logger.warn { "Search menu item not found" }
        }
    }

    /**
     * 工具栏菜单项点击处理
     * 处理数据清理等菜单操作
     *
     * @param item 被点击的菜单项
     * @return 是否处理了菜单点击事件
     */
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.item_clear -> {
                // 检查Fragment View是否仍然有效
                if (!uiReady()) {
                    return true
                }

                BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
                    .setTitle(getString(R.string.delete_data))
                    .setMessage(getString(R.string.delete_msg))
                    .setPositiveButton(getString(R.string.sure_msg)) { _, _ ->
                        launch {
                            AppDataAPI.clear()
                            logger.info { "Data cleared successfully" }
                            page = 1
                            reload()
                        }
                    }
                    .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ ->
                        logger.debug { "User cancelled data clear" }
                    }
                    .show()
                return true
            }

            else -> {
                logger.debug { "Unknown menu item clicked: ${item?.itemId}" }
                return false
            }
        }
    }

    /**
     * 请求规则适配帮助
     * 原 AppDataRepository.requestRuleHelp 方法
     */
    private suspend fun requestRuleHelp(item: AppDataModel, result: String) {
        val type = item.type.name
        val title = "[Adaptation Request][$type]${item.app}"

        val (url, timeout) = Pastebin.add(result).getOrThrow()
        val body = """
<!------ 
 1. 请不要手动复制数据，下面的链接中已包含数据；
 2. 您可以新增信息，但是不要删除本页任何内容；
 3. 一般情况下，您直接划到底部点击submit即可。
 ------>
## 数据链接
[数据过期时间：${timeout}](${url})
## 其他信息
<!------ 
 1. 您可以在下面添加说明信息。
 ------>

                """.trimIndent()

        if (PrefManager.githubConnectivity) {
            submitGithub(title, body)
        } else {
            submitCloud(title, body)
        }
    }

    /**
     * 反馈规则Bug
     * 原 AppDataRepository.requestRuleBug 方法
     */
    private suspend fun requestRuleBug(item: AppDataModel, result: String, desc: String) {
        val type = item.type.name
        val title = "[Bug Report][$type]${item.app}"

        val (url, timeout) = Pastebin.add(result).getOrThrow()
        val body = """
<!------ 
 1. 请不要手动复制数据，下面的链接中已包含数据；
 2. 该功能是反馈规则识别错误的，请勿写其他无关内容；
 ------>
## 规则
${item.rule}
## 数据
[数据过期时间：${timeout}](${url})
## 说明
$desc

                         
                                            """.trimIndent()

        if (PrefManager.githubConnectivity) {
            submitGithub(title, body)
        } else {
            submitCloud(title, body)
        }
    }

    /**
     * 通过Github提交Issue
     * 原 AppDataRepository.submitGithub 方法
     */
    private fun submitGithub(title: String, body: String) {
        CustomTabsHelper.launchUrl(
            "$GITHUB_ISSUE_URL/new?title=${Uri.encode(title)}&body=${Uri.encode(body)}".toUri()
        )
    }

    /**
     * 通过云端API提交Issue
     * 原 AppDataRepository.submitCloud 方法
     */
    private suspend fun submitCloud(title: String, body: String) {
        val result = RuleAPI.submit(title, body)
        val data = Gson().fromJson(result, JsonObject::class.java)
        if (data.get("code").asInt != 200) {
            throw RuntimeException(data.get("msg").asString)
        }
    }
}