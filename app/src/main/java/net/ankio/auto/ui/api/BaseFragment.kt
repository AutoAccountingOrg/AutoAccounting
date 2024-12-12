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

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ViewUtils


/**
 * 基础的Fragment
 */
abstract class BaseFragment : Fragment() {

    abstract val binding: ViewBinding

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    override fun onStop() {
        super.onStop()
        App.pageStopOrDestroy()
    }


    open fun beforeViewBindingDestroy() {

    }

    override fun onDestroy() {
        super.onDestroy()
        App.pageStopOrDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val materialToolbar = ViewUtils.findMaterialToolbar(view) ?: return
        if (materialToolbar.navigationIcon != null) {
            val backIcon = androidx.appcompat.R.drawable.abc_ic_ab_back_material
            if (compareDrawables(materialToolbar.navigationIcon, requireContext(), backIcon)) {
                materialToolbar.setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
            }
        }
    }


    private fun compareDrawables(drawable1: Drawable?, context: Context, resId: Int): Boolean {
        if (drawable1 == null) return false
        val drawable2 = ContextCompat.getDrawable(context, resId)
        return drawable1.constantState == drawable2?.constantState
    }

}
