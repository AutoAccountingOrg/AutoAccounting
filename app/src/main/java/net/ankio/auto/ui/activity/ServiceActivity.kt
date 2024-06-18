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

package net.ankio.auto.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityServiceBinding
import net.ankio.auto.databinding.DialogProgressBinding
import net.ankio.auto.exceptions.UnsupportedDeviceException
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.ServiceUtils
import net.ankio.auto.utils.server.AutoServer
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class ServiceActivity : BaseActivity() {
    private lateinit var binding: ActivityServiceBinding
    private lateinit var serviceUtils: ServiceUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceBinding.inflate(layoutInflater)

        //   EventBus.register(AutoServerConnectedEvent::class.java, onConnectedListener)
        setContentView(binding.root)
        initView()
        onViewCreated()

    }



    override fun onDestroy() {
        super.onDestroy()
        //   EventBus.unregister(AutoServerConnectedEvent::class.java, onConnectedListener)
    }

    private fun initView() {
        serviceUtils = ServiceUtils(this)


        if (serviceUtils.hasRoot()){
            serviceUtils.startServerByRoot(true)
        }

        binding.start.setOnClickListener {
            // 启动服务
            serviceUtils.startServerByRoot()
        }
        binding.copyCommand.setOnClickListener {
            // 复制命令
            serviceUtils.copyAdbCommand()
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAffinity(this@ServiceActivity) // 关闭所有活动并退出应用
                }
            },
        )

        lifecycleScope.launch {
            // 检测52045端口是否开放
            val host = AutoServer.HOST.replace("ws://", "")
            val port = AutoServer.PORT

            withContext(Dispatchers.IO) {
                while (!isPortOpen(host, port)) {
                    Logger.i("Waiting for port $port to open")
                    delay(1000)
                }
                withContext(Dispatchers.Main) {
                    AppUtils.restart()
                }
            }
        }
    }

    private fun isPortOpen(
        ip: String,
        port: Int,
    ): Boolean {
        return try {
            Socket(ip, port).use {
                true
            }
        } catch (e: Exception) {
            false
        }
    }


}
