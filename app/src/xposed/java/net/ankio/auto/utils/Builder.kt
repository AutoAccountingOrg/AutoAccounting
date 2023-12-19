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

package net.ankio.auto.utils

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable



class Builder {
    var defaultColor = Color.TRANSPARENT
    var pressedColor = -0x1a1a1b
    var round = 0
    fun create(): Drawable {
        val statesDrawable = StateListDrawable()
        statesDrawable.addState(
            intArrayOf( android.R.attr.state_pressed),
            if (round > 0) roundDrawable(round, pressedColor) else ColorDrawable(pressedColor)
        )
        statesDrawable.addState(
            intArrayOf(),
            if (round > 0) roundDrawable(round, defaultColor) else ColorDrawable(defaultColor)
        )
        return statesDrawable
    }

    fun defaultColor(color: Int): Builder {
        defaultColor = color
        return this
    }

    fun pressedColor(color: Int): Builder {
        pressedColor = color
        return this
    }

    /**
     * @param round pixel
     * @return
     */
    fun round(round: Int): Builder {
        this.round = round
        return this
    }

    companion object {
        private fun roundDrawable(round: Int, color: Int): Drawable {
            val shape = GradientDrawable()
            shape.cornerRadius = round.toFloat()
            shape.setColor(color)
            return shape
        }
    }
}