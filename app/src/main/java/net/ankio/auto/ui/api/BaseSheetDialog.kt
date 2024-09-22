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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.Setting

abstract class BaseSheetDialog(private val context: Context) :
    BottomSheetDialog(context, R.style.BottomSheetDialog) {
    lateinit var cardView: MaterialCardView
    lateinit var cardViewInner: ViewGroup

    abstract fun onCreateView(inflater: LayoutInflater): View

    open fun show(
        float: Boolean = false,
        cancel: Boolean = false,
    ) {
        val inflater = LayoutInflater.from(context)
        val root = this.onCreateView(inflater)
        this.setContentView(root)
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
        val bottomSheet: View = root.parent as View

        val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)

        // 禁用向下滑动关闭行为
        bottomSheetBehavior.isDraggable = false
        // 设置BottomSheetDialog展开到全屏高度
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        // 是否使用圆角风格
        val margin = App.dp2px( 20f)
        val round = ConfigUtils.getBoolean(Setting.USE_ROUND_STYLE, false)
        if (::cardView.isInitialized) {
            val layoutParams =
                if (cardView.layoutParams != null) {
                    cardView.layoutParams as ViewGroup.MarginLayoutParams
                } else {
                    // 如果 RadiusCardView 还没有布局参数，则创建新的参数
                    ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                }

            if (round) {
                layoutParams.setMargins(margin, margin, margin, margin)
                cardView.layoutParams = layoutParams
                // 使用圆角风格
            } else {
                layoutParams.setMargins(0, 0, 0, -margin)
                cardView.layoutParams = layoutParams
            }

            val color = SurfaceColors.SURFACE_3.getColor(context)
            cardView.setCardBackgroundColor(color)
        }

        if (::cardViewInner.isInitialized) {
            cardViewInner.setPadding(
                margin,
                margin,
                margin,
                if (round) margin else margin * 2,
            )
        }

        show()
    }

    override fun dismiss() {
        runCatching { super.dismiss() }.onFailure {
            it.printStackTrace()
            Logger.e("Dismiss error", it)
        }
    }

}