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

package net.ankio.auto.ui.componets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import net.ankio.auto.R
import net.ankio.auto.databinding.StatusPageBinding


class StatusPage : RelativeLayout {

    private var loadingView: ProgressBar? = null
    private var emptyView: LinearLayout? = null
    private var errorView: LinearLayout? = null
    var contentView: RecyclerView? = null


    constructor(context: Context) : super(context) {
       init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
       init(context, attrs)
    }

    fun init( context: Context,
              attrs: AttributeSet?){
        val binding = StatusPageBinding.inflate(LayoutInflater.from(context), this, true)
        loadingView = binding.loadingView
        emptyView = binding.emptyView
        errorView = binding.errorView
        contentView = binding.contentView
        val  root = binding.rootView
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StatusPage,
            0,
            0,
        ).apply {
            try {
                val height = getString(R.styleable.StatusPage_innerHeight)
                val layoutParams = root.layoutParams
                layoutParams.height = if (height.equals("wrap_content")) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
                root.layoutParams = layoutParams
            } finally {
                recycle()
            }

        }


    }




    fun showLoading() {
        setVisibility(VISIBLE, GONE, GONE, GONE)
    }

    fun showEmpty() {
        setVisibility(GONE, VISIBLE, GONE, GONE)
    }

    fun showError() {
        setVisibility(GONE, GONE, VISIBLE, GONE)
    }

    fun showContent() {
        setVisibility(GONE, GONE, GONE, VISIBLE)
    }

    private fun setVisibility(loading: Int, empty: Int, error: Int, content: Int) {
        loadingView!!.visibility = loading
        emptyView!!.visibility = empty
        errorView!!.visibility = error
        contentView!!.visibility = content
    }
}