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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import net.ankio.auto.autoApp
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.DisplayUtils
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.constant.Setting

/**
 * 基础底部弹窗对话框
 *
 * 这是一个自定义的底部弹窗基类，提供了以下功能：
 * - 自动生命周期管理
 * - 支持圆角和直角样式
 * - 适配平板和折叠屏
 * - 键盘弹出处理
 * - 悬浮窗支持
 *
 * @param lifecycleOwner 生命周期
 */
abstract class BaseSheetDialog(protected val lifecycleOwner: LifecycleOwner) :
    BottomSheetDialog(autoApp, R.style.BottomSheetDialog) {

    /** 生命周期观察者，当Fragment/Activity销毁时自动关闭对话框 */
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            Logger.d("Lifecycle destroyed, dismissing dialog")
            dismiss()
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }


    /**
     * 子类必须实现: 用于生成内容View
     *
     * @param inflater 布局填充器
     * @return 返回要显示的内容视图
     */
    abstract fun onCreateView(inflater: LayoutInflater): View

    /**
     * 子类可重写：View创建后回调（必须调用super）
     *
     * @param view 创建的内容视图
     */
    open fun onViewCreated(view: View) {}

    /**
     * 子类可重写：键盘弹出时的事件
     */
    open fun onImeVisible() {}

    /**
     * 创建外层CardView
     *
     * @param round 是否使用圆角样式
     * @param maxWidthPx 最大宽度（像素）
     * @param margin 边距
     * @return 配置好的MaterialCardView
     */
    private fun createCardView(round: Boolean, maxWidthPx: Int, margin: Int): MaterialCardView {
        return MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(margin, margin, margin, margin + App.navigationBarHeight)
                if (DisplayUtils.isTabletOrFoldable(context)) width = maxWidthPx
            }
            cardElevation = 0f
            strokeColor =
                App.getThemeAttrColor(com.google.android.material.R.attr.colorSurfaceContainerHighest)
            strokeWidth = 0
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
            radius = if (round) App.dp2px(16f).toFloat() else 0f
        }
    }

    /**
     * 创建背景View
     *
     * @param round 是否使用圆角样式
     * @return 配置好的背景LinearLayout
     */
    private fun createBackgroundView(round: Boolean): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            val drawableRes = if (round) R.drawable.rounded_all else R.drawable.rounded_top
            val drawable = ContextCompat.getDrawable(context, drawableRes)?.mutate()
            drawable?.setTint(SurfaceColors.SURFACE_3.getColor(context))
            background = drawable
            if (!round) updatePadding(bottom = App.navigationBarHeight)
        }
    }

    /**
     * 创建内容承载的LinearLayout
     *
     * @return 配置好的内容容器LinearLayout
     */
    private fun createInnerView(): LinearLayout {
        return LinearLayout(context).apply {
            id = R.id.innerView
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.cardPadding)
            setPadding(padding, padding, padding, padding)
        }
    }

    /**
     * 组装完整BaseView
     *
     * @return 组装好的根MaterialCardView
     */
    private fun prepareBaseView(): MaterialCardView {
        val isTablet = DisplayUtils.isTabletOrFoldable(context)
        val round = ConfigUtils.getBoolean(Setting.USE_ROUND_STYLE, DefaultData.USE_ROUND_STYLE)
        val maxWidthPx =
            if (isTablet) App.dp2px(400f) else DisplayUtils.getRealScreenSize(context).x
        val margin = App.dp2px(20f)

        Logger.d("Preparing base view - isTablet: $isTablet, round: $round, maxWidth: $maxWidthPx")

        val cardView = createCardView(round, maxWidthPx, margin)
        val backgroundView = createBackgroundView(round)
        val innerView = createInnerView()
        backgroundView.addView(innerView)
        cardView.addView(backgroundView)
        return cardView
    }

    /**
     * 配置BottomSheet弹窗行为
     *
     * @param cardView 要配置的卡片视图
     */
    private fun setupBottomSheet(cardView: MaterialCardView) {
        val bottomSheet = cardView.parent as? View ?: return
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.apply {
            isDraggable = false
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
            maxHeight =
                DisplayUtils.getRealScreenSize(context).y - DisplayUtils.getStatusBarHeight(context)
        }

        Logger.d("BottomSheet configured - maxHeight: ${bottomSheetBehavior.maxHeight}")

        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            bottomSheet.updatePadding(
                bottom = if (imeVisible) imeHeight - DisplayUtils.getNavigationBarHeight(
                    context
                ) else 0
            )
            if (imeVisible) {
                Logger.d("IME visible, height: $imeHeight")
                onImeVisible()
            }
            insets
        }
    }

    /**
     * 主show方法
     *
     * @param float 是否以悬浮窗形式显示
     * @param cancel 是否可取消
     */
    open fun show(
        float: Boolean = false,
        cancel: Boolean = false,
    ) {
        Logger.d("Showing dialog - float: $float, cancel: $cancel")
        val cardView = prepareBaseView()
        val root = onCreateView(LayoutInflater.from(context))
        cardView.findViewById<LinearLayout>(R.id.innerView).addView(root)
        setContentView(cardView)
        onViewCreated(root)
        cardView.layoutParams = (cardView.layoutParams as FrameLayout.LayoutParams).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        setCancelable(cancel)
        window?.let { win ->
            runCatching {
                val params = win.attributes
                if (float) {
                    params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    Logger.d("Set as overlay window")
                }
                win.attributes = params
            }.onFailure {
                Logger.e("Failed to set window attributes", it)
            }
        }
        setupBottomSheet(cardView)
        super.show()
    }

    /**
     * 兼容旧版：无参show即普通弹窗
     */
    override fun show() {
        show(float = false, cancel = true)
    }

    /**
     * 关闭弹窗，自动解绑生命周期
     */
    override fun dismiss() {
        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        runCatching {
            if (isShowing && window?.decorView?.isAttachedToWindow == true) {
                super.dismiss()
            }
        }.onFailure {
            it.printStackTrace()
            Logger.e("Dismiss error", it)
        }
    }
}
