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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import net.ankio.auto.R
import net.ankio.auto.databinding.StatusPageBinding


class StatusPage : ConstraintLayout {

    private var loadingView: CircularProgressIndicator? = null
    private var emptyIcon: ImageView? = null
    private var emptyText: TextView? = null
    private var errorIcon: ImageView? = null
    private var errorText: TextView? = null
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

    fun init(
        context: Context,
        attrs: AttributeSet?
    ) {
        val binding = StatusPageBinding.inflate(LayoutInflater.from(context), this, true)
        loadingView = binding.loadingView
        emptyIcon = binding.emptyIcon
        emptyText = binding.emptyText
        errorIcon = binding.errorIcon
        errorText = binding.errorText
        contentView = binding.contentView
        val root = binding.rootView
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StatusPage,
            0,
            0,
        ).apply {
            try {
                val height = getString(R.styleable.StatusPage_innerHeight)
                val layoutParams = root.layoutParams
                layoutParams.height =
                    if (height.equals("wrap_content")) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
                root.layoutParams = layoutParams
            } finally {
                recycle()
            }

        }


    }


    fun showLoading() {
        setVisibility(loading = true)
    }

    fun showEmpty() {
        setVisibility(empty = true)
    }

    fun showError() {
        setVisibility(error = true)
    }

    fun showContent() {
        setVisibility(content = true)
    }

    private fun setVisibility(
        loading: Boolean = false,
        empty: Boolean = false,
        error: Boolean = false,
        content: Boolean = false
    ) {
        loadingView?.visibility = if (loading) View.VISIBLE else View.GONE

        emptyIcon?.visibility = if (empty) View.VISIBLE else View.GONE
        emptyText?.visibility = if (empty) View.VISIBLE else View.GONE

        errorIcon?.visibility = if (error) View.VISIBLE else View.GONE
        errorText?.visibility = if (error) View.VISIBLE else View.GONE

        contentView?.visibility = if (content) View.VISIBLE else View.GONE
    }
}