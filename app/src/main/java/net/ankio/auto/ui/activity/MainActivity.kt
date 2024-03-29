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
import com.hjq.toast.Toaster
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.AutoAccountingServiceUtils
import net.ankio.auto.utils.BackupUtils
import net.ankio.auto.utils.BookSyncUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.Github
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.UpdateUtils


class MainActivity : BaseActivity() {


    //视图绑定
    private lateinit var binding: ActivityMainBinding

    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment

    private fun onLogin() {
        val uri = intent.data
        if (uri != null) {
            //val dialog = DialogUtil.createLoadingDialog(this, getString(R.string.auth_waiting))

            val code = uri.getQueryParameter("code")

            Github.parseAuthCode(code, {

            }, {
                runOnUiThread {
                    Toaster.show(it)
                }
             //   DialogUtil.closeDialog(dialog)
            })
        }
    }


    val barList = arrayListOf(
        arrayListOf(R.id.homeFragment, R.drawable.bottom_select_home, R.drawable.bottom_unselect_home),
        arrayListOf(R.id.dataFragment, R.drawable.bottom_select_data, R.drawable.bottom_unselect_data),
        arrayListOf(R.id.settingFragment, R.drawable.bottom_select_setting, R.drawable.bottom_unselect_setting),
        arrayListOf(R.id.logFragment, R.drawable.bottom_select_log, R.drawable.bottom_unselect_log),
        arrayListOf(R.id.orderFragment, R.drawable.bottom_select_order, R.drawable.bottom_unselect_order),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Github登录
        onLogin()
        //备份注册
        onBackup()
        //初始化底部导航栏
        onBottomViewInit()


        //检查自动记账服务
        lifecycleScope.launch {
            checkAutoService()
        }

        onViewCreated()

    }


    fun onBottomViewInit(){
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragmentContainerView = binding.navHostFragment
        bottomNavigationView = binding.bottomNavigation

        toolbarLayout = binding.toolbarLayout
        toolbar = binding.toolbar
        bottomNavigationView.addNavigationBarBottomPadding()
        scrollView = binding.scrollView
        navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?)!!


        val appBarConfiguration = AppBarConfiguration.Builder(*barList.map { it[0] }.toIntArray()).build()

        NavigationUI.setupWithNavController(
            toolbar,
            navHostFragment.navController,
            appBarConfiguration
        )
        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.navController)


        navHostFragment.navController.addOnDestinationChangedListener { controller, navDestination, _ ->

            // 重置底部导航栏图标
            barList.forEach {
                bottomNavigationView.menu.findItem(it[0]).setIcon(it[2])
                if (it[0] == navDestination.id){
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




    private suspend fun checkAutoService() = withContext(Dispatchers.IO){

        runCatching {
            if(!AutoAccountingServiceUtils.isServerStart(this@MainActivity)){
                throw Exception("自动记账服务未连接")
            }
        }.onFailure {
            //如果服务没启动，则跳转到服务未启动界面
            Logger.e("自动记账服务未连接",it)
            withContext(Dispatchers.Main){
                navHostFragment.navController.navigate(R.id.serviceFragment)
            }
        }.onSuccess {

            withContext(Dispatchers.Main){
                runCatching {
                    checkBookApp()
                    checkUpdate()
                }.onFailure {
                    Logger.e("检查更新失败",it)

                }
            }
            AppUtils.logger = true
        }
    }

    private fun checkBookApp(){
        //判断是否设置了记账软件
        if (SpUtils.getString("bookApp", "").isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_book_app)
                .setMessage(R.string.msg_book_app)
                .setPositiveButton(R.string.sure_book_app) { _, _ ->
                    CustomTabsHelper.launchUrlOrCopy(this,getString(R.string.book_app_url))
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    //finish()
                }
                .show()
        }
    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            BookSyncUtils.sync(this@MainActivity)
        }
    }

    private fun checkUpdate() {
        val updateUtils = UpdateUtils()
        updateUtils.checkAppUpdate { version, log, date, download,code ->
          UpdateDialog(this, hashMapOf("url" to download), log, version, date, 0,code).show(cancel = true)
        }
        updateUtils.checkRuleUpdate { version, log, date, category, rule,code ->
          UpdateDialog(this, hashMapOf("category" to category, "rule" to rule), log, version, date, 1,code).show(cancel = true)
        }
    }

    fun getBinding(): ActivityMainBinding {
        return binding
    }

    fun getNavController(): NavController {
        return navHostFragment.navController
    }


}