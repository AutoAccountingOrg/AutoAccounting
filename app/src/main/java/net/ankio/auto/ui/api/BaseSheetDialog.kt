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

package net.ankio.auto.ui.api

import android.content.Context
import android.content.ContextWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.DisplayUtils
import org.ezbook.server.constant.Setting

abstract class BaseSheetDialog(private val context: Context) :
    BottomSheetDialog(context, R.style.BottomSheetDialog) {
    var lifecycleOwner: LifecycleOwner? = null
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            dismiss()
        }
    }

    // 添加绑定生命周期的方法
     fun bindToLifecycle(owner: LifecycleOwner) {
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = owner
        owner.lifecycle.addObserver(lifecycleObserver)
    }


    abstract fun onCreateView(inflater: LayoutInflater): View

    private fun prepareBaseView(): MaterialCardView {
        val maxWidthPx =  if (DisplayUtils.isTabletOrFoldable(context)) {
            // 平板或折叠屏模式：固定500px宽度
            App.dp2px(400f)
        } else {
            // 手机模式：占满屏幕宽度
            DisplayUtils.getRealScreenSize(context).x
        }
        // 创建cardView
        val round = ConfigUtils.getBoolean(Setting.USE_ROUND_STYLE, false)
        val margin = App.dp2px(20f)
        val cardView = MaterialCardView(context).apply {
       //     id = R.id.cardView // 这里需要提供一个有效的 ID

            layoutParams = if (round) {
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(margin, margin, margin, margin + App.navigationBarHeight)
                    width = maxWidthPx
                }
            } else {

                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    width = maxWidthPx
                }

            }

            cardElevation = 0f
            strokeColor = App.getThemeAttrColor(com.google.android.material.R.attr.colorSurfaceContainerHighest)
            strokeWidth = 0
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
            radius = 0f
        }

        val backgroundView = LinearLayout(context).apply {
           // id = R.id.backgroundView // 设置 ID
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setPadding(0,0,0,0) // 设置内边距

            val drawable = if (round) {
                ContextCompat.getDrawable(context, R.drawable.rounded_all)?.mutate() // 使用 mutate() 创建一个可变的 Drawable
            } else {
                ContextCompat.getDrawable(context, R.drawable.rounded_top)?.mutate()
            }
            val color = SurfaceColors.SURFACE_3.getColor(context)
            drawable?.setTint(color)

            // 设置 Drawable 背景
            background = drawable

            if (!round) {
                updatePadding(
                    bottom =  App.navigationBarHeight,
                )
            }
        }

        val innerView = LinearLayout(context).apply {
            id = R.id.innerView // 设置 ID
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setPadding(resources.getDimensionPixelSize(R.dimen.cardPadding),
                resources.getDimensionPixelSize(R.dimen.cardPadding),
                resources.getDimensionPixelSize(R.dimen.cardPadding),
                resources.getDimensionPixelSize(R.dimen.cardPadding)) // 设置内边距
        }
        backgroundView.addView(innerView)
        cardView.addView(backgroundView)
        // 设置子元素水平居中
        return cardView
    }
    open fun onViewCreated(view: View) {
        // 空实现
    }

    open fun show(
        float: Boolean = false,
        cancel: Boolean = false,
    ) {
        // 自动检测和绑定生命周期
        when (context) {
            is LifecycleOwner -> bindToLifecycle(context)
            is ContextWrapper -> {
                val baseContext = context.baseContext
                if (baseContext is LifecycleOwner) {
                    bindToLifecycle(baseContext)
                }
            }
        }

        val cardView = prepareBaseView()
        val inflater = LayoutInflater.from(context)
        val root = this.onCreateView(inflater)
        cardView.findViewById<LinearLayout>(R.id.innerView).addView(root)
        setContentView(cardView)
        onViewCreated(root)
        val layoutParams = cardView.layoutParams as FrameLayout.LayoutParams
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        cardView.layoutParams = layoutParams
        // 想上找，找到最顶层的LinearLayout
       /* val rootView = (cardView.rootView as ViewGroup).getChildAt(0)
        Logger.d("rootView: $rootView")
        // 设置margin = 0
        val layoutParamsRoot = rootView.layoutParams as  FrameLayout.LayoutParams
        layoutParamsRoot.bottomMargin = 0
        rootView.layoutParams = layoutParamsRoot*/
        this.setCancelable(cancel)
        if (float) {
            window?.let {
                // 获取当前的窗口参数
                val params = it.attributes

                // 设置为悬浮窗口类型
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

                // 添加 FLAG_NOT_FOCUSABLE 标志
              //  params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

                // 应用更新的窗口参数
                it.attributes = params
            }
        }


        // 动态调整边距
        //val margin = App.dp2px(20f)
     //   val round = ConfigUtils.getBoolean(Setting.USE_ROUND_STYLE, false)
       // val cardView = ViewUtils.findView(root,0,5,MaterialCardView::class.java)?: return
    //    val cardViewInner = if (cardView.childCount > 0 )  cardView.getChildAt(0) else return
     //   val layoutParams = cardView.layoutParams as LinearLayout.LayoutParams
    //    if (round) {
    //        layoutParams.setMargins(margin, margin, margin, margin)
    //    } else {
    //        layoutParams.setMargins(0, 0, 0, -App.navigationBarHeight)
     //   }
   //     layoutParams.width = maxWidth
   //     cardView.layoutParams = layoutParams
    //    val color = SurfaceColors.SURFACE_3.getColor(context)
   //     cardView.setCardBackgroundColor(color)

    /*    cardViewInner.setPadding(
            margin,
            margin,
            margin,
            if (round) margin else margin + App.navigationBarHeight,
        )*/
     //   val parent = cardView.parent as LinearLayout
        // 设置子元素水平居中
   //   parent.gravity = Gravity.CENTER_HORIZONTAL

       // parent.setHorizontalGravity(LinearLayout.HORIZONTAL)


        val bottomSheet: View = cardView.parent as View
        // 设置子元素水平居中

      //  val lay = bottomSheet.layoutParams as ViewGroup.MarginLayoutParams
    //    lay.bottomMargin = - App.navigationBarHeight
     //   bottomSheet.layoutParams = lay

        val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)

        // 禁用向下滑动关闭行为
        bottomSheetBehavior.isDraggable = false
        // 设置BottomSheetDialog展开到全屏高度
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        super.show()
    }

    override fun dismiss() {
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
        runCatching {
            if (this.isShowing && window?.decorView?.isAttachedToWindow == true) {
                super.dismiss()
            }
        }.onFailure {
            it.printStackTrace()
            Logger.e("Dismiss error", it)
        }
    }

    //重写默认的show方法
    override fun show() {
        show(float = false,cancel = true)
    }

    // 添加Fragment专用的show方法
    fun showInFragment(fragment: Fragment, float: Boolean = false, cancel: Boolean = false) {
        bindToLifecycle(fragment.viewLifecycleOwner)
        show(float, cancel)
    }

}