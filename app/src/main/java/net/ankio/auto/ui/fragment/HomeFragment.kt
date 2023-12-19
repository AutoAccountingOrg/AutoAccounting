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
import com.quickersilver.themeengine.ThemeEngine
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentHomeBinding
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.SpUtils
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
        binding.logCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))
        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))
        binding.ruleCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))
        binding.ruleVersion.text = SpUtils.getString("ruleVersionName","None")
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
                val fileUri = FileProvider.getUriForFile(requireContext(), "net.ankio.auto.fileprovider", file)
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)

                // 添加可选的文本标题
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_file))

                // 启动分享意图
                requireContext().startActivity(Intent.createChooser(shareIntent, getString(R.string.share_file)))

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        refreshStatus()
        return binding.root
    }






    override fun onResume() {
        super.onResume()
        refreshStatus()
    }
    /**
     * 获取主题色
     */
    private fun getThemeAttrColor( @AttrRes attrResId: Int): Int {
        return MaterialColors.getColor(ContextThemeWrapper(requireContext(), ThemeEngine.getInstance(requireContext()).getTheme()), attrResId, Color.WHITE)
    }
    private fun setActive(@ColorInt backgroundColor:Int, @ColorInt textColor:Int, @DrawableRes drawable:Int){
        binding.active2.setBackgroundColor(backgroundColor)
        binding.imageView2.setImageDrawable(
            AppCompatResources.getDrawable(
                requireActivity(),
                drawable
            )
        )
        val versionName = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        val names = versionName.split("-")
        binding.msgLabel2.text = names[0].trim()
        binding.msgLabel3.text = getString(R.string.releaseInfo)
        binding.imageView2.setColorFilter(textColor)
        binding.msgLabel2.setTextColor(textColor)
        binding.msgLabel3.setTextColor(textColor)
    }

    private fun refreshStatus(){
        if(!ActiveUtils.getActiveAndSupportFramework()){
            setActive(SurfaceColors.SURFACE_3.getColor(requireContext()),getThemeAttrColor(com.google.android.material.R.attr.colorPrimary), R.drawable.ic_error)
        }else{
            setActive(getThemeAttrColor(com.google.android.material.R.attr.colorPrimary),getThemeAttrColor(com.google.android.material.R.attr.colorOnPrimary),R.drawable.ic_success)
        }
    }

}