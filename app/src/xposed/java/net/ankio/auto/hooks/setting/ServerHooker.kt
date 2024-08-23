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

package net.ankio.auto.hooks.setting

import android.content.Context
import de.robv.android.xposed.XposedBridge
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import org.ezbook.server.Server

class ServerHooker(hooker: Hooker) : PartHooker(hooker) {
    override val hookName: String
        get() = "Server"

    override fun onInit(classLoader: ClassLoader, context: Context) {
       try {
           Server().startServer()
           XposedBridge.log("ServerHooker onInit success")
       }catch (e: Exception){
           e.printStackTrace()
           XposedBridge.log("ServerHooker onInit error: ${e.message}")
       }
    }
}