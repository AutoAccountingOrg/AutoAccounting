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

import android.app.Activity
import android.app.Service
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
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.DisplayUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.toThemeColor
import java.lang.reflect.ParameterizedType

/**
 * 基础底部弹窗对话框
 *
 * 这是一个自定义的底部弹窗基类，提供了以下功能：
 * - 自动生命周期管理：自动监听Activity/Fragment生命周期，在销毁时自动关闭弹窗
 * - 支持圆角和直角样式：根据用户偏好设置显示不同样式的弹窗
 * - 适配平板和折叠屏：自动处理不同屏幕尺寸和导航栏高度
 * - 键盘弹出处理：监听IME状态变化，自动调整弹窗高度
 * - 悬浮窗支持：支持在Service中显示悬浮窗形式的弹窗
 * - 安全的内存管理：防止内存泄漏和崩溃
 *
 * 使用方式：
 * 1. 继承此类并实现 onCreateView() 方法
 * 2. 可选择重写 onViewCreated() 进行初始化
 * 3. 调用 show() 方法显示弹窗
 */
abstract class BaseSheetDialog<VB : ViewBinding> : BottomSheetDialog {
    /**
     * ViewBinding 实例，在 Fragment 销毁时会被置空以防止内存泄漏
     */
    private var _binding: VB? = null

    /**
     * 对外暴露的 ViewBinding 属性，提供非空访问
     * 在 Fragment 生命周期内可以安全使用
     */
    protected val binding get() = _binding!!

    /** 生命周期所有者，用于自动管理弹窗生命周期 */
    private val lifecycleOwner: LifecycleOwner?

    /** 是否为悬浮窗模式，Service环境下必须为true */
    private val isOverlay: Boolean

    /**
     * 使用Activity构造弹窗
     *
     * @param activity 宿主Activity，将自动监听其生命周期
     */
    constructor(activity: Activity) : super(activity, R.style.BottomSheetDialog) {
        lifecycleOwner = if (activity is LifecycleOwner) activity else null
        isOverlay = false
        lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
    }

