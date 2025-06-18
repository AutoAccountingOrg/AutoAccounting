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
import net.ankio.auto.service.CoreService
import net.ankio.auto.ui.fragment.plugin.DataFragment

class HomeActivity : BaseActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private data class NavigationItem(
        @StringRes val label: Int,
        val fragmentProvider: () -> Fragment,
        val selectedIcon: Int,
        val unselectedIcon: Int
    )

    private val bottomNavigationView by lazy { binding.bottomNavigation }

    // 当前 Fragment tag
    private var currentTag: String? = null

    // Fragment 缓存
    private val fragmentMap = mutableMapOf<String, Fragment>()

    private fun bottomList(): List<NavigationItem> {
        val list = ArrayList<NavigationItem>()
        if (PrefManager.bookApp === BuildConfig.APPLICATION_ID) {
            // 填入真实内容
        } else {
            list.add(
                NavigationItem(
                    label = R.string.title_home,
                    fragmentProvider = { HomeFragment() },
                    R.drawable.bottom_select_home,
                    R.drawable.bottom_unselect_home
                ),
            )
            list.add(
                NavigationItem(
                    label = R.string.title_data,
                    fragmentProvider = { DataFragment() },
                    R.drawable.bottom_select_data,
                    R.drawable.bottom_select_data
                ),
            )
            // 更多 fragment 可添加于此
        }
        return list
    }

    private lateinit var barList: List<NavigationItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        barList = bottomList()
        initBottomNavigation()
        //BackupUtils.initRequestPermission(this)
        // 默认加载第一个页面
        if (savedInstanceState == null && barList.isNotEmpty()) {
            switchFragment(0)
        }
    }

    private fun initBottomNavigation() {
        val menu = bottomNavigationView.menu
        barList.forEachIndexed { index, item ->
            menu.add(0, index, index, item.label).apply {
                setIcon(item.unselectedIcon)
            }
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            switchFragment(menuItem.itemId)
            true
        }
    }

    private fun switchFragment(index: Int) {
        val tag = "fragment_$index"
        if (tag == currentTag) return

        val transaction = supportFragmentManager.beginTransaction()

        val newFragment = fragmentMap.getOrPut(tag) {
            barList[index].fragmentProvider()
        }

        supportFragmentManager.fragments.forEach {
            transaction.hide(it)
        }

        if (newFragment.isAdded) {
            transaction.show(newFragment)
        } else {
            transaction.add(R.id.nav_host_fragment, newFragment, tag)
        }

        transaction.commitNowAllowingStateLoss()
        currentTag = tag
        updateIcons(index)
    }

    private fun updateIcons(selectedIndex: Int) {
        for (i in barList.indices) {
            val iconRes = if (i == selectedIndex)
                barList[i].selectedIcon
            else
                barList[i].unselectedIcon
            bottomNavigationView.menu[i].icon = getDrawable(iconRes)
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            runCatching {
                BackupUtils.autoBackup(this@HomeActivity)
            }
        }
    }
}

private fun Int.toId(): Int = 1000 + this // 避免与系统资源 ID 冲突
