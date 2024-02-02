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


import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.elevation.SurfaceColors
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import com.zackratos.ultimatebarx.ultimatebarx.addStatusBarTopPadding
import com.zackratos.ultimatebarx.ultimatebarx.navigationBar
import com.zackratos.ultimatebarx.ultimatebarx.statusBar
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.LanguageUtils

/**
 * 基础的BaseActivity
 */
open class BaseActivity : AppCompatActivity() {
    lateinit var toolbarLayout: AppBarLayout
    lateinit var toolbar: MaterialToolbar
    lateinit var scrollView: View
    var tag = "BaseActivity"

    /**
     * 重构context
     */
    override fun attachBaseContext(newBase: Context?) {
       val context = newBase?.let {
           LanguageUtils.initAppLanguage(it)
        }
        super.attachBaseContext(context)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //主题初始化
        ThemeEngine.applyToActivity(this)
    }

    /**
     * 在子activity手动调用该方法
     */
    fun onViewCreated(){

        //主题初始化
        val themeMode = ThemeEngine.getInstance(this@BaseActivity).themeMode

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        statusBar {
            fitWindow = false
            background.transparent()
            light =
                !(themeMode == ThemeMode.DARK || (themeMode == ThemeMode.AUTO && currentNightMode == Configuration.UI_MODE_NIGHT_YES))
        }
        //根据主题设置statusBar
        navigationBar { transparent() }
        if(::toolbarLayout.isInitialized){
            toolbarLayout.addStatusBarTopPadding()
        }
        if(::toolbarLayout.isInitialized && ::toolbar.isInitialized && ::scrollView.isInitialized){
            val mStatusBarColor = getThemeAttrColor(android.R.attr.colorBackground)
            var last = mStatusBarColor
            val mStatusBarColor2 =  SurfaceColors.SURFACE_4.getColor(this)
            var animatorStart = false
            //滚动页面调整toolbar颜色
            scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                var scrollYs = scrollY //获取宽度
                if(scrollView is RecyclerView){
                    //RecyclerView获取真实高度
                    scrollYs = (scrollView as RecyclerView).computeVerticalScrollOffset();
                }

                if(animatorStart)return@setOnScrollChangeListener

                if(scrollYs.toFloat()>0){
                    if (last!=mStatusBarColor2){
                        animatorStart = true
                        viewBackgroundGradientAnimation(toolbarLayout,mStatusBarColor,mStatusBarColor2)
                        last=mStatusBarColor2
                    }
                }else{
                    if (last!=mStatusBarColor){
                        animatorStart = true
                        viewBackgroundGradientAnimation(toolbarLayout,mStatusBarColor2,mStatusBarColor)
                        last=mStatusBarColor
                    }


                }
                animatorStart = false
            }

            scrollView.addNavigationBarBottomPadding()

        }
    }

    /**
     * 获取主题色
     */
    fun getThemeAttrColor( @AttrRes attrResId: Int): Int {
       return AppUtils.getThemeAttrColor(attrResId)
    }



    /**
     * toolbar颜色渐变动画
     */
    private fun viewBackgroundGradientAnimation(view: View, fromColor: Int, toColor: Int, duration: Long = 600) {
        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnimator.addUpdateListener { animation ->
            val color = animation.animatedValue as Int //之后就可以得到动画的颜色了
            view.setBackgroundColor(color) //设置一下, 就可以看到效果.
        }
        colorAnimator.duration = duration
        colorAnimator.start()
    }



    /**
     * 切换activity
     */
    inline fun <reified T : BaseActivity> Context.start() {
        val intent = Intent(this, T::class.java)
        startActivity(intent)
    }

    inline fun <reified T : BaseActivity> Context.startNew() {
        val intent = Intent(this, T::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }


}