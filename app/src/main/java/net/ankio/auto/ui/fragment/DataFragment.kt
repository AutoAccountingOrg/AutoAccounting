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
import net.ankio.auto.app.js.Engine
import net.ankio.auto.app.model.AppData
import net.ankio.auto.constant.DataType
import net.ankio.auto.constant.toDataType
import net.ankio.auto.databinding.FragmentDataBinding
import net.ankio.auto.ui.adapter.DataAdapter
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.Github
import java.io.File


class DataFragment : BaseFragment() {
    private lateinit var binding: FragmentDataBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DataAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val dataItems = mutableListOf<AppData>()

    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //判断缓存中的data_issue文件夹是否存在，不存在创建
        file = File(AppUtils.getApplication().cacheDir, "data_issue")
        if (!file.exists()) {
            file.mkdirs()
        }
    }

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
                        withContext(Dispatchers.Main) {

                            Toaster.show(R.string.no_match)
                        }
                    } else {
                       //TODO 弹出记账面板
                    }
                }
            },

            onClickUploadData = { item: AppData, position: Int ->

                if (item.issue != 0) {
                    Toaster.show(getString(R.string.repeater_issue))
                    return@DataAdapter
                }


                context?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle(getString(R.string.upload_sure))  // 设置对话框的标题
                        .setMessage(getString(R.string.upload_info))  // 设置对话框的消息
                        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                            val type = when (item.type.toDataType()) {
                                DataType.App -> "App"
                                DataType.Helper -> "Helper"
                                DataType.Notice -> "Notice"
                                DataType.Sms -> "Sms"
                            }
                            val loadingUtils = LoadingUtils(requireActivity())
                            loadingUtils.show(R.string.upload_waiting)
                            Github.createIssue("[Adaptation Request][$type]${item.source}", """
```
                ${item.data}
```
            """.trimIndent(), { issue ->
                                item.issue = issue.toInt()
                                requireActivity().runOnUiThread {
                                    loadingUtils.close()
                                    adapter.notifyItemChanged(position)
                                    Toaster.show(getString(R.string.upload_success))
                                }


                                writeIssue(item, issue.toInt(), position)

                            }, { msg ->
                                requireActivity().runOnUiThread {
                                    Toaster.show(msg)
                                    CustomTabsHelper.launchUrl(
                                        it,
                                        Uri.parse(Github.getLoginUrl())
                                    )
                                    loadingUtils.close()
                                }

                            })

                            // 可以在这里添加你的处理逻辑
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.close) { dialog, which ->
                            // 在取消按钮被点击时执行的操作
                            dialog.dismiss()
                        }
                        .show()
                }


            }
        )



        recyclerView.adapter = adapter



        return binding.root
    }

    private fun loadMoreData() {
        dataItems.clear()
        lifecycleScope.launch {
            AppUtils.getService().getData {
                val collection: Collection<AppData> = AppData.fromTxt(it)
                forEachIssue(collection)
                dataItems.addAll(collection)
                if (!collection.isEmpty()) {
                    adapter.notifyDataSetChanged()
                }
            }
        }


    }

    private fun writeIssue(item: AppData, issue: Int, pos: Int) {
        val file = File(file, "${item.hash()}.txt")
        file.writeText(issue.toString())
        item.issue = issue
        adapter.notifyItemChanged(pos)
    }

    private fun forEachIssue(collection: Collection<AppData>) {
        //遍历file文件夹下所有的txt
        file.listFiles()?.forEach {
            val issue = it.readText().toInt()
            val hashCode = it.nameWithoutExtension
            val item = collection.find { item ->
                item.hash() == hashCode
            }
            if (item != null) {
                item.issue = issue
            } else {
                //没有匹配到文件需要删除
                it.delete()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //加载数据
        loadMoreData()
    }


}