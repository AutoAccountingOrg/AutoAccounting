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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.storage.BackupUtils
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.Setting

class MainActivity : BaseActivity() {
    // 视图绑定
     lateinit var binding: ActivityMainBinding
    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment


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

    private fun shouldBackup(): Boolean {
        val lastBackupTime = ConfigUtils.getLong(Setting.LAST_BACKUP_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val fiveMinutesInMillis = 5 * 60 * 1000L
        return (currentTime - lastBackupTime) >= fiveMinutesInMillis
    }

    override fun onStop() {
        super.onStop()
        if (ConfigUtils.getBoolean(Setting.AUTO_BACKUP)) {
            lifecycleScope.launch {
                if (!shouldBackup()) {
                  //  ToastUtils.info(R.string.backup_too_frequent)
                    return@launch
                }

                ConfigUtils.putLong(Setting.LAST_BACKUP_TIME, System.currentTimeMillis())


                ToastUtils.info(R.string.backup_loading)
                val backupUtils = BackupUtils(this@MainActivity)
                runCatching {
                    if (ConfigUtils.getBoolean(Setting.USE_WEBDAV)) {
                        backupUtils.putWebdavBackup(this@MainActivity)
                    } else {
                        backupUtils.putLocalBackup()
                    }
                }.onFailure {
                    Logger.e("自动备份失败", it)
                    ToastUtils.error(R.string.backup_error)
                }.onSuccess {
                    ConfigUtils.putLong("last_backup_time", System.currentTimeMillis())
                    ToastUtils.info(R.string.backup_success)
                }
            }
        }
    }


}
