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

package net.ankio.auto.ui.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.backup.BackupManager
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.slideDown
import net.ankio.auto.ui.utils.slideUp

class HomeActivity : BaseActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val navController = (supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        // 将 BottomNavigation 与 NavController 的绑定延后到下一帧，
        // 避免在 NavController.currentDestination 尚未初始化时触发点击导致 NPE。
        binding.bottomNavigation.post {
            binding.bottomNavigation.setupWithNavController(navController)
        }
        // 监听当前fragment变化
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // 这里判断destination.id是否在底部tab范围内
            val idsToShowBottomNav = setOf(
                R.id.homeFragment,
                R.id.dataFragment,
                R.id.ruleFragment,
                R.id.orderFragment,
                R.id.settingFragment
            )
            if (destination.id in idsToShowBottomNav) {
                binding.bottomNavigation.slideUp()
            } else {
                binding.bottomNavigation.slideDown()
            }
        }

        var lastBackPressedTime = 0L
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navController.popBackStack()) {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressedTime < 2000) {
                        moveTaskToBack(true)
                    } else {
                        lastBackPressedTime = now
                        ToastUtils.info(R.string.press_again_to_exit)
                    }
                }
            }
        });
    }

    override fun onStop() {
        super.onStop()

        Logger.d("HomeActivity onStop - 触发自动备份检查")

        // 使用全局协程作用域执行备份，避免阻塞UI线程
        App.launch {
            BackupManager.autoBackup()
        }
    }

    override fun onDestroy() {
        // 取消所有未完成的动画，防止内存泄漏
        // 这是修复 LeakCanary 报告的 BottomNavigationItemView 内存泄漏问题
        binding.bottomNavigation.clearAnimation()
        binding.bottomNavigation.animate().cancel()
        super.onDestroy()
    }
}
