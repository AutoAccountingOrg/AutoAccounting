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
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.storage.BackupUtils
import net.ankio.auto.ui.api.BaseActivity

class MainActivity : BaseActivity() {
    // 视图绑定
    private val binding  : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private data class NavigationItem(
        val id: Int,
        val selectedIcon: Int,
        val unselectedIcon: Int
    )

    private val barList = listOf(
        NavigationItem(
            R.id.homeFragment,
            R.drawable.bottom_select_home,
            R.drawable.bottom_unselect_home
        ),
        NavigationItem(
            R.id.dataFragment,
            R.drawable.bottom_select_data,
            R.drawable.bottom_unselect_data
        ),
        NavigationItem(
            R.id.settingFragment,
            R.drawable.bottom_select_setting,
            R.drawable.bottom_unselect_setting
        ),
        NavigationItem(
            R.id.dataRuleFragment,
            R.drawable.bottom_select_rule,
            R.drawable.bottom_unselect_rule
        ),
        NavigationItem(
            R.id.orderFragment,
            R.drawable.bottom_select_order,
            R.drawable.bottom_unselect_order
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // 初始化底���导航栏
        onBottomViewInit()
        BackupUtils.initRequestPermission(this)
    }
    /**
     * 导航栏初始化
     */
    private fun onBottomViewInit() {
        setupNavigationComponents()
        setupDestinationChangeListener()
    }

    private val bottomNavigationView: BottomNavigationView by lazy {
        binding.bottomNavigation
    }
    private lateinit var navHostFragment: NavHostFragment

    private fun setupNavigationComponents() {
        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
            ?: throw IllegalStateException("NavHostFragment not found")

       // binding.navHostFragment.setPadding(0,0,0,0)

        val navController = navHostFragment.navController
        NavigationUI.setupWithNavController(bottomNavigationView, navController)
    }

    /**
     * 设置目标更改监听器
     */
    private fun setupDestinationChangeListener() {
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNavigationIcons(destination.id)
            updateBottomNavigationVisibility(destination.id)
        }
    }
    /**
     * 更新导航图标
     */
    private fun updateNavigationIcons(currentDestinationId: Int) {
        barList.forEach { (itemId, selectedIcon, unselectedIcon) ->
            bottomNavigationView.menu.findItem(itemId).setIcon(
                if (itemId == currentDestinationId) selectedIcon else unselectedIcon
            )
        }
    }

    /**
     * 更新底部导航栏可见性
     */
    private fun updateBottomNavigationVisibility(destinationId: Int) {
        val shouldShow = barList.any { it.id == destinationId }
        
        if (shouldShow && bottomNavigationView.visibility == View.GONE) {
            // 显示动画
            bottomNavigationView.apply {
                visibility = View.VISIBLE
                alpha = 0f
                translationY = height.toFloat()
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            }
        } else if (!shouldShow && bottomNavigationView.visibility == View.VISIBLE) {
            // 隐藏动画
            bottomNavigationView.animate()
                .alpha(0f)
                .translationY(bottomNavigationView.height.toFloat())
                .setDuration(300)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction {
                    bottomNavigationView.visibility = View.GONE
                }
                .start()
        }
    }

    /**
     * 应用停止
     */
    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            runCatching {
                BackupUtils.autoBackup(this@MainActivity)
            }
        }
    }

}
