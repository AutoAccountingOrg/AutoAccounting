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
import net.ankio.auto.databinding.ActivityIntroBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.ui.adapter.IntroPagerAdapter.IntroPage
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.vm.IntroSharedVm
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.ServiceManager

class MainActivity : BaseActivity() {
    private val binding: ActivityIntroBinding by lazy {
        ActivityIntroBinding.inflate(layoutInflater)
    }
    private val vm: IntroSharedVm by viewModels()

    /** 服务管理器，负责处理服务启动和权限管理 */
    private val serviceManager = ServiceManager.create()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Logger.i("PrefManager.introIndex = ${PrefManager.introIndex}")

        // 初始化服务管理器
        serviceManager.initialize(
            caller = this,
            onReady = {
                // 权限就绪后启动服务并检查是否需要跳转到主页
                serviceManager.startCoreService(this, forceStart = true)
                if (PrefManager.introIndex + 1 >= IntroPage.entries.size) {
                    start<HomeActivity>()
                }
            },
            onDenied = {
                // 权限被拒绝时重新请求
                serviceManager.requestProjectionPermission()
            }
        )

        // 尝试启动核心服务
        startCoreService()

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
            startCoreService()
            binding.viewPager.setCurrentItem(idx.ordinal, true)
        }
    }

    /**
     * 启动核心服务
     * 使用ServiceManager统一管理服务启动逻辑
     */
    private fun startCoreService() {
        val started = serviceManager.startCoreService(this)
        Logger.i("服务启动结果: $started，状态: ${serviceManager.getServiceStatus()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放服务管理器资源
        serviceManager.release()
    }

}