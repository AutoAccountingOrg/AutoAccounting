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
    override fun onInit(classLoader: ClassLoader?, context: Context?) {
        XposedHelpers.findAndHookMethod("com.alipay.android.phone.home.setting.MySettingActivity_",classLoader,"setContentView",Int::class.java,object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val activity = param.thisObject as Activity
                createListView(activity)
            }
        })
    }

    private fun createListView(activity: Activity) {
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
        itemNameText.text = HookMainApp.name
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
        val listViewId = activity.resources.getIdentifier(
            "setting_list",
            "id",
            "com.alipay.android.phone.openplatform"
        )
        val listView: ListView = activity.findViewById(listViewId)
        listView.addHeaderView(rootLinearLayout)
    }

}