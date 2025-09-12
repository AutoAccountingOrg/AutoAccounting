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

package net.ankio.auto.xposed.core.logger

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Layout
import ch.qos.logback.core.UnsynchronizedAppenderBase
import java.lang.reflect.Method

class XposedBridgeAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {

    lateinit var encoder: Layout<ILoggingEvent>

    private var xposedBridgeLogMetho: Method? = null

    override fun append(eventObject: ILoggingEvent) {
        if (!isStarted || xposedBridgeLogMetho == null) return

        val formattedPart = encoder.doLayout(eventObject)

        try {
            xposedBridgeLogMetho!!.invoke(null, formattedPart)
        } catch (e: Exception) {
            xposedBridgeLogMetho = null
            addError("Failed to invoke XposedBridge.log method.", e)
        }
    }


    override fun start() {
        // 检查 layout 是否已经设置
        if (!::encoder.isInitialized) {
            addError("Layout has not been set for the Appender named [$name].")
            return
        }

        xposedBridgeLogMetho = try {
            val cls = Class.forName("de.robv.android.xposed.XposedBridge")
            cls.getDeclaredMethod("log", String::class.java)
        } catch (e: Exception) {
            addError("XposedBridge class not found. Is Xposed installed?", e)
            return
        }

        super.start()
    }

}