/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.activity

import android.os.Bundle
import androidx.activity.viewModels
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.ActivityIntroBinding
import net.ankio.auto.service.CoreService
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.ui.adapter.IntroPagerAdapter.IntroPage
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.vm.IntroSharedVm
import net.ankio.auto.utils.PrefManager

class MainActivity : BaseActivity() {
    private val binding: ActivityIntroBinding by lazy {
        ActivityIntroBinding.inflate(layoutInflater)
    }
    private val vm: IntroSharedVm by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoreService.start(this, intent)
        Logger.d("初始化完成")
        if (PrefManager.introIndex + 1 >= IntroPage.entries.size) {
            if (PrefManager.workMode !== WorkMode.Xposed) start<HomeActivity>(true)
            return
        }


        // 如果引导页已完成，直接返回不显示引导界面
        if (PrefManager.introIndex + 1 >= IntroPage.entries.size) {
            return
        }

        // 设置引导页界面
        setContentView(binding.root)
        binding.viewPager.adapter = IntroPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.setCurrentItem(PrefManager.introIndex, false)

        // 监听页面切换请求
        vm.pageRequest.observe(this) { idx ->
            PrefManager.introIndex = idx.ordinal
            CoreService.start(this, intent)
            binding.viewPager.setCurrentItem(idx.ordinal, true)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }

}