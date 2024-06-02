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

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hjq.toast.Toaster
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.events.AutoServiceErrorEvent
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.BackupUtils
import net.ankio.auto.utils.Github
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.event.EventBus

class MainActivity : BaseActivity() {
    // 视图绑定
    private lateinit var binding: ActivityMainBinding
    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment

    private fun onLogin() {
        val uri = intent.data
        if (uri != null) {
            // val dialog = DialogUtil.createLoadingDialog(this, getString(R.string.auth_waiting))
            val code = uri.getQueryParameter("code")
            lifecycleScope.launch {
                runCatching {
                    Github.parseAuthCode(code)
                }.onFailure {
                    Toaster.show(it.message)
                }.onSuccess {
                    Toaster.show(R.string.auth_success)
                }
            }
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
            arrayListOf(R.id.logFragment, R.drawable.bottom_select_log, R.drawable.bottom_unselect_log),
            arrayListOf(
                R.id.orderFragment,
                R.drawable.bottom_select_order,
                R.drawable.bottom_unselect_order,
            ),
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.register(AutoServiceErrorEvent::class.java, autoListener)
        // Github登录
        onLogin()
        // 备份注册
        onBackup()
        // 初始化底部导航栏
        onBottomViewInit()
        // 检查规则更新
        lifecycleScope.launch(Dispatchers.IO) {
            checkUpdate()
        }

        onViewCreated()

        lifecycleScope.launch {
            AppUtils.getService().connect()
        }
    }

    fun onBottomViewInit() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragmentContainerView = binding.navHostFragment
        bottomNavigationView = binding.bottomNavigation

        toolbarLayout = binding.toolbarLayout
        toolbar = binding.toolbar
        bottomNavigationView.addNavigationBarBottomPadding()

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

    fun getBinding(): ActivityMainBinding {
        return binding
    }

    fun getNavController(): NavController {
        return navHostFragment.navController
    }

    private val autoListener = { event: AutoServiceErrorEvent ->
        Logger.e("自动记账服务未连接", event.exception)
        runOnUiThread {
            // 判断当前fragment是否为serviceFragment
            if (navHostFragment.navController.currentDestination?.id != R.id.serviceFragment) {
                navHostFragment.navController.navigate(R.id.serviceFragment)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.unregister(AutoServiceErrorEvent::class.java, autoListener)
    }
}
