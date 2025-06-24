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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.ActivityIntroBinding
import net.ankio.auto.service.CoreService
import net.ankio.auto.service.utils.ProjectionGateway
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
        //已经在引导页面呆过，直接进入主页

        Logger.i("PrefManager.introIndex = ${PrefManager.introIndex}")

        projectionLauncher = ProjectionGateway.register(this, onReady = {
            CoreService.start(this)
            if (PrefManager.introIndex + 1 >= IntroPage.entries.size) {
                start<HomeActivity>()
            }
        }, onDenied = {
            projectionLauncher.launch(Unit)
        })

        startCoreService()

        if (PrefManager.introIndex + 1 >= IntroPage.entries.size) {
            return
        }


        setContentView(binding.root)

        binding.viewPager.adapter = IntroPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false



        binding.viewPager.setCurrentItem(PrefManager.introIndex, false)

        vm.pageRequest.observe(this) { idx ->
            PrefManager.introIndex = idx.ordinal
            startCoreService()
            binding.viewPager.setCurrentItem(idx.ordinal, true)
        }


    }

    private lateinit var projectionLauncher: ActivityResultLauncher<Unit>

    private fun startCoreService() {
        // 引导页未完成？直接返回
        if (PrefManager.introIndex < IntroPage.APP.ordinal) return

        // 非 OCR 模式 → 无需截屏授权
        if (PrefManager.workMode != WorkMode.Ocr) {
            CoreService.start(this)
            return
        }

        // OCR 模式
        if (ProjectionGateway.isReady()) {
            CoreService.start(this)
        } else {
            projectionLauncher.launch(Unit)   // 弹一次系统授权窗
        }
    }

}