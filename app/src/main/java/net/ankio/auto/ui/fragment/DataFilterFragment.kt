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

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import net.ankio.auto.databinding.FragmentDataFilterBinding
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.utils.PrefManager

/**
 * 数据过滤条件管理 Fragment
 *
 * 使用两个多行文本框分别管理白名单和黑名单：
 * - 白名单：数据必须包含任一关键词才进入分析
 * - 黑名单：匹配白名单后，包含任一黑名单关键词则排除
 * - 每行一个关键词，保存时自动去空行
 */
class DataFilterFragment : BaseFragment<FragmentDataFilterBinding>() {

    /**
     * 视图创建完成后的初始化逻辑
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 加载现有数据
        binding.whitelistEdit.setText(PrefManager.dataFilter)
        binding.blacklistEdit.setText(PrefManager.dataFilterBlacklist)

        // 保存按钮
        binding.saveButton.setOnClickListener {
            save()
            findNavController().popBackStack()
        }
    }

    /**
     * 保存白名单和黑名单到配置
     */
    private fun save() {
        PrefManager.dataFilter = binding.whitelistEdit.text?.toString() ?: ""
        PrefManager.dataFilterBlacklist = binding.blacklistEdit.text?.toString() ?: ""
    }
}
