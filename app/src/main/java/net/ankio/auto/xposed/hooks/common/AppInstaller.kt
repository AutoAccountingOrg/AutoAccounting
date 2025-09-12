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

package net.ankio.auto.xposed.hooks.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import net.ankio.auto.BuildConfig
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.Server
import android.os.Process
import io.github.oshai.kotlinlogging.KotlinLogging
object AppInstaller {

    private val logger = KotlinLogging.logger(this::class.java.name)
    private const val TARGET_PACKAGE = BuildConfig.APPLICATION_ID

    fun init(context: Context, server: Server) {
        try {
            // 创建广播接收器
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.let {
                        val packageName = it.data?.schemeSpecificPart
                        if (packageName == TARGET_PACKAGE) {
                            when (it.action) {
                                Intent.ACTION_PACKAGE_ADDED,
                                Intent.ACTION_PACKAGE_REPLACED -> {
                                    runCatching {
                                        server.stopServer()
                                    }
                                    runCatching {
                                        AppRuntime.restart()
                                    }.onFailure { e ->
                                        logger.error(e) { }
                                        Process.killProcess(Process.myPid())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 注册广播接收器
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }

            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            logger.error(e) { }
        }
    }
}