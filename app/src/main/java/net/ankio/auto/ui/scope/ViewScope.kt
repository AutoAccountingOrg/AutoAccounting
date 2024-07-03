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

package net.ankio.auto.ui.scope
import android.view.View
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import net.ankio.auto.R
private class ViewStateListener(
    private val view: View,
    private val job: Job
) : View.OnAttachStateChangeListener, CompletionHandler {
    override fun onViewDetachedFromWindow(v: View) {
        view.removeOnAttachStateChangeListener(this)
        job.cancel()
    }

    override fun onViewAttachedToWindow(v: View) {}

    override fun invoke(cause: Throwable?) {
        view.removeOnAttachStateChangeListener(this)
        job.cancel()
    }
}
private class ViewAutoDisposeInterceptor(
    private val view: View
) : ContinuationInterceptor {
    override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        val job = continuation.context[Job]
        if (job != null) {
            view.addOnAttachStateChangeListener(ViewStateListener(view, job))
        }
        return continuation
    }
}
val View.autoDisposeScope: CoroutineScope
    get() {
        val exist = getTag(R.string.auto_dispose_scope) as? CoroutineScope
        if (exist != null) {
            return exist
        }
        val newScope = CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Main +
                    ViewAutoDisposeInterceptor(this)
        )
        setTag(R.string.auto_dispose_scope, newScope)
        return newScope
    }