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
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.app.js.Engine
import net.ankio.auto.database.table.AppData
import net.ankio.auto.constant.DataType
import net.ankio.auto.constant.toDataType
import net.ankio.auto.database.Db
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.ui.adapter.DataAdapter
import net.ankio.auto.ui.dialog.DataEditorDialog
import net.ankio.auto.ui.dialog.FilterDialog
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.AutoAccountingServiceUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.Github
import net.ankio.auto.utils.Logger
import java.io.File


class DataFragment : BaseFragment() {
    private lateinit var binding: FragmentDataBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DataAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val dataItems = mutableListOf<AppData>()
    override val menuList: ArrayList<MenuItem> = arrayListOf(
        MenuItem(R.string.item_filter, R.drawable.menu_icon_filter) {
            FilterDialog(requireContext()) { keyword ->
                loadMoreData(keyword)
            }.show(false)
        }
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDataBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

        adapter = DataAdapter(
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
                    val result = Engine.analyze(item.type, item.source, item.data)
                    if (result == null) {
                        //弹出悬浮窗
                        Toaster.show(R.string.no_match)
                    } else {
                        AppUtils.getService().config().let {
                            FloatEditorDialog(requireActivity(), result,it).show(float = false)
                        }
                    }
                }
            },

            onClickUploadData = { item: AppData, position: Int ->

                if (item.issue != 0) {
                    Toaster.show(getString(R.string.repeater_issue))
                    return@DataAdapter
                }



                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.upload_sure))  // 设置对话框的标题
                    .setMessage(getString(R.string.upload_info))  // 设置对话框的消息
                    .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                        DataEditorDialog(requireContext(), item.data) { data ->
                            val type = when (item.type.toDataType()) {
                                DataType.App -> "App"
                                DataType.Helper -> "Helper"
                                DataType.Notice -> "Notice"
                                DataType.Sms -> "Sms"
                            }
                            val loadingUtils = LoadingUtils(requireActivity())
                            loadingUtils.show(R.string.upload_waiting)
                            lifecycleScope.launch {
                                runCatching {
                                    val issue = Github.createIssue("[Adaptation Request][$type]${item.source}", """
```
                ${item.data}
```
            """.trimIndent())
                                    item.issue = issue.toInt()
                                    requireActivity().runOnUiThread {
                                        loadingUtils.close()
                                        adapter.notifyItemChanged(position)
                                        Toaster.show(getString(R.string.upload_success))
                                    }

                                    Db.get().AppDataDao().update(item)
                                }.onFailure {
                                    requireActivity().runOnUiThread {
                                        Toaster.show(it.message)
                                        CustomTabsHelper.launchUrl(
                                            requireContext(),
                                            Uri.parse(Github.getLoginUrl())
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

            }
        )



        recyclerView.adapter = adapter



        return binding.root
    }




    private val callEditorPanel = {

    }
    private fun loadMoreData(
         keywords:String = ""
    ) {

        lifecycleScope.launch {
            val data =  AutoAccountingServiceUtils.get("data",requireContext())
            val collection: Collection<AppData> = AppData.fromTxt(data)
            //如果不为空需要做对比后插入数据库
            if(collection.isEmpty()){
                binding.empty.root.visibility = if(dataItems.isEmpty()) View.VISIBLE else View.GONE
                return@launch
            }
            val appData = Db.get().AppDataDao().loadAll()

            val filteredCollection = collection.filter { item ->
                appData.none { it.hash() == item.hash() }
            }

            Db.get().AppDataDao().addList(filteredCollection)

            val resultList = mutableListOf<AppData>()
            resultList.addAll(appData)
            resultList.addAll(filteredCollection)
            //处理完成再删
            AutoAccountingServiceUtils.delete("data",requireContext())

            //在这里处理搜索逻辑
            val resultSearch = resultList.filter {
                var include = true

                if (keywords != "") {
                    if(
                        !it.data.contains(keywords) &&
                        !it.rule.contains(keywords)
                            ){
                        include = false
                    }

                }

                include
            }


            dataItems.clear()
            dataItems.addAll(resultSearch)
            adapter.notifyDataSetChanged()
            binding.empty.root.visibility = if(dataItems.isEmpty()) View.VISIBLE else View.GONE

        }


    }


    override fun onResume() {

        super.onResume()
        //加载数据
        loadMoreData()
    }


}