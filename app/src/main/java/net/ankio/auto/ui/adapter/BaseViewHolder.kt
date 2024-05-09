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

package net.ankio.auto.ui.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

open class BaseViewHolder(open val binding: ViewBinding, open val context: Context) : RecyclerView.ViewHolder(binding.root) {
    private lateinit var job: Job
    lateinit var scope: CoroutineScope
    var hasInit = false

    fun createScope() {
        cancelScope()
        job = Job()
        scope = CoroutineScope(Dispatchers.Main + job)
    }

    fun cancelScope() {
        if (::job.isInitialized && !job.isCancelled) job.cancel()
    }
}
