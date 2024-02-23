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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentLogBinding
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import java.io.File

class LogFragment : BaseFragment() {
    private lateinit var binding: FragmentLogBinding

    override val menuList: ArrayList<MenuItem> = arrayListOf(
        MenuItem(R.string.item_share, R.drawable.ic_share) {

            runCatching {
                val cacheDir = AppUtils.getApplication().externalCacheDir
                val file = File(cacheDir, "/shell/log.txt")
                val shareIntent = Intent(Intent.ACTION_SEND)
                // 设置分享类型为文件
                shareIntent.type = "application/octet-stream"
                // 将文件URI添加到分享意图
                val fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    "net.ankio.auto.fileprovider",
                    file
                )
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                // 添加可选的文本标题
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_file))

                // 启动分享意图
                requireContext().startActivity(
                    Intent.createChooser(
                        shareIntent,
                        getString(R.string.share_file)
                    )
                )
            }.onFailure {
                Logger.e("日志分享失败", it)
            }
        },
        MenuItem(R.string.item_clear, R.drawable.ic_clear) {
            runCatching {
                val cacheDir = AppUtils.getApplication().externalCacheDir
                val file = File(cacheDir, "/shell/log.txt")
                if (file.exists()) {
                    file.delete()
                }
                binding.logTextView.text = "no logs"
            }.onFailure {
                Logger.e("清除失败", it)
            }
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLogBinding.inflate(layoutInflater)



        return binding.root
    }

    override fun onResume() {
        super.onResume()
        AppUtils.getService().getLog {
            binding.logTextView.text = it
        }
    }
}