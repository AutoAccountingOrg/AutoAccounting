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

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.storage.BackupUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.Github

class MainActivity : BaseActivity() {
    // 视图绑定
     lateinit var binding: ActivityMainBinding
    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment

    private var hasLogin = false
    private fun onLogin() {
        if (hasLogin) return
        val uri = intent.data
        Logger.d("Login From Github: $uri")
        if (uri != null) {
            // val dialog = DialogUtil.createLoadingDialog(this, getString(R.string.auth_waiting))
            val code = uri.getQueryParameter("code")
            lifecycleScope.launch {
                runCatching {
                    Github.parseAuthCode(code)
                }.onFailure {
                    Logger.e("Error on login", it)
                    ToastUtils.error(it.message!!)
                }.onSuccess {
                    ToastUtils.info(R.string.auth_success)
                }
            }
            hasLogin = true
        }
    }

    private val barList =
        arrayListOf(
            arrayListOf(
                R.id.homeFragment,
                R.drawable.bottom_select_home,
                R.drawable.bottom_unselect_home,
            ),
            arrayListOf(
                R.id.dataFragment,
                R.drawable.bottom_select_data,
                R.drawable.bottom_unselect_data,
            ),
            arrayListOf(
                R.id.settingFragment,
                R.drawable.bottom_select_setting,
                R.drawable.bottom_unselect_setting,
            ),
            arrayListOf(
                R.id.dataRuleFragment,
                R.drawable.bottom_select_rule,
                R.drawable.bottom_unselect_rule
            ),
            arrayListOf(
                R.id.orderFragment,
                R.drawable.bottom_select_order,
                R.drawable.bottom_unselect_order,
            ),
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Github登录
        onLogin()
        // 备份注册
        onBackup()
        // 初始化底部导航栏
        onBottomViewInit()
    }


    private fun onBottomViewInit() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragmentContainerView = binding.navHostFragment
        bottomNavigationView = binding.bottomNavigation

        toolbarLayout = binding.toolbarLayout
        toolbar = binding.toolbar

        navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?)!!

        val appBarConfiguration =
            AppBarConfiguration.Builder(*barList.map { it[0] }.toIntArray()).build()

        NavigationUI.setupWithNavController(
            toolbar!!,
            navHostFragment.navController,
            appBarConfiguration,
        )
        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.navController)

        navHostFragment.navController.addOnDestinationChangedListener { controller, navDestination, _ ->

            // 重置底部导航栏图标
            barList.forEach {
                bottomNavigationView.menu.findItem(it[0]).setIcon(it[2])
                if (it[0] == navDestination.id) {
                    bottomNavigationView.menu.findItem(it[0]).setIcon(it[1])
                }
            }

            // 如果目标不在barList里面则隐藏底部导航栏

            if (barList.any { it[0] == navDestination.id }) {
                bottomNavigationView.visibility = View.VISIBLE
            } else {
                bottomNavigationView.visibility = View.GONE
            }
        }
    }

    lateinit var backupLauncher: ActivityResultLauncher<Uri?>

    lateinit var restoreLauncher: ActivityResultLauncher<Array<String>>

    private fun onBackup() {
        BackupUtils.registerBackupLauncher(this).let {
            backupLauncher = it
        }

        BackupUtils.registerRestoreLauncher(this).let {
            restoreLauncher = it
        }
    }



}
