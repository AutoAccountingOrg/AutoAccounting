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

package net.ankio.auto.shell

import android.util.Log
import net.ankio.shortcuts.shell.RootShell
import net.ankio.shortcuts.shell.ShizukuShell
import java.io.Closeable

class Shell : Closeable {
    private val rootExecutor = RootShell()
    private val shizukuExecutor = ShizukuShell()

    suspend fun rootExecCommand(command: String): String {
        Log.d("Root执行命令", command)
        val data = rootExecutor.execute(command)
        // Log.d("命令执行结果",data)
        return data
    }

    suspend fun shizukuExecCommand(command: String): String {
        Log.d("shizuku执行命令", command)
        val data = shizukuExecutor.runAndGetOutput(command)
        // Log.d("命令执行结果",data)
        return data
    }

    private fun rootPermission(): Boolean = rootExecutor.openShell()

    private fun shizukuPermission(): Boolean =
        shizukuExecutor.isSupported && !shizukuExecutor.isPermissionDenied

    fun checkPermission(): Boolean {
        return rootPermission() || shizukuPermission()
    }

    override fun close() {
        rootExecutor.close()
    }

    suspend fun exec(command: String): String {
        if (rootPermission()) return rootExecCommand(command)
        else if (shizukuPermission()) return shizukuExecCommand(command)
        error("no permission")
    }


}