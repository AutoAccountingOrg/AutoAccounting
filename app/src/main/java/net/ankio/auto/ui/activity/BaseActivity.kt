package net.ankio.auto.ui.activity


import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.elevation.SurfaceColors
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import com.zackratos.ultimatebarx.ultimatebarx.addStatusBarTopPadding
import com.zackratos.ultimatebarx.ultimatebarx.navigationBar
import com.zackratos.ultimatebarx.ultimatebarx.statusBar

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
        super.attachBaseContext(newBase)
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
            val theme = ThemeEngine.getInstance(this@BaseActivity).getTheme()
            val mStatusBarColor = getThemeAttrColor(this,theme,android.R.attr.colorBackground)
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
        return MaterialColors.getColor(ContextThemeWrapper(this, ThemeEngine.getInstance(this@BaseActivity).getTheme()), attrResId, Color.WHITE)
    }

    /**
     * 获取主题色
     */
    @ColorInt
    fun getThemeAttrColor(context: Context, @StyleRes themeResId: Int, @AttrRes attrResId: Int): Int {
        return MaterialColors.getColor(ContextThemeWrapper(context, themeResId), attrResId, Color.WHITE)
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


}