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

package net.ankio.auto.ui.componets

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.size
import net.ankio.auto.R
import net.ankio.auto.utils.ThemeUtils

/**
 * 流动布局元素
 */
class FlowElement(
    private val context: Context,
    private val flowLayoutManager: FlowLayoutManager,
    prev: FlowElement? = null,

) {
    private var elements = ArrayList<View>()
    var data = HashMap<String, Any>()
    var type = 0 // 0 button 1  text 2 wave
    private var index = 0
    private var text = ""
    var connector = false
    var waveCallback: ((FlowElement, WaveTextView) -> Unit)? = null

    var firstWaveTextView = false

    private lateinit var connectorView: TextView

    init {
        index = prev?.getViewStart() ?: (flowLayoutManager.size - 1)
        this.removed()
    }

    fun removed(): FlowElement {
        index =  getViewStart()
        elements.forEach {
            flowLayoutManager.removeView(it)
        }
        if (::connectorView.isInitialized) {
            flowLayoutManager.removeView(connectorView)
        }
        elements.clear()
        return this
    }

    fun getViewStart(): Int {
        if (elements.size <= 0) return if(index<0)0 else index
        return flowLayoutManager.indexOfChild(elements.first())
    }

    fun getViewEnd(): Int {
        if (elements.size <= 0) return 0
        return flowLayoutManager.indexOfChild(elements.last())
    }

    /**
     * 返回的是结束值
     */
    fun setAsWaveTextview(
        text: String,
        connector: Boolean = false,
        callback: ((FlowElement, WaveTextView) -> Unit)?
    ): Int {
        this.text = text
        type = 2
        if (waveCallback != callback) {
            waveCallback = callback
        }
        if(flowLayoutManager.findFirstWave()!=null){
            this.connector = connector
            if (connector) {
                setAsButton(context.getString(R.string.and)) { _, view ->
                    view.text =
                        if (view.text == context.getString(R.string.or)) context.getString(R.string.and) else context.getString(
                            R.string.or
                        )
                }
            }
        }else{
            firstWaveTextView = true
            index= flowLayoutManager.firstWaveTextViewPosition
        }


        text.trim().split("").forEach {
            if (it.isEmpty()) {
                return@forEach
            }
            val waveTextView = WaveTextView(context)
            waveTextView.text = it
            waveTextView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge)
            waveTextView.setPadding(0, 0, 0, 0)
            val color = ThemeUtils.getThemeAttrColor(
                context,
                com.google.android.material.R.attr.colorPrimary
            )
            waveTextView.setTextColor(color)
            waveTextView.isClickable = true
            waveTextView.isFocusable = true
            waveTextView.background = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ripple_effect,
                context.theme
            )
            waveTextView.setOnClickListener {
                callback?.let { it1 -> it1(this, waveTextView) }
            }
            waveTextView.setOnLongClickListener { it2 ->
                if(firstWaveTextView){
                    flowLayoutManager.removedElement(this)
                    flowLayoutManager.findAdd()?.callOnClick()
                }else if (connector) {
                    flowLayoutManager.removedElement(this)
                }
                true
            }
            elements.add(waveTextView)
            flowLayoutManager.addView(waveTextView, index++)
        }


        return index
    }


    fun setAsTextView(text: String) {
        type = 1
        this.text = text
        text.trim().split("").forEach {
            val textView = TextView(context)
            textView.text = it
            textView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge)
            textView.setPadding(0, 0, 0, 0)
            elements.add(textView)
            flowLayoutManager.addView(textView, index++)
        }

    }

    fun setAsButton(text: String, callback: (FlowElement, TextView) -> Unit) {
        this.text = text
        type = 0
        val textView = TextView(context)
        textView.text = text
        textView.gravity = Gravity.CENTER
        textView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge)
        textView.setTextColor(
            ThemeUtils.getThemeAttrColor(
                context,
                com.google.android.material.R.attr.colorOnSecondaryFixed
            )
        )
        textView.isClickable = true
        textView.isFocusable = true
        textView.width = 120
        textView.background =
            ResourcesCompat.getDrawable(context.resources, R.drawable.ripple_effect, context.theme)
        textView.setBackgroundResource(R.drawable.rounded_border3)
        textView.setPadding(10, 0, 10, 5)
        textView.setOnClickListener {
            callback(this, textView)
        }
        elements.add(textView)
        flowLayoutManager.addView(textView, index++)
    }

    fun getElementText(): String {
        return text
    }

    fun getFirstView(): View? {
        return elements.firstOrNull()
    }


}