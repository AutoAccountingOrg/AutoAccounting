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
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentPluginDataBinding
import net.ankio.auto.http.api.AppDataAPI
import net.ankio.auto.http.api.JsAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.AppDataAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BillEditorDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.dialog.DataEditorDialog
import net.ankio.auto.ui.dialog.EditorDialogBuilder
import net.ankio.auto.ui.models.RailMenuItem
import net.ankio.auto.http.Pastebin
import net.ankio.auto.http.license.RuleAPI
import net.ankio.auto.ui.components.MaterialSearchView
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.SystemUtils
import net.ankio.auto.utils.getAppInfoFromPackageName
import org.ezbook.server.constant.DataType
import org.ezbook.server.db.model.AppDataModel

/**
 * 插件数据管理Fragment
 *
 * 该Fragment负责展示和管理应用数据，包括：
 * - 左侧应用列表展示
 * - 数据筛选（通知数据、应用数据、匹配状态）
 * - 搜索功能切换
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

    /**
     * 应用信息数据类 - 统一的数据结构，消除双重维护
     * 按照Linus的"好品味"原则：一个数据结构解决一个问题
     */
    data class AppInfo(
        val packageName: String,
        val name: String,
        val icon: android.graphics.drawable.Drawable?
    )

    /** 应用列表 - 单一数据源，消除索引转换的特殊情况 */
    private val appList = mutableListOf<AppInfo>()

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
    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())

        return AppDataAdapter().apply {
            onTestRuleClick = ::handleTestRuleClick
            onTestRuleLongClick = ::handleTestRuleLongClick
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
     * 处理测试规则长按事件：强制 AI 识别
     */
    private fun handleTestRuleLongClick(item: AppDataModel) {
        launch {
            val loading = LoadingUtils(requireContext())
            loading.show(R.string.rule_testing)

            val billResultModel = JsAPI.analysis(item.type, item.data, item.app, true, true)

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
     * 设置左侧数据、过滤器按钮和搜索功能
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpLeftData()
        setupFilterButtons()
        binding.toolbar.setOnMenuItemClickListener(this)
        setUpSearch()
    }

    /**
     * 设置左侧应用列表数据
     * 消除索引转换 - 直接使用数组索引，无特殊情况
     */
    private fun setUpLeftData() {
        binding.leftList.setOnItemSelectedListener { menuItem ->
            page = 1
            // Linus式简化：无需索引转换，直接使用数组位置
            val index = menuItem.id - 1
            if (index in appList.indices) {
                app = appList[index].packageName
                Logger.d("Selected app: $app (${appList[index].name})")
                reload()
            } else {
                Logger.w("Invalid app selection index: $index, list size: ${appList.size}")
            }
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
     * 简化的单一数据流 - 从API到UI，无重复数据结构
     */
    private fun refreshLeftData() {
        Logger.d("Refreshing left data for plugin data")
        launch {
            try {
                // 1. 清空单一数据源
                binding.leftList.clear()
                appList.clear()

                // 2. 获取应用数据
                val apiResult = AppDataAPI.apps()
                Logger.d("Fetched ${apiResult.size()} apps from plugin API")

                // 3. 构建应用列表 - 单次遍历，无重复逻辑
                var index = 1
                for (packageName in apiResult.keySet()) {
                    val appInfo = getAppInfoFromPackageName(packageName)
                    if (appInfo == null) {
                        Logger.w("Failed to get app info for package: $packageName")
                        continue
                    }

                    // 创建统一的应用数据对象
                    val app = AppInfo(packageName, appInfo.name, appInfo.icon)
                    appList.add(app)

                    // 添加到UI - 使用统一数据源
                    binding.leftList.addMenuItem(
                        RailMenuItem(index, app.icon!!, app.name)
                    )

                    Logger.d("Added app: ${app.name} ($packageName)")
                    index++
                }

                // 4. 处理空状态 - 简化条件判断
                if (appList.isEmpty() || !binding.leftList.performFirstItem()) {
                    Logger.w("No apps available, showing empty state")
                    statusPage.showEmpty()
                }
            } catch (e: Exception) {
                Logger.e("Error refreshing left data", e)
                statusPage.showError()
            }
        }
    }

    /**
     * 设置过滤按钮：类型、匹配状态
     */
    private fun setupFilterButtons() {
        setupTypeButton()
        setupMatchButton()
    }

    /**
     * 配置"类型"筛选按钮：全部/通知/数据/OCR
     */
    private fun setupTypeButton() {
        val labels = resources.getStringArray(R.array.rule_type_options)
        binding.typeButton.text = labels[0]

        val items = linkedMapOf(
            labels[0] to "",
            labels[1] to DataType.NOTICE.name,
            labels[2] to DataType.DATA.name,
            labels[3] to DataType.OCR.name
        )

        // 统一风格：点击主按钮弹出菜单
        binding.typeButton.setOnClickListener { anchorView ->
            ListPopupUtilsGeneric.create<String>(requireContext())
                .setAnchor(anchorView)
                .setList(items)
                .setOnItemClick { _, key, value ->
                    type = value
                    binding.typeButton.text = key
                    Logger.d("Data type filter updated: type='$type'")
                    reload()
                }
                .show()
        }
    }

    /**
     * 配置"匹配状态"筛选按钮：全部/已匹配/未匹配
     */
    private fun setupMatchButton() {
        val labels = resources.getStringArray(R.array.match_options)
        binding.matchButton.text = labels[0]

        val items = linkedMapOf<String, Boolean?>(
            labels[0] to null,
            labels[1] to true,
            labels[2] to false
        )

        // 统一风格：点击主按钮弹出菜单
        binding.matchButton.setOnClickListener { anchorView ->
            ListPopupUtilsGeneric.create<Boolean?>(requireContext())
                .setAnchor(anchorView)
                .setList(items)
                .setOnItemClick { _, key, value ->
                    match = value
                    binding.matchButton.text = key
                    Logger.d("Match filter updated: match=$match")
                    reload()
                }
                .show()
        }
    }

    /**
     * 设置搜索功能
     * 配置搜索视图的查询文本监听器
     */
    private fun setUpSearch() {

        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        if (searchItem != null) {
            val searchView = searchItem.actionView as MaterialSearchView
            searchView.setOnQueryTextListener(object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = true
                override fun onQueryTextChange(newText: String?): Boolean {
                    searchData = newText ?: ""
                    reload()
                    return true
                }
            })
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
                            Logger.i("Data cleared successfully")
                            page = 1
                            reload()
                        }
                    }
                    .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ ->
                        Logger.d("User cancelled data clear")
                    }
                    .show()
                return true
            }

            else -> {
                Logger.d("Unknown menu item clicked: ${item?.itemId}")
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
        val localRuleVersion = PrefManager.ruleVersion.ifEmpty { "未知" }
        val body = """
<!------ 
 1. 请不要手动复制数据，下面的链接中已包含数据；
 2. 您可以新增信息，但是不要删除本页任何内容；
 3. 一般情况下，您直接划到底部点击submit即可。
 ------>
## 自动记账版本
${BuildConfig.VERSION_NAME}
## 本地规则版本
$localRuleVersion
## 数据链接
[数据过期时间：${timeout}](${url})
## 其他信息
<!------ 
 1. 您可以在下面添加说明信息。
 ------>

                """.trimIndent()

        submitGithub(title, body)
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
## 自动记账版本
${BuildConfig.VERSION_NAME}
## 规则
${item.rule}${if (item.version.isNotEmpty()) " (规则版本: ${item.version})" else ""}
## 数据
[数据过期时间：${timeout}](${url})
## 说明
$desc

                         
                                            """.trimIndent()

        submitGithub(title, body)
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

}