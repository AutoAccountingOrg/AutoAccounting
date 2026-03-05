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
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.storage.backup.BackupManager
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.ui.utils.slideDown
import net.ankio.auto.ui.utils.slideUp
import net.ankio.auto.ui.vm.HomeActivityVm
import net.ankio.auto.update.AppUpdateHelper
import net.ankio.auto.update.RuleUpdateHelper
import net.ankio.auto.utils.CustomTabsHelper

class HomeActivity : BaseActivity() {
    private val vm: HomeActivityVm by viewModels()

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    /** 注册 VM 的 LiveData 观察，用于展示规则/应用更新弹窗 */
    private fun setupUpdateListeners() {
        vm.appUpdateModel.observe(this) { model ->
            if (model == null) return@observe
            vm.consumeAppUpdate()
            BaseSheetDialog.create<UpdateDialog>(this)
                .setUpdateModel(model)
                .setRuleTitle(getString(R.string.app))
                .setOnClickUpdate {
                    CustomTabsHelper.launchUrl(AppUpdateHelper.buildApkUrl(model.version).toUri())
                }
                .show()
        }

        vm.ruleUpdateModel.observe(this) { model ->
            if (model == null) return@observe
            vm.consumeRuleUpdate()
            BaseSheetDialog.create<UpdateDialog>(this)
                .setUpdateModel(model)
                .setRuleTitle(getString(R.string.rule))
                .setOnClickUpdate {
                    lifecycleScope.launch {
                        RuleUpdateHelper.updateRule(this@HomeActivity, model)
                        vm.refreshStatus(this@HomeActivity)
                    }
                }
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupUpdateListeners()
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

    override fun onResume() {
        super.onResume()
        vm.startSyncUpdateTask()
    }

    override fun onStop() {
        super.onStop()
        vm.startAutoBackup()
    }

    override fun onDestroy() {
        // 取消所有未完成的动画，防止内存泄漏
        // 这是修复 LeakCanary 报告的 BottomNavigationItemView 内存泄漏问题
        binding.bottomNavigation.clearAnimation()
        binding.bottomNavigation.animate().cancel()
        super.onDestroy()
    }
}
