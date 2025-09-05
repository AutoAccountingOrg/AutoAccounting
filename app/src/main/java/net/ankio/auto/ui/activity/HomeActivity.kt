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
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
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
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
            ?: throw IllegalStateException("NavHostFragment not found")
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

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
        binding.bottomNavigation.setOnItemSelectedListener { item: MenuItem ->
            if (navController.currentDestination?.parent == null) {
                true
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }


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
