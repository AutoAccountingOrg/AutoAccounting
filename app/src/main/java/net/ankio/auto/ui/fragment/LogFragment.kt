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
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentLogBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.LogAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.viewBinding
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.db.model.LogModel
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter


/**
 * 日志页面
 */
class LogFragment : BasePageFragment<LogModel>() {
    override val binding: FragmentLogBinding by viewBinding(FragmentLogBinding::inflate)

    /**
     * 加载数据
     */
    override suspend fun loadData(callback: (resultData: List<LogModel>) -> Unit) {
        LogModel.list(page, pageSize).let { result ->
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    override fun onCreateAdapter() {
        val recyclerView = binding.statusPage.contentView
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = LogAdapter(pageData)
        recyclerView?.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

    }

    /**
     * 创建视图
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_share -> {
                    val loadingUtils = LoadingUtils(requireActivity())
                    loadingUtils.show(R.string.loading_logs)
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val logFile = prepareLogFile()
                            writeLogsToFile(logFile)
                            withContext(Dispatchers.Main) {
                                loadingUtils.close()
                                shareLogFile(logFile)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                loadingUtils.close()
                                Logger.e("Share Log Error", e)
                            }
                        }
                    }
                    true
                }

                R.id.item_clear -> {
                    BottomSheetDialogBuilder(requireActivity())
                        .setTitle(requireActivity().getString(R.string.delete_data))
                        .setMessage(requireActivity().getString(R.string.delete_msg))
                        .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                            lifecycleScope.launch {
                                LogModel.clear()
                                page = 1
                                loadDataInside()
                            }
                        }
                        .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
                        .showInFragment(this, false, true)
                    true
                }

                else -> false
            }
        }
    }

    private fun prepareLogFile(): File {
        val cacheDir = requireContext().cacheDir
        return File(cacheDir, "log.txt").apply {
            if (exists()) delete()
            createNewFile()
        }
    }

    private suspend fun writeLogsToFile(file: File) = withContext(Dispatchers.IO) {
        BufferedWriter(FileWriter(file)).use { writer ->
            for (i in 1..20) {
                val list = LogModel.list(i, 100)
                if (list.isEmpty()) break
                val logText = list.joinToString("\n") {
                    "${DateUtils.stampToDate(it.time)}  ${it.app}  [${it.level}][${it.location}]${it.message}"
                }
                writer.write(logText)
                writer.newLine()  // Ensure each log entry starts on a new line
            }
        }
    }

    private fun shareLogFile(file: File) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            val fileUri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_file))
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_file)))
    }


}
