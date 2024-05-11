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

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.view.Menu
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.AppUtils

abstract class BaseFragment : Fragment() {
    open val menuList: ArrayList<MenuItem> = arrayListOf()

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    protected lateinit var activityBinding: ActivityMainBinding

    private var init = false
    lateinit var scrollView: View

    override fun onResume() {
        super.onResume()
        val mainActivity = activity as MainActivity
        if (!this::activityBinding.isInitialized) {
            activityBinding = mainActivity.getBinding()
        }
        if (init)return
        activityBinding.toolbar.visibility = View.VISIBLE
        // 重置顶部导航栏图标
        activityBinding.toolbar.menu.clear()
        // 添加菜单
        menuList.forEach {
            addMenuItem(it)
        }
        if (mainActivity.toolbarLayout != null && ::scrollView.isInitialized) {
            var animatorStart = false
            // 滚动页面调整toolbar颜色
            scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                var scrollYs = scrollY // 获取宽度
                if (scrollView is RecyclerView) {
                    // RecyclerView获取真实高度
                    scrollYs = (scrollView as RecyclerView).computeVerticalScrollOffset()
                }

                if (animatorStart) return@setOnScrollChangeListener

                if (scrollYs.toFloat() > 0) {
                    if (mainActivity.last != mainActivity.mStatusBarColor2) {
                        animatorStart = true
                        viewBackgroundGradientAnimation(
                            mainActivity.toolbarLayout!!,
                            mainActivity.mStatusBarColor!!,
                            mainActivity.mStatusBarColor2!!,
                        )
                        mainActivity.last = mainActivity.mStatusBarColor2
                    }
                } else {
                    if (mainActivity.last != mainActivity.mStatusBarColor) {
                        animatorStart = true
                        viewBackgroundGradientAnimation(
                            mainActivity.toolbarLayout!!,
                            mainActivity.mStatusBarColor2!!,
                            mainActivity.mStatusBarColor!!,
                        )
                        mainActivity.last = mainActivity.mStatusBarColor
                    }
                }
                animatorStart = false
            }

            scrollView.addNavigationBarBottomPadding()
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
                AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorOnBackground),
            )
        }
        menuItem.setOnMenuItemClickListener {
            menuItemObject.callback.invoke((activity as MainActivity).getNavController())
            true
        }
    }

    /**
     * toolbar颜色渐变动画
     */
    private fun viewBackgroundGradientAnimation(
        view: View,
        fromColor: Int,
        toColor: Int,
        duration: Long = 600,
    ) {
        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnimator.addUpdateListener { animation ->
            val color = animation.animatedValue as Int // 之后就可以得到动画的颜色了
            view.setBackgroundColor(color) // 设置一下, 就可以看到效果.
        }
        colorAnimator.duration = duration
        colorAnimator.start()
    }
}
