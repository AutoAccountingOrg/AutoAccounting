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

package net.ankio.auto.hooks.alipay.hooks

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.HookMainApp
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.Builder
import net.ankio.auto.utils.DpUtils.dip2px
import net.ankio.auto.utils.StyleUtils
import net.ankio.auto.utils.StyleUtils.apply
import net.ankio.auto.utils.ViewUtils


class SettingUIHooker(hooker: Hooker) :PartHooker(hooker) {
    override val hookName: String
        get() = "支付宝设置页面"
    override fun onInit(classLoader: ClassLoader?, context: Context?) {

        XposedHelpers.findAndHookMethod("com.alipay.mobile.framework.app.ui.BaseActivity",classLoader,"setContentView",View::class.java,object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                logD("支付宝设置页面Hook成功，添加自动记账设置项目中....")
                val activity = param.thisObject as Activity
                val view = param.args[0] as View
                createListView(activity,view)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun createListView(activity: Activity, view: View) {
        val lineTopView = View(activity)
        lineTopView.setBackgroundColor(-0x111112)
        val itemHlinearLayout = LinearLayout(activity)
        itemHlinearLayout.orientation = LinearLayout.HORIZONTAL
        itemHlinearLayout.weightSum = 1f
        itemHlinearLayout.background =
            Builder().defaultColor(Color.WHITE).pressedColor(-0x262627).create()
        itemHlinearLayout.gravity = Gravity.CENTER_VERTICAL
        itemHlinearLayout.isClickable = true
        itemHlinearLayout.setOnClickListener {
            ActiveUtils.startApp(activity)
        }
        val itemNameText = TextView(activity)
        apply(itemNameText)
        itemNameText.text = "\uD83D\uDCB0  "+HookMainApp.name
        itemNameText.gravity = Gravity.CENTER_VERTICAL
        itemNameText.setPadding(dip2px(activity, 12), 0, 0, 0)
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG)
        val itemSummerText = TextView(activity)
        apply(itemSummerText)
        itemSummerText.text = HookMainApp.versionName
        itemSummerText.gravity = Gravity.CENTER_VERTICAL
        itemSummerText.setPadding(0, 0, dip2px(activity, 18), 0)
        itemSummerText.setTextColor(-0x666667)

        //try use Alipay style
        try {
            val settingsView = ViewUtils.findViewByName(activity, "com.alipay.mobile.antui", "item_left_text")
            if (settingsView is TextView) {
                val scale = itemNameText.textSize / settingsView.textSize
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingsView.textSize)
                itemSummerText.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    itemSummerText.textSize / scale
                )
                itemNameText.setTextColor(settingsView.currentTextColor)
            }
        } catch (_: Exception) {

        }

        itemHlinearLayout.addView(
            itemNameText,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )
        itemHlinearLayout.addView(
            itemSummerText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        val lineBottomView = View(activity)
        lineBottomView.setBackgroundColor(-0x111112)
        val rootLinearLayout = LinearLayout(activity)
        val paddingSide = 25
        val paddingTop = 5
        // 参数是：左侧，顶部，右侧和底部的 padding，单位是像素
        rootLinearLayout.setPadding(paddingSide, paddingTop, paddingSide, paddingTop)
        rootLinearLayout.orientation = LinearLayout.VERTICAL
        rootLinearLayout.addView(
            lineTopView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        )
        val lineParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        lineTopView.visibility = View.INVISIBLE
        itemHlinearLayout.background = Builder().defaultColor(Color.WHITE)
            .pressedColor(-0x141415).round(32).create()
        lineBottomView.visibility = View.INVISIBLE
        lineParams.bottomMargin = dip2px(activity, 8)
        rootLinearLayout.addView(
            itemHlinearLayout,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dip2px(activity, 50))
        )
        rootLinearLayout.addView(lineBottomView, lineParams)


        val firstLinearLayout = findFirstLinearLayout(view as ViewGroup)

        firstLinearLayout?.addView(rootLinearLayout)
    }

    private fun findFirstLinearLayout(viewGroup: ViewGroup): LinearLayout? {
        for (i in 0 until viewGroup.childCount) {
            val view = viewGroup.getChildAt(i)
            if (view is LinearLayout) {
                return view // 找到第一个 LinearLayout 并返回
            } else if (view is ViewGroup) {
                val result = findFirstLinearLayout(view)
                if (result != null) return result
            }
        }
        return null // 如果没有找到 LinearLayout，则返回 null
    }
}