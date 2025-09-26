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
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.DisplayUtils
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.SystemUtils.findLifecycleOwner
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.utils.toThemeCtx
import java.lang.reflect.ParameterizedType
import kotlin.coroutines.cancellation.CancellationException

/**
 * 基础底部弹窗对话框
 *
 * 利用 BottomSheetDialog 自身的 LifecycleOwner 特性：
 * - 使用 Dialog 自身的生命周期，无需额外管理
 * - 自动推断悬浮窗模式：Service 环境自动启用
 * - 极简构造：只需 Context 一个参数
 * - 完整生命周期回调：实现 DefaultLifecycleObserver
 *
 * 使用方式：
 * ```kotlin
 * // 所有场景统一使用
 * MyDialog.create(context).show()
 * ```
 */
abstract class BaseSheetDialog<VB : ViewBinding> :
    BottomSheetDialog, DefaultLifecycleObserver {
    /**
     * ViewBinding 实例，在销毁时会被置空以防止内存泄漏
     */
    private var _binding: VB? = null

    /**
     * 对外暴露的 ViewBinding 属性，提供安全访问
     */
    protected val binding
        get() = _binding
            ?: throw IllegalStateException("ViewBinding 初始化失败，请检查布局文件和泛型参数")


    protected fun uiReady() =
        _binding != null && isShowing && window?.decorView?.isAttachedToWindow == true

    /** 宿主生命周期所有者，用于监听宿主销毁 */
    private val hostLifecycleOwner: LifecycleOwner

    /** 是否为悬浮窗模式，Service环境下必须为true */
    private val isOverlay: Boolean


    protected val ctx = context.toThemeCtx()

    /**
     * 弹窗消失回调（链式设置）。
     * 通过 [setOnDismiss] 进行设置；无论用户点击外部/按钮关闭，还是代码调用 dismiss()，系统都会触发。
     */
    private var onDismissCallback: (() -> Unit)? = null

    /**
     * 在Dialog生命周期内启动协程
     * 统一处理异常，业务代码无需再捕获异常
     *
     * @param block 协程代码块，专注于业务逻辑
     */
    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch(CoroutineExceptionHandler { _, _ -> }, block = block).apply {
            invokeOnCompletion { e ->
                when (e) {
                    null -> Unit // 正常完成不处理
                    is CancellationException -> {
                        Logger.d("Dialog协程已取消: ${e.message}")
                    }

                    else -> {
                        Logger.e("Dialog协程执行异常: ${javaClass.simpleName}", e)
                        // 可以在这里添加全局异常处理逻辑
                    }
                }
            }
        }
    }

    // ① 在 class 里加一个小工具函数（放在 init 外即可）
    private fun findViewBindingClass(start: Class<*>): Class<out ViewBinding>? {
        var current: Class<*>? = start
        while (current != null) {
            val superType = current.genericSuperclass
            if (superType is ParameterizedType) {
                @Suppress("UNCHECKED_CAST") val vb = superType.actualTypeArguments
                    .firstOrNull { it is Class<*> && ViewBinding::class.java.isAssignableFrom(it) }
                        as? Class<out ViewBinding>
                if (vb != null) return vb
            }
            current = current.superclass          // 继续向上
        }
        return null
    }


    init {
        try {

            // 通过反射获取泛型参数中的 ViewBinding 类型
            val bindingClass = findViewBindingClass(javaClass)
                ?: error("无法解析 ViewBinding 泛型，请确保每一层继承都带上具体类型")

            val inflate = bindingClass.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
            )
            @Suppress("UNCHECKED_CAST")
            _binding = inflate.invoke(null, LayoutInflater.from(ctx)) as VB

        } catch (e: Exception) {
            Logger.e("为 ${javaClass.simpleName} 创建视图失败", e)
            // 初始化失败时抛出异常，避免后续使用时崩溃
            throw IllegalStateException("ViewBinding 初始化失败: ${e.message}", e)
        }
    }

    /**
     * 简化构造函数 - 自动推断生命周期
     *
     * @param context 上下文，自动从中推断LifecycleOwner
     */
    constructor(context: Context) : super(context, R.style.BottomSheetDialog) {
        val owner = context.findLifecycleOwner()
        this.hostLifecycleOwner = owner
        this.isOverlay = owner is LifecycleService

        // 监听宿主生命周期，当宿主销毁时自动关闭弹窗
        hostLifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * 链式设置弹窗消失回调。
     * @param listener 回调函数（无参数）
     * @return 当前对话框实例，便于链式调用
     */
    fun setOnDismiss(listener: () -> Unit): BaseSheetDialog<VB> {
        onDismissCallback = listener
        super.setOnDismissListener { onDismissCallback?.invoke() }
        return this
    }

    /**
     * 宿主生命周期销毁事件处理
     * 当宿主（Activity/Fragment/Service）销毁时自动关闭弹窗
     */
    final override fun onDestroy(owner: LifecycleOwner) {
        hostLifecycleOwner.lifecycle.removeObserver(this)
        onDialogDestroy()
        dismiss()
    }

    /**
     * 子类可重写：弹窗销毁时的清理操作
     *
     * 在弹窗销毁时调用，子类可以在这里执行：
     * - 取消网络请求
     * - 释放资源
     * - 清理监听器
     *
     * 注意：Dialog 自身也是 LifecycleOwner，可以直接使用 this 作为生命周期：
     * ```kotlin
     * // 在子类中可以直接使用
     * viewModel.liveData.observe(this) { data ->
     *     // 更新UI，会自动跟随Dialog生命周期
     * }
     * ```
     */
    @CallSuper
    open fun onDialogDestroy() {
        // 子类可重写进行清理
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
        return MaterialCardView(ctx).apply {
            // 设置布局参数
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                if (round) {
                    // 圆角样式：设置四周边距，底部额外加上导航栏高度
                    setMargins(
                        margin, margin, margin, margin + DisplayUtils.getNavigationBarHeight(
                            ctx
                        )
                    )
                } else {
                    // 直角样式：无边距
                    setMargins(0, 0, 0, 0)
                }
            }

            // 配置卡片样式
            cardElevation = 0f  // 无阴影
            strokeColor = DynamicColors.SurfaceContainerHighest  // 边框颜色
            strokeWidth = 0     // 无边框
            setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.transparent))  // 透明背景
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
        return LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)

            // 根据样式选择背景drawable
            val drawableRes = if (round) R.drawable.rounded_all else R.drawable.rounded_top
            val drawable = ContextCompat.getDrawable(ctx, drawableRes)?.mutate()

            // 设置主题颜色
            drawable?.setTint(DynamicColors.SurfaceColor3)
            background = drawable

            // 直角样式时底部添加导航栏高度
            if (!round) updatePadding(
                bottom = DisplayUtils.getNavigationBarHeight(
                    ctx
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
        return LinearLayout(ctx).apply {
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

        Logger.d("准备基础视图 - 圆角样式: $round")

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
                DisplayUtils.getRealScreenSize(ctx).y - DisplayUtils.getStatusBarHeight(ctx)
        }

        Logger.d("BottomSheet 已配置 - 最大高度: ${bottomSheetBehavior.maxHeight}")

        // 监听窗口插入变化（主要是键盘）
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            // 键盘弹出时调整底部边距
            bottomSheet.updatePadding(
                bottom = if (imeVisible) imeHeight - DisplayUtils.getNavigationBarHeight(
                    ctx
                ) else 0
            )

            // 通知子类键盘状态变化
            if (imeVisible) {
                Logger.d("输入法可见，高度: $imeHeight")
                // 增加UI就绪校验，避免在对话框已销毁或未附加窗口时访问binding导致崩溃
                if (uiReady()) {
                    onImeVisible()
                } else {
                    Logger.d("IME可见但UI未就绪或已销毁，跳过 onImeVisible 回调")
                }
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
     * @param cancel 是否可取消，true表示点击外部可关闭弹窗
     */
    open fun show(cancel: Boolean = false) {
        // 宿主生命周期安全检查
        if (hostLifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            Logger.w("无法显示对话框：宿主生命周期已销毁")
            return
        }

        // Activity状态安全检查：如果 ctx 是 Activity 需检查存活
        (ctx as? Activity)?.let {
            if (it.isFinishing || it.isDestroyed) {
                Logger.w("无法显示对话框：Activity 未运行")
                return
            }
        }

        Logger.d("显示对话框 - 悬浮窗模式: $isOverlay, 可取消: $cancel")

        // 创建和配置视图
        val cardView = prepareBaseView()

        cardView.findViewById<LinearLayout>(R.id.innerView).addView(binding.root)
        setContentView(cardView)

        // 回调子类
        onViewCreated(binding.root)

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
                if (isOverlay) {
                    // 设置为悬浮窗类型
                    params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    Logger.d("设置为悬浮窗")
                }
                win.attributes = params
            }.onFailure {
                Logger.e("设置窗口属性失败", it)
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
     * 默认可取消。
     */
    override fun show() {
        show(cancel = true)
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
            // 检查对话框是否正在显示且窗口有效
            if (isShowing && window?.decorView?.isAttachedToWindow == true) {
                super.dismiss()
            } else {
                Logger.d("对话框未显示或窗口未附加，跳过关闭操作")
            }
        }.onFailure {
            it.printStackTrace()
            Logger.e("关闭对话框出错", it)
        }
    }

    companion object {
        /**
         * 创建弹窗 - 统一简化的工厂方法
         *
         * @param context 上下文，自动推断LifecycleOwner
         * @return 对话框实例，支持链式调用
         */
        inline fun <reified T : BaseSheetDialog<*>> create(
            context: Context
        ): T {
            val clazz = T::class.java
            val constructor = clazz.getDeclaredConstructor(
                Context::class.java
            )
            constructor.isAccessible = true
            return constructor.newInstance(context) as T
        }
    }

}
