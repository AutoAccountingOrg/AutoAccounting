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

package net.ankio.shell

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import net.ankio.shell.BuildConfig
import net.ankio.shell.IUserService
import net.ankio.shell.shizuku.UserService
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs

class ShizukuShell(private val packageName: String) {

    private var serviceReady = CompletableDeferred<IUserService?>()

    /**
     * 异步绑定 UserService，超时后自动取消
     */
    suspend fun ensureUserService(timeoutMillis: Long = 30_000): IUserService? {
        if (mUserService != null) return mUserService

        if (!serviceReady.isCompleted) {
            val args = UserServiceArgs(
                ComponentName(packageName, UserService::class.java.name)
            ).apply {
                daemon(false)
                processNameSuffix("service")
            }

            Shizuku.bindUserService(args, serviceConnection)
        }

        return try {
            withTimeout(timeoutMillis) {
                serviceReady.await()
            }
        } catch (e: Exception) {
            Log.e("ShizukuShell", "Service binding timeout", e)
            null
        }
    }

    /**
     * 协程方式执行命令并获取输出
     */
    suspend fun runAndGetOutput(command: String): String {
        val service = ensureUserService() ?: return ""
        return try {
            service.execCommand(command)
        } catch (e: Exception) {
            Log.e("ShizukuShell", "Command execution failed", e)
            ""
        }
    }

    /**
     * 单纯执行命令，无返回
     */
    suspend fun runCommand(command: String) {
        val service = ensureUserService() ?: return
        try {
            service.execCommand(command)
        } catch (e: Exception) {
            Log.e("ShizukuShell", "Command execution failed", e)
        }
    }

    /**
     * 权限检查
     */
    val isPermissionDenied: Boolean
        get() = Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED

    val isReady: Boolean
        get() = isSupported && !isPermissionDenied

    val isSupported: Boolean
        get() = Shizuku.pingBinder() && Shizuku.getVersion() >= 11 && !Shizuku.isPreV11()

    fun requestPermission() {
        Shizuku.requestPermission(0)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder?.pingBinder() == true) {
                mUserService = IUserService.Stub.asInterface(binder)
                serviceReady.complete(mUserService)
            } else {
                serviceReady.complete(null)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mUserService = null
            serviceReady = CompletableDeferred() // reset for next bind
        }
    }

    companion object {
        private var mUserService: IUserService? = null
    }
}
