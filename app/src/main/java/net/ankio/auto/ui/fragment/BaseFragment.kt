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

import android.view.Menu
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.AppUtils

abstract class BaseFragment:Fragment() {
   open val menuList:ArrayList<MenuItem> = arrayListOf()

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    private lateinit var  activityBinding : ActivityMainBinding

    override fun onResume() {
        super.onResume()
        if(!this::activityBinding.isInitialized){
            activityBinding =  (activity as MainActivity).getBinding()
        }
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
            DrawableCompat.setTint(
                icon,
                AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorOnBackground)
            )
            menuItem.setIcon(icon)
        }
        menuItem.setOnMenuItemClickListener {
            menuItemObject.callback.invoke((activity as MainActivity).getNavController())
            true
        }
    }
}