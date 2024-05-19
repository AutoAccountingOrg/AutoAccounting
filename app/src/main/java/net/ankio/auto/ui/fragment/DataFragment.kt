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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.app.js.Engine
import net.ankio.auto.constant.DataType
import net.ankio.auto.constant.toDataType
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.databinding.SettingItemInputBinding
import net.ankio.auto.ui.adapter.DataAdapter
import net.ankio.auto.ui.dialog.DataEditorDialog
import net.ankio.auto.ui.dialog.FilterDialog
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.Github
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.server.model.AppData

class DataFragment : BaseFragment() {
    private lateinit var binding: FragmentDataBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DataAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val dataItems = mutableListOf<AppData>()
    override val menuList: ArrayList<MenuItem> =
        arrayListOf(
            MenuItem(R.string.item_filter, R.drawable.menu_icon_filter) {
                FilterDialog(requireActivity()) {
                    loadMoreData()
                }.show(false)
            },
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDataBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        scrollView = recyclerView
        adapter =
            DataAdapter(
                dataItems,
                onClickContent = { string ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(requireContext().getString(R.string.content_title))
                        .setMessage(string)
                        .setPositiveButton(requireContext().getString(R.string.cancel_msg)) { _, _ -> }
                        .show()
                },
                onClickTest = { item ->
                    lifecycleScope.launch {
                        val result = Engine.analyze(item.type, item.source, item.data, false)
                        if (result == null) {
                            // 弹出悬浮窗
                            Toaster.show(R.string.no_match)
                        } else {
                            val tpl = SpUtils.getString("setting_bill_remark", "【商户名称】 - 【商品名称】")
                            result.remark = BillUtils.getRemark(result, tpl)
                            BillUtils.setAccountMap(result)
                            AppUtils.getService().config().let {
                                FloatEditorDialog(requireActivity(), result, it).show(float = false)
                            }
                        }
                    }
                },
                onClickUploadData = { item: AppData, position: Int ->

                    if (item.issue != 0) {
                        Toaster.show(getString(R.string.repeater_issue))
                        return@DataAdapter
                    }

                    val builder =
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(if (item.match) getString(R.string.data_question) else getString(R.string.upload_sure)) // 设置对话框的标题

                    var settingItemInputBinding: SettingItemInputBinding? = null

                    if (!item.match) {
                        builder.setMessage(getString(R.string.upload_info))
                    } else {
                        settingItemInputBinding = SettingItemInputBinding.inflate(layoutInflater)
                        settingItemInputBinding.inputLayout.hint = getString(R.string.data_question_info)
                        builder.setView(settingItemInputBinding.root)
                    }
                    builder.setPositiveButton(getString(R.string.ok)) { dialog, which ->
                        var text = ""
                        if (settingItemInputBinding != null) {
                            text = settingItemInputBinding.input.text.toString()
                        }
                        val uploadData = AppUtils.toPrettyFormat(item.data)
                        DataEditorDialog(requireContext(), uploadData) { data ->
                            val type =
                                when (item.type.toDataType()) {
                                    DataType.App -> "App"
                                    DataType.Helper -> "Helper"
                                    DataType.Notice -> "Notice"
                                    DataType.Sms -> "Sms"
                                }
                            val loadingUtils = LoadingUtils(requireActivity())
                            loadingUtils.show(R.string.upload_waiting)
                            lifecycleScope.launch {
                                runCatching {
                                    val title =
                                        if (!item.match) {
                                            "[Adaptation Request][$type]${item.source}"
                                        } else {
                                            "[Bug][Rule][$type]${item.source}"
                                        }
                                    val msg =
                                        if (!item.match) {
                                            """
```
                $data
```
                                            """.trimIndent()
                                        } else {
                                            """
## 规则
${item.rule}
## 说明
$text
## 数据
```
$data
```
                                            """.trimIndent()
                                        }
                                    val issue =
                                        Github.createIssue(
                                            title,
                                            msg,
                                            if (!item.match) "AutoRule" else "AutoAccounting",
                                        )
                                    item.issue = issue.toInt()
                                    withContext(Dispatchers.Main) {
                                        loadingUtils.close()
                                        dataItems[position] = item
                                        adapter.notifyItemChanged(position)
                                        Toaster.show(
                                            if (!item.match) {
                                                getString(
                                                    R.string.upload_success,
                                                )
                                            } else {
                                                getString(R.string.question_success)
                                            },
                                        )
                                    }
                                    AppData.put(item)
                                }.onFailure {
                                    withContext(Dispatchers.Main) {
                                        Toaster.show(it.message)
                                        CustomTabsHelper.launchUrl(
                                            requireContext(),
                                            Uri.parse(Github.getLoginUrl()),
                                        )
                                        loadingUtils.close()
                                    }
                                }
                            }
                            dialog.dismiss()
                        }.show(false)
                    }
                        .setNegativeButton(R.string.close) { dialog, which ->
                            // 在取消按钮被点击时执行的操作
                            dialog.dismiss()
                        }
                        .show()
                },
            )

        recyclerView.adapter = adapter

        return binding.root
    }

    private fun loadMoreData() {
        val loading = LoadingUtils(requireActivity())
        loading.show(R.string.loading)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dataList = AppData.get(500)

                val resultList = mutableListOf<AppData>()
                resultList.addAll(dataList)

                // 在这里处理搜索逻辑
                val resultSearch =
                    resultList.filter {
                        var include = true

                        val dataType = SpUtils.getInt("dialog_filter_data_type", 0)

                        if (dataType != 0) {
                            if (it.type != dataType) {
                                include = false
                            }
                        }

                        val match = SpUtils.getInt("dialog_filter_match", 0)
                        if (match != 0) {
                            if (it.match && match == 2) {
                                include = false
                            }

                            if (!it.match && match == 1) {
                                include = false
                            }
                        }

                        val upload = SpUtils.getInt("dialog_filter_upload", 0)

                        if (upload != 0) {
                            if (it.issue != 0 && upload == 2) {
                                include = false
                            }

                            if (it.issue == 0 && upload == 1) {
                                include = false
                            }
                        }

                        val keywords = SpUtils.getString("dialog_filter_data", "")

                        if (keywords != "") {
                            if (
                                !it.data.contains(keywords)
                            ) {
                                include = false
                            }
                        }

                        include
                    }.reversed()

                // 倒序排列resultSearch
                dataItems.clear()
                dataItems.addAll(resultSearch)
                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                    binding.empty.root.visibility = if (dataItems.isEmpty()) View.VISIBLE else View.GONE
                    loading.close()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 加载数据
        loadMoreData()
    }
}
