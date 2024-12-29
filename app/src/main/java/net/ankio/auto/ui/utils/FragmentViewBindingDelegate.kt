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

package net.ankio.auto.ui.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.google.android.material.appbar.MaterialToolbar
import net.ankio.auto.ui.api.BaseFragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FragmentViewBindingDelegate<T : ViewBinding>(
    val fragment: BaseFragment,
    private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> T
) : ReadOnlyProperty<BaseFragment, T> {

    private var binding: T? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
                    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            // 查找所有的MaterialToolbar
                            if (binding != null) {
                                val appToolbar = ViewUtils.findView(
                                    binding!!.root,
                                    0,
                                    5,
                                    MaterialToolbar::class.java
                                )
                                if (appToolbar != null) {
                                    appToolbar.setOnMenuItemClickListener(null)
                                    appToolbar.setNavigationOnClickListener(null)
                                }
                                fragment.beforeViewBindingDestroy()
                                binding = null
                            }

                        }
                    })
                }
            }
        })
    }

    override fun getValue(thisRef: BaseFragment, property: KProperty<*>): T {
        val currentBinding = binding
        if (currentBinding != null) {
            return currentBinding
        }

        val lifecycle = fragment.viewLifecycleOwner.lifecycle
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            throw IllegalStateException("Should not attempt to get bindings when Fragment views are destroyed.")
        }

        return bindingInflater.invoke(thisRef.layoutInflater, null, false)
            .also { this.binding = it }
    }
}

fun <T : ViewBinding> BaseFragment.viewBinding(bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> T) =
    FragmentViewBindingDelegate(this, bindingInflater)