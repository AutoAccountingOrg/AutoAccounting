/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentLogBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.LogAdapter
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.db.model.LogModel
import java.io.File

class LogFragment : BaseFragment() {
    private lateinit var binding: FragmentLogBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LogAdapter
    private lateinit var layoutManager: LinearLayoutManager
    override val menuList: ArrayList<MenuItem> =
        arrayListOf(
            MenuItem(R.string.item_share, R.drawable.menu_icon_share) {
                runCatching {
                    val loadingUtils = LoadingUtils(requireActivity())
                    loadingUtils.show(R.string.loading_logs)

                    lifecycleScope.launch {
                        val cacheDir = requireContext().cacheDir
                        val file = File(cacheDir, "/log.txt")

                        if (file.exists()) {
                            file.delete()
                        }
                        file.createNewFile()

                        //循环10页日志，每页100条
                        for (i in 1..10) {
                            LogModel.list(i, 100).let { list ->
                                file.appendText(list.joinToString("\n") {
                                    "[${DateUtils.getTime(it.time)}] [ ${it.app} ] [ ${it.location} ] [ ${it.level} ] ${it.message}"
                                })
                            }
                        }




                        loadingUtils.close()
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        // 设置分享类型为文件
                        shareIntent.type = "application/octet-stream"
                        // 将文件URI添加到分享意图
                        val fileUri =
                            FileProvider.getUriForFile(
                                requireContext(),
                                "net.ankio.auto.fileprovider",
                                file,
                            )
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        // 添加可选的文本标题
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_file))

                        // 启动分享意图
                        requireContext().startActivity(
                            Intent.createChooser(
                                shareIntent,
                                getString(R.string.share_file),
                            ),
                        )
                    }
                }.onFailure {
                    Logger.e("日志分享失败", it)
                }
            },
            MenuItem(R.string.item_clear, R.drawable.menu_icon_clear) {
                runCatching {
                    lifecycleScope.launch {
                        LogModel.clear()
                        page = 1
                        loadData()
                    }
                }.onFailure {
                    Logger.e("清除失败", it)
                }
            },
        )
    private var page = 1
    private val pageSize = 20
    private val pageData = mutableListOf<LogModel>()

    /**
     * 加载数据，如果数据为空或者加载失败返回false
     */
    private fun loadData(callback: ((Boolean, Boolean) -> Unit)?=null) {
        if (page == 1) {
            pageData.clear()
            adapter.notifyDataSetChanged()
        }
        lifecycleScope.launch {
            LogModel.list(page, pageSize).let { result ->
                withContext(Dispatchers.Main) {
                    if (result.isEmpty()) {
                        callback?.invoke(true, false)
                        return@withContext
                    }
                    pageData.addAll(result)
                    adapter.notifyItemInserted(pageData.size - pageSize)
                    if (callback != null) callback(true, result.size >= pageData.size)
                }

            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLogBinding.inflate(layoutInflater)
        recyclerView = binding.recyclerView
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        adapter = LogAdapter(pageData)
        recyclerView.adapter = adapter
        scrollView = recyclerView
        loadDataEvent(binding.refreshLayout)
        return binding.root
    }

    private fun loadDataEvent(refreshLayout: RefreshLayout) {
        refreshLayout.setRefreshHeader(ClassicsHeader(requireContext()))
        refreshLayout.setRefreshFooter(ClassicsFooter(requireContext()))
        refreshLayout.setOnRefreshListener {
            page = 1
            loadData { success, hasMore ->
                it.finishRefresh(0, success, hasMore) //传入false表示刷新失败
            }
        }
        refreshLayout.setOnLoadMoreListener {
            page++
            loadData { success, hasMore ->
                it.finishLoadMore(0, success, hasMore) //传入false表示加载失败
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}
