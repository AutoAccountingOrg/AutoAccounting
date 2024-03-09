/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.AutoAccountingServiceUtils

abstract class BaseFragment : Fragment() {
    open val menuList: ArrayList<MenuItem> = arrayListOf()

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    protected lateinit var activityBinding: ActivityMainBinding

    private fun goToServiceFragment() {
        // 获取 NavController
        val navController = (activity as MainActivity).getNavController()

// 弹出当前 Fragment
        navController.popBackStack()

// 导航到新的 Fragment
        navController.navigate(R.id.serviceFragment)

    }

    override fun onResume() {
        if (this.javaClass.simpleName != "ServiceFragment") {
            try {
                lifecycleScope.launch {
                    if (!AutoAccountingServiceUtils.isServerStart(requireContext())) {
                        withContext(Dispatchers.Main) {
                            goToServiceFragment()
                        }
                    }
                }
            } catch (e: Exception) {
                goToServiceFragment()
            }
        }
        super.onResume()
        if (!this::activityBinding.isInitialized) {
            activityBinding = (activity as MainActivity).getBinding()
        }
        activityBinding.toolbar.visibility = View.VISIBLE
        // 重置顶部导航栏图标
        activityBinding.toolbar.menu.clear()
        //添加菜单
        menuList.forEach {
            addMenuItem(it)
        }


     }

    private fun addMenuItem(menuItemObject: MenuItem) {
        val menu = activityBinding.toolbar.menu
        val menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, getString(menuItemObject.title))
        menuItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        val icon = AppCompatResources.getDrawable(requireActivity(), menuItemObject.drawable)
        if (icon != null) {
            menuItem.setIcon(icon)
            DrawableCompat.setTint(
                icon,
                AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorOnPrimary)
            )

        }
        menuItem.setOnMenuItemClickListener {
            menuItemObject.callback.invoke((activity as MainActivity).getNavController())
            true
        }
    }
}