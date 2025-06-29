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
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.storage.BackupUtils
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.fragment.plugin.HomeFragment
import net.ankio.auto.utils.PrefManager
import androidx.core.view.get
import net.ankio.auto.storage.Logger
import androidx.core.view.size
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import net.ankio.auto.service.CoreService
import net.ankio.auto.ui.fragment.plugin.DataFragment
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.ui.utils.slideDown
import net.ankio.auto.ui.utils.slideUp
import kotlin.system.exitProcess

class HomeActivity : BaseActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val navController = (supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        binding.bottomNavigation.setupWithNavController(navController)
        // 监听当前fragment变化
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // 这里判断destination.id是否在底部tab范围内
            val idsToShowBottomNav = setOf(
                R.id.homeFragment,
                R.id.dataFragment
                //TODO ...其他底部tab id
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


}