    /**
     * 使用Fragment构造弹窗
     *
     * @param fragment 宿主Fragment，将监听其viewLifecycleOwner的生命周期
     */
    constructor(fragment: Fragment) : super(fragment.requireContext(), R.style.BottomSheetDialog) {
        lifecycleOwner = fragment.viewLifecycleOwner
        isOverlay = false
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * 使用Service构造弹窗（悬浮窗模式）
     *
     * @param service 宿主Service，弹窗将以悬浮窗形式显示
     */
    constructor(service: Service) : super(service, R.style.BottomSheetDialog) {
        lifecycleOwner = null // Service无生命周期监听
        isOverlay = true      // 必须悬浮窗
    }

    /**
     * 生命周期观察者，监听宿主生命周期变化
     * 当宿主销毁时自动关闭弹窗，防止内存泄漏
     */
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) dismiss()
    }

    /**
     * 子类必须实现: 用于生成内容View
     *
     * 这个方法在弹窗显示时被调用，子类需要返回要显示的内容视图。
     * 返回的View将被添加到弹窗的内容容器中。
     *
     * @param inflater 布局填充器，用于inflate布局文件
     * @return 返回要显示的内容视图
     */
    fun onCreateView(inflater: LayoutInflater): View? {
        try {

            // 通过反射获取泛型参数中的 ViewBinding 类型
            val type = javaClass.genericSuperclass as ParameterizedType
            val bindingClass = type.actualTypeArguments.firstOrNull {
                it is Class<*> && ViewBinding::class.java.isAssignableFrom(it)
            } as? Class<VB>
                ?: throw IllegalStateException("Cannot infer ViewBinding type for ${javaClass.name}")


            // 获取 ViewBinding 的 inflate 方法
            val method = bindingClass.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
            )

            // 调用 inflate 方法创建绑定实例
            @Suppress("UNCHECKED_CAST")
            _binding = method.invoke(null, inflater) as VB


            return binding.root
        } catch (e: Exception) {
            Logger.e("Failed to create view for ${javaClass.simpleName}", e)
            return null
        }
    }

    /**
     * 子类可重写：View创建后回调（必须调用super）
     *
     * 这个方法在内容View创建完成后被调用，子类可以在这里进行：
     * - 视图初始化
     * - 数据绑定
     * - 事件监听器设置
     * - 其他初始化操作
     *
     * @param view 创建的内容视图
     */
    open fun onViewCreated(view: View?) {}

    /**
     * 子类可重写：键盘弹出时的事件
     *
     * 当软键盘弹出时，这个方法会被调用。
     * 子类可以在这里处理键盘弹出时的特殊逻辑，比如：
     * - 滚动到特定位置
     * - 调整UI布局
     * - 隐藏某些元素
     */
    open fun onImeVisible() {}

    /**
     * 创建外层CardView
     *
     * 根据样式设置创建外层的MaterialCardView，负责：
     * - 设置边距和圆角
     * - 配置背景和边框
     * - 处理导航栏高度适配
     *
     * @param round 是否使用圆角样式，true为圆角，false为直角
     * @param margin 边距大小（像素）
     * @return 配置好的MaterialCardView
     */
    private fun createCardView(round: Boolean, margin: Int): MaterialCardView {
        return MaterialCardView(context).apply {
            // 设置布局参数
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                if (round) {
                    // 圆角样式：设置四周边距，底部额外加上导航栏高度
                    setMargins(
                        margin, margin, margin, margin + DisplayUtils.getNavigationBarHeight(
                            context
                        )
                    )
                } else {
                    // 直角样式：无边距
                    setMargins(0, 0, 0, 0)
                }
            }

            // 配置卡片样式
            cardElevation = 0f  // 无阴影
            strokeColor =
                com.google.android.material.R.attr.colorSurfaceContainerHighest.toThemeColor()  // 边框颜色
            strokeWidth = 0     // 无边框
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.transparent))  // 透明背景
            radius = if (round) DisplayUtils.dp2px(16f).toFloat() else 0f  // 圆角半径
        }
    }

    /**
     * 创建背景View
     *
     * 创建背景容器LinearLayout，负责：
     * - 设置背景drawable（圆角或直角）
     * - 配置主题颜色
     * - 处理导航栏适配
     *
     * @param round 是否使用圆角样式
     * @return 配置好的背景LinearLayout
     */
    private fun createBackgroundView(round: Boolean): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)

            // 根据样式选择背景drawable
            val drawableRes = if (round) R.drawable.rounded_all else R.drawable.rounded_top
            val drawable = ContextCompat.getDrawable(context, drawableRes)?.mutate()

            // 设置主题颜色
            drawable?.setTint(SurfaceColors.SURFACE_3.getColor(context))
            background = drawable

            // 直角样式时底部添加导航栏高度
            if (!round) updatePadding(
                bottom = DisplayUtils.getNavigationBarHeight(
                    context
                )
            )
        }
    }

    /**
     * 创建内容承载的LinearLayout
     *
     * 创建内容容器，负责：
     * - 承载子类返回的内容View
     * - 设置内边距
     * - 提供垂直布局
     *
     * @return 配置好的内容容器LinearLayout
     */
    private fun createInnerView(): LinearLayout {
        return LinearLayout(context).apply {
            id = R.id.innerView  // 设置ID便于查找
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL

            // 设置内边距
            val padding = resources.getDimensionPixelSize(R.dimen.cardPadding)
            setPadding(padding, padding, padding, padding)
        }
    }

    /**
     * 组装完整BaseView
     *
     * 将各个组件组装成完整的弹窗视图结构：
     * MaterialCardView (外层容器)
     * └── LinearLayout (背景容器)
     *     └── LinearLayout (内容容器)
     *         └── 子类内容View
     *
     * @return 组装好的根MaterialCardView
     */
    private fun prepareBaseView(): MaterialCardView {
        // 获取用户偏好设置
        val round = PrefManager.uiRoundStyle
        val margin = DisplayUtils.dp2px(20f)

        Logger.d("Preparing base view - round: $round")

        // 创建各个组件
        val cardView = createCardView(round, margin)
        val backgroundView = createBackgroundView(round)
        val innerView = createInnerView()

        // 组装视图层次
        backgroundView.addView(innerView)
        cardView.addView(backgroundView)
        return cardView
    }

    /**
     * 配置BottomSheet弹窗行为
     *
     * 配置BottomSheetBehavior的各种属性：
     * - 禁用拖拽
     * - 跳过折叠状态
     * - 设置最大高度
     * - 监听键盘状态变化
     *
     * @param cardView 要配置的卡片视图
     */
    private fun setupBottomSheet(cardView: MaterialCardView) {
        // 获取BottomSheet容器
        val bottomSheet = cardView.parent as? View ?: return
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // 配置行为属性
        bottomSheetBehavior.apply {
            isDraggable = false        // 禁用拖拽
            skipCollapsed = true       // 跳过折叠状态
            state = BottomSheetBehavior.STATE_EXPANDED  // 默认展开
            // 设置最大高度为屏幕高度减去状态栏高度
            maxHeight =
                DisplayUtils.getRealScreenSize(context).y - DisplayUtils.getStatusBarHeight(context)
        }

        Logger.d("BottomSheet configured - maxHeight: ${bottomSheetBehavior.maxHeight}")

        // 监听窗口插入变化（主要是键盘）
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            // 键盘弹出时调整底部边距
            bottomSheet.updatePadding(
                bottom = if (imeVisible) imeHeight - DisplayUtils.getNavigationBarHeight(
                    context
                ) else 0
            )

            // 通知子类键盘状态变化
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
     * 显示弹窗的主要方法，包含完整的安全检查和配置：
     * - 生命周期状态检查
     * - Activity状态检查
     * - 悬浮窗权限检查
     * - 视图创建和配置
     * - 窗口属性设置
     *
     * @param float 是否以悬浮窗形式显示，Service环境下必须为true
     * @param cancel 是否可取消，true表示点击外部可关闭弹窗
     */
    open fun show(
        float: Boolean = false,
        cancel: Boolean = false,
    ) {
        // 生命周期安全检查：只有生命周期Owner时检查
        lifecycleOwner?.let {
            if (it.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                Logger.w("Cannot show dialog: lifecycle is DESTROYED")
                return
            }
        }

        // Activity状态安全检查：如果 context 是 Activity 需检查存活
        (context as? Activity)?.let {
            if (it.isFinishing || it.isDestroyed) {
                Logger.w("Cannot show dialog: activity not running")
                return
            }
        }

        // 悬浮窗权限检查：Service 下只能用悬浮窗
        if (isOverlay && !float) {
            Logger.e("Service context must use overlay window")
            return
        }
        
        Logger.d("Showing dialog - float: $float, cancel: $cancel")

        // 创建和配置视图
        val cardView = prepareBaseView()
        val root = onCreateView(LayoutInflater.from(context))
        cardView.findViewById<LinearLayout>(R.id.innerView).addView(root)
        setContentView(cardView)

        // 回调子类
        onViewCreated(root)

        // 设置布局参数
        cardView.layoutParams = (cardView.layoutParams as FrameLayout.LayoutParams).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 设置可取消性
        setCancelable(cancel)

        // 配置窗口属性
        window?.let { win ->
            runCatching {
                val params = win.attributes
                if (float) {
                    // 设置为悬浮窗类型
                    params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    Logger.d("Set as overlay window")
                }
                win.attributes = params
            }.onFailure {
                Logger.e("Failed to set window attributes", it)
            }
        }

        // 配置BottomSheet行为
        setupBottomSheet(cardView)

        // 显示弹窗
        super.show()
    }

    /**
     * 兼容旧版：无参show即普通弹窗
     *
     * 为了向后兼容，提供无参数的show方法。
     * 默认以非悬浮窗形式显示，且可取消。
     */
    override fun show() {
        show(float = false, cancel = true)
    }

    /**
     * 关闭弹窗，自动解绑生命周期
     *
     * 安全地关闭弹窗，包含：
     * - 移除生命周期观察者
     * - 检查弹窗状态
     * - 异常处理
     */
    override fun dismiss() {

        
        runCatching {
            _binding = null
            // 移除生命周期观察者，防止内存泄漏
            lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
            // 检查对话框是否正在显示且窗口有效
            if (isShowing && window?.decorView?.isAttachedToWindow == true) {
                super.dismiss()
            } else {
                Logger.d("Dialog is not showing or window is not attached, skipping dismiss")
            }
        }.onFailure {
            it.printStackTrace()
            Logger.e("Dismiss error", it)
        }
    }


}
