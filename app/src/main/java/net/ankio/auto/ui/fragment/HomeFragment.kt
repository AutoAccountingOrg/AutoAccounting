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

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.google.android.material.elevation.SurfaceColors
import com.google.gson.Gson
import com.quickersilver.themeengine.ThemeEngine
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentHomeBinding
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.AutoAccountingServiceUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.common.config.AccountingConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        bindBookAppEvents()

        val cards = listOf(
            binding.infoCard,
            binding.logCard,
            binding.groupCard,
            binding.ruleCard
        )
        val color = SurfaceColors.SURFACE_1.getColor(requireContext())
        cards.forEach { it.setCardBackgroundColor(color) }


        binding.ruleVersion.text = SpUtils.getString("ruleVersionName", "None")
        binding.showLog.setOnClickListener {
            findNavController().navigate(R.id.logFragment)
        }
        binding.shareLog.setOnClickListener {

            val cacheDir = requireContext().cacheDir
            val file = File(cacheDir, "auto-accounting.log")

            try {
                // 创建文件输出流
                val fileOutputStream = FileOutputStream(file)

                // 将内容写入文件
                fileOutputStream.write(ActiveUtils.getLogList(requireContext()).toByteArray())

                // 刷新缓冲区
                fileOutputStream.flush()

                // 关闭文件输出流
                fileOutputStream.close()

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

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        //UI刷新
        refreshUI()
        return binding.root
    }

    private fun refreshUI() {
        bindActiveUI()
        bindBookAppUI()

    }

    /**
     * 绑定记账软件数据部分的UI
     */
    private fun bindBookAppUI() {
        AppUtils.getService().config {
            binding.book.visibility = if (it.multiBooks) View.VISIBLE else View.GONE
            binding.assets.visibility = if (it.assetManagement) View.VISIBLE else View.GONE
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
    }

    private fun bindBookAppEvents() {

        binding.bookAppContainer.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.book_app_url))
        }
        //资产映射
        binding.map.setOnClickListener {

        }
        //资产管理
        binding.readAssets.setOnClickListener {

        }
        //账本数据
        binding.readBook.setOnClickListener {
            BookSelectorDialog(requireContext()) {
                Logger.i("选择的账本是：${it.name}")
            }.show(cancel = true)
        }
        //分类数据
        binding.readCategory.setOnClickListener {

        }
    }

    private fun bindActiveUI() {
        val colorPrimary = AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary)

        if (!ActiveUtils.getActiveAndSupportFramework(requireContext())) {
            setActive(
                SurfaceColors.SURFACE_3.getColor(requireContext()),
                colorPrimary,
                R.drawable.ic_error
            )
        } else {
            setActive(
                colorPrimary,
                AppUtils.getThemeAttrColor(
                    com.google.android.material.R.attr.colorOnPrimary
                ),
                R.drawable.ic_success
            )
        }
    }


    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun setActive(
        @ColorInt backgroundColor: Int,
        @ColorInt textColor: Int,
        @DrawableRes drawable: Int
    ) {
        binding.active2.setBackgroundColor(backgroundColor)
        binding.imageView2.setImageDrawable(
            AppCompatResources.getDrawable(
                requireActivity(),
                drawable
            )
        )
        val versionName = AppUtils.getVersionName()
        val names = versionName.split("-")
        binding.msgLabel2.text = names[0].trim()
        binding.msgLabel3.text = getString(R.string.releaseInfo)
        binding.imageView2.setColorFilter(textColor)
        binding.msgLabel2.setTextColor(textColor)
        binding.msgLabel3.setTextColor(textColor)
    }


}