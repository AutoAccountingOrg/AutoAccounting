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

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.request.Pastebin
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.AppDataAdapter
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.componets.CustomNavigationRail
import net.ankio.auto.ui.componets.MaterialSearchView
import net.ankio.auto.ui.componets.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.dialog.DataEditorDialog
import net.ankio.auto.ui.models.RailMenuItem
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.viewBinding
import net.ankio.auto.utils.CustomTabsHelper
import org.ezbook.server.Server
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.BillInfoModel

class DataFragment : BasePageFragment<AppDataModel>(), Toolbar.OnMenuItemClickListener {
    var app: String = ""
    var type: String = ""
    var match = false
    var searchData = ""
    override suspend fun loadData(callback: (resultData: List<AppDataModel>) -> Unit) {
        AppDataModel.list(app, type, match, page, pageSize, searchData).let { result ->
            callback(result)
        }
    }

    private lateinit var adapter: AppDataAdapter

    override fun onCreateAdapter() {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(requireContext())
        adapter = AppDataAdapter(requireActivity() as BaseActivity)
        adapter
            .setOnEditClick(::onEditClick)
            .setOnLongClick(::onLongClick)
            .setOnUploadClick(::onUploadClick)
            .setOnTestRuleClick(::onTestRuleClick)
            .setOnTestRuleAiClick(::onAITestRuleClick)
            .setOnContentClick(::onContentClick)
        recyclerView.adapter = adapter
    }

    fun onContentClick(item: AppDataModel) {
        BottomSheetDialogBuilder(requireActivity())
            .setTitle(requireActivity().getString(R.string.content_title))
            .setMessage(item.data)
            .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
            .setPositiveButton(requireActivity().getString(R.string.copy)) { _, _ ->
                App.copyToClipboard(item.data)
                ToastUtils.error(R.string.copy_command_success)
            }
            .showInFragment(this, false, true)
    }

    fun onEditClick(item: AppDataModel) {
        // 跳转编辑页
    }

    fun onLongClick(item: AppDataModel) {
        BottomSheetDialogBuilder(requireActivity())
            .setTitle(requireActivity().getString(R.string.delete_title))
            .setMessage(requireActivity().getString(R.string.delete_data_message))
            .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                pageData.remove(item)
                adapter.updateItems(pageData)
                lifecycleScope.launch {
                    AppDataModel.delete(item.id)
                }
            }
            .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
            .showInFragment(this, false, true)
    }

    fun onUploadClick(item: AppDataModel) {
        DataEditorDialog(requireActivity(), item.data) { result ->
            val loading = LoadingUtils(requireActivity())
            loading.show(R.string.upload_waiting)
            lifecycleScope.launch {
                val type = item.type.name
                val title = if (!item.match) {
                    "[Adaptation Request][$type]${item.app}"
                } else {
                    "[Bug][Rule][$type]${item.app}"
                }
                runCatching {
                    val (url, timeout) = Pastebin.add(result, requireContext())
                    val body = if (!item.match) {
                        """
<!------ 
 1. 请不要手动复制数据，下面的链接中已包含数据；
 2. 您可以新增信息，但是不要删除本页任何内容；
 3. 一般情况下，您直接划到底部点击submit即可。
 ------>                        
## 数据链接                        
[数据过期时间：${timeout}](${url})
## 其他信息

                """.trimIndent()
                    } else {
                        """
<!------ 
 1. 请不要手动复制数据，下面的链接中已包含数据；
 2. 该功能是反馈规则识别错误的，请勿写其他无关内容；
 ------>  
## 规则
${item.rule}
## 数据
[数据过期时间：${timeout}](${url})
## 说明


                         
                                            """.trimIndent()
                    }
                    val uri = if (item.match) {
                        "https://github.com/AutoAccountingOrg/AutoAccounting/issues"
                    } else {
                        "https://github.com/AutoAccountingOrg/AutoRule/issues"
                    }
                    CustomTabsHelper.launchUrl(
                        requireContext(),
                        Uri.parse("$uri/new?title=${Uri.encode(title)}&body=${Uri.encode(body)}"),
                    )
                    loading.close()
                }.onFailure {
                    ToastUtils.error(it.message!!)
                    loading.close()
                    return@launch
                }
            }
        }.showInFragment(this, false, true)
    }

    fun onTestRuleClick(item: AppDataModel) {
        lifecycleScope.launch {
            runTest(false, item)
        }
    }

    fun onAITestRuleClick(item: AppDataModel) {
        lifecycleScope.launch {
            runTest(true, item)
        }
    }

    private suspend fun testRule(item: AppDataModel, ai: Boolean = false): BillInfoModel? =
        withContext(Dispatchers.IO) {
            val result = Server.request(
                "js/analysis?type=${item.type.name}&app=${item.app}&fromAppData=true&ai=${ai}",
                item.data
            ) ?: return@withContext null
            Logger.d("Test Result: $result")
            val data = Gson().fromJson(result, JsonObject::class.java)
            if (data.get("code").asInt != 200) {
                Logger.w("Test Error Info: ${data.get("msg").asString}")
                return@withContext null
            }
            return@withContext Gson().fromJson(
                data.getAsJsonObject("data"),
                BillInfoModel::class.java
            )
        }

    private suspend fun runTest(ai: Boolean = false, item: AppDataModel) {
        val loadingUtils = LoadingUtils(requireActivity())
        if (ai) {
            loadingUtils.show(
                requireActivity().getString(
                    R.string.ai_loading,
                    ConfigUtils.getString(Setting.AI_MODEL)
                )
            )
        }
        val billModel = testRule(item, ai)
        if (ai) {
            loadingUtils.close()
        }
        if (billModel == null) {
            ToastUtils.error(R.string.no_rule_hint)
        } else {
            val serviceIntent =
                Intent(activity, FloatingWindowService::class.java).apply {
                    putExtra("parent", "")
                    putExtra("billInfo", Gson().toJson(billModel))
                    putExtra("showWaitTip", false)
                    putExtra("from", "AppData")
                }
            Logger.d("Start FloatingWindowService")
            requireActivity().startService(serviceIntent)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override val binding: FragmentDataBinding by viewBinding(FragmentDataBinding::inflate)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadLeftData(binding.leftList)
        setupChipEvent()
        val leftList = binding.leftList
        lifecycleScope.launch {
            AppDataModel.apps().let { result ->
                leftData = result
                var i = 0
                for (key in result.keySet()) {
                    i++
                    var app = App.getAppInfoFromPackageName(key)

                    if (app == null) {
                        if (App.debug) {
                            app = arrayOf(
                                key,
                                ResourcesCompat.getDrawable(
                                    App.app.resources,
                                    R.drawable.default_asset,
                                    null
                                ),
                                ""
                            )
                        } else {
                            continue
                        }

                    }

                    leftList.addMenuItem(
                        RailMenuItem(i, app[1] as Drawable, app[0] as String)
                    )

                }
                if (!leftList.triggerFirstItem()) {
                    statusPage.showEmpty()
                }
            }
        }

        binding.toolbar.setOnMenuItemClickListener(this)
        setUpSearch()
    }

    private var leftData = JsonObject()
    private fun loadLeftData(leftList: CustomNavigationRail) {

        leftList.setOnItemSelectedListener {
            val id = it.id
            page = 1
            app = leftData.keySet().elementAt(id - 1)
            statusPage.showLoading()
            loadDataInside()
        }
    }

    private fun setupChipEvent() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedId ->

            match = false
            type = ""

            if (R.id.chip_match in checkedId) {
                match = true
            }

            if (R.id.chip_notify in checkedId) {
                type = DataType.NOTICE.name
            }

            if (R.id.chip_data in checkedId) {
                type = DataType.DATA.name
            }

            if (R.id.chip_notify in checkedId && R.id.chip_data in checkedId) {
                type = ""
            }


            loadDataInside()
        }
    }

    private fun setUpSearch() {
        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        if (searchItem != null) {
            val searchView = searchItem.actionView as MaterialSearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchData = newText ?: ""
                    reload()
                    return true
                }

            })
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.item_clear -> {
                BottomSheetDialogBuilder(requireActivity())
                    .setTitle(requireActivity().getString(R.string.delete_data))
                    .setMessage(requireActivity().getString(R.string.delete_msg))
                    .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                        lifecycleScope.launch {
                            AppDataModel.clear()
                            page = 1
                            loadDataInside()
                        }
                    }
                    .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
                    .showInFragment(this, false, true)
                return true
            }

            else -> {
                return false
            }
        }
    }
}
