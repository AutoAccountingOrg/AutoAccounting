/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.components

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView

/**
 * A vertical group of ExpandableCardView that:
 *  • animates layout changes (CHANGING)
 *  • applies a small gap and proper corner radii based on child position
 *  • supports collapseAll() and selectedIndex
 */
class ExpandableCardGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    // gap between cards in dp
    private val gapDp = 2

    // corner radius for first/last cards in dp
    private val cornerDp = 24f

    init {
        orientation = VERTICAL
        // enable smooth layout changes for expansions/collapses
        layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        applyGrouping()
    }


    private fun applyGrouping() {
        val density = resources.displayMetrics.density
        val gapPx = (gapDp * density).toInt()
        val cornerPx = cornerDp * density

        val visibleChildren = mutableListOf<View>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.VISIBLE) {
                visibleChildren.add(child)
            }
        }
        val visibleCount = visibleChildren.size

        for (i in 0 until visibleCount) {
            val child = visibleChildren[i]

            // 1) apply top-gap margin (except on the first card)
            (child.layoutParams as? MarginLayoutParams)?.let { lp ->
                lp.topMargin = if (i == 0) 0 else gapPx
                child.layoutParams = lp
            }

            // 2) adjust corner radii on MaterialCardView children
            if (child is MaterialCardView) {
                val builder = child.shapeAppearanceModel.toBuilder()

                // top corners
                if (i == 0) {
                    builder
                        .setTopLeftCornerSize(cornerPx)
                        .setTopRightCornerSize(cornerPx)
                } else {
                    builder
                        .setTopLeftCornerSize(0f)
                        .setTopRightCornerSize(0f)
                }

                // bottom corners
                if (i == visibleCount - 1) {
                    builder
                        .setBottomLeftCornerSize(cornerPx)
                        .setBottomRightCornerSize(cornerPx)
                } else {
                    builder
                        .setBottomLeftCornerSize(0f)
                        .setBottomRightCornerSize(0f)
                }

                child.shapeAppearanceModel = builder.build()
            }
        }
    }

    /** Collapse (close) all child ExpandableCardView */
    fun collapseAll() {
        for (i in 0 until childCount) {
            (getChildAt(i) as? ExpandableCardView)?.isExpanded = false
        }
    }

    /** Returns the index of the currently expanded card, or -1 if none */
    val selectedIndex: Int
        get() = (0 until childCount)
            .firstOrNull { (getChildAt(it) as? ExpandableCardView)?.isExpanded == true }
            ?: -1
}
