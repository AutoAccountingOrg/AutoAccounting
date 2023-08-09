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
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import net.ankio.auto.R
import net.ankio.auto.databinding.AboutDialogBinding
import net.ankio.auto.databinding.ActivityMainBinding
import rikka.html.text.toHtml


class MainActivity : BaseActivity() {

    //视图绑定
    private lateinit var binding: ActivityMainBinding

    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragmentContainerView = binding.navHostFragment
        bottomNavigationView = binding.bottomNavigation

        toolbarLayout = binding.toolbarLayout
        toolbar = binding.toolbar
        bottomNavigationView.addNavigationBarBottomPadding()
        scrollView = binding.scrollView
        navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?)!!


       val appBarConfiguration = AppBarConfiguration.Builder(
           R.id.homeFragment,
           R.id.dataFragment,
           R.id.settingFragment,
           R.id.ruleFragment,
           R.id.orderFragment
        ).build()

        NavigationUI.setupWithNavController(toolbar, navHostFragment.navController,appBarConfiguration)
        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.navController);
        // 添加 Navigate 跳转监听，如果参数不带 ShowAppBar 将不显示底部导航栏
        navHostFragment.navController.addOnDestinationChangedListener { controller, navDestination, _ ->
            bottomNavigationView.menu.findItem(R.id.homeFragment).setIcon(R.drawable.unselect_home);
            bottomNavigationView.menu.findItem(R.id.dataFragment).setIcon(R.drawable.unselect_data);
            bottomNavigationView.menu.findItem(R.id.settingFragment).setIcon(R.drawable.unselect_setting);
            bottomNavigationView.menu.findItem(R.id.ruleFragment).setIcon(R.drawable.unselect_rule);
            bottomNavigationView.menu.findItem(R.id.orderFragment).setIcon(R.drawable.unselect_order);
            clearMenuItems();
            if (navDestination.id == R.id.homeFragment
                || navDestination.id == R.id.dataFragment
                || navDestination.id == R.id.settingFragment
                || navDestination.id == R.id.ruleFragment
                || navDestination.id == R.id.orderFragment) {
                bottomNavigationView.visibility = View.VISIBLE;
            } else {
                bottomNavigationView.visibility = View.GONE;
            }

            when (navDestination.id) {
                R.id.homeFragment -> {
                    addMenuItem(R.string.title_setting,R.drawable.item_setting)
                    addMenuItem(R.string.title_more,R.drawable.item_more)
                    toolbar.title = getString(R.string.title_home)
                    bottomNavigationView.menu.findItem(R.id.homeFragment).setIcon(R.drawable.select_home);
                }
                R.id.dataFragment -> {
                    toolbar.title = getString(R.string.title_data)
                    bottomNavigationView.menu.findItem(R.id.dataFragment).setIcon(R.drawable.select_data);
                }
                R.id.settingFragment -> {
                    toolbar.title = getString(R.string.title_setting)
                    bottomNavigationView.menu.findItem(R.id.settingFragment).setIcon(R.drawable.select_setting);
                }
                R.id.ruleFragment -> {
                    toolbar.title = getString(R.string.title_rule)
                    bottomNavigationView.menu.findItem(R.id.ruleFragment).setIcon(R.drawable.select_rule);
                }

                R.id.orderFragment->{
                    toolbar.title = getString(R.string.title_order)
                    bottomNavigationView.menu.findItem(R.id.orderFragment).setIcon(R.drawable.select_order);
                }

                R.id.setting2Fragment->{
                    toolbar.title = getString(R.string.title_setting2)
                }

            }
        }


        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                getString(R.string.title_more) -> {
                    val binding = AboutDialogBinding.inflate(LayoutInflater.from(this), null, false)
                    binding.sourceCode.movementMethod = LinkMovementMethod.getInstance()
                    binding.sourceCode.text = getString(
                        R.string.about_view_source_code,
                        "<b><a href=\"https://github.com/Auto-Accounting/AutoAccounting\n\">GitHub</a></b>"
                    ).toHtml()

                    binding.versionName.text = packageManager.getPackageInfo(packageName, 0).versionName
                    MaterialAlertDialogBuilder(this)
                        .setView(binding.root)
                        .show()
                    true
                }
                getString(R.string.title_setting) -> {
                    navHostFragment.navController.navigate(R.id.setting2Fragment)
                    true
                }
                else -> false
            }

        }
        onViewCreated()
    }

    // Method to dynamically add a new menu item
    private fun addMenuItem(@StringRes stringResId: Int,@DrawableRes iconResId: Int) {
        val menu = toolbar.menu
        val menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, getString(stringResId))
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        val icon =  AppCompatResources.getDrawable(this,iconResId)
        if (icon != null) {
            DrawableCompat.setTint(icon, getThemeAttrColor(com.google.android.material.R.attr.colorOnBackground))
            menuItem.setIcon(icon)
        }
    }

    // Method to clear the existing menu items
    private fun clearMenuItems() {
        toolbar.menu.clear()
    }




}