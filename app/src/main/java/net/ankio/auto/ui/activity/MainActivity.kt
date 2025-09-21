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

/**
 * 应用主入口 Activity。
 *
 * 负责：
 * - 根据引导页完成状态，决定跳转到 `HomeActivity` 或显示引导页。
 * - 在非 Xposed 工作模式下启动核心服务 `CoreService`。
 */
class MainActivity : BaseActivity() {
    private val binding: ActivityIntroBinding by lazy {
        ActivityIntroBinding.inflate(layoutInflater)
    }
    private val vm: IntroSharedVm by viewModels()

    // 记录上次的工作模式，用于检测变化
    private var lastWorkMode: WorkMode? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化当前工作模式记录
        lastWorkMode = PrefManager.workMode
        
        // 非 Xposed 模式下启动核心服务
        if (WorkMode.isOcrOrLSPatch()) CoreService.start(this, intent)
        Logger.d("初始化完成")
        if (PrefManager.introIndex + 1 >= IntroPage.entries.size) {
            start<HomeActivity>(true)
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

            // 检查工作模式是否发生变化
            val currentWorkMode = PrefManager.workMode
            if (lastWorkMode != null && lastWorkMode != currentWorkMode) {
                Logger.i("工作模式变更: $lastWorkMode -> $currentWorkMode，重启服务")
                CoreService.restart(this, intent)
            } else if (lastWorkMode == null && WorkMode.isOcrOrLSPatch()) {
                // 首次启动且为非Xposed模式，启动服务
                Logger.i("首次启动服务，模式: $currentWorkMode")
                CoreService.start(this, intent)
            }

            // 更新记录的模式
            lastWorkMode = currentWorkMode
            binding.viewPager.setCurrentItem(idx.ordinal, true)
        }
    }

}