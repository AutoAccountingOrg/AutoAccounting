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

import android.content.res.AssetManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityServiceBinding
import net.ankio.auto.databinding.DialogProgressBinding
import net.ankio.auto.exceptions.UnsupportedDeviceException
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.AutoAccountingServiceUtils
import net.ankio.auto.utils.Logger
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

class ServiceActivity : BaseActivity() {
    private lateinit var binding: ActivityServiceBinding
    private lateinit var shell: String
    private var cacheDir: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        cacheDir = AppUtils.getApplication().externalCacheDir
        if (cacheDir === null) {
            throw UnsupportedDeviceException(getString(R.string.unsupport_device))
        }

        super.onCreate(savedInstanceState)
        shell = "sh ${cacheDir!!.path}/shell/starter.sh"
        //复制二进制文件到缓存路径
        lifecycleScope.launch {
            copyAssetsShellFolderToCache()
        }
        //绑定
        binding = ActivityServiceBinding.inflate(layoutInflater)
        binding.start.setOnClickListener {
            //启动服务
            startServerByRoot()
        }
        binding.copyCommand.setOnClickListener {
            //复制命令
            AppUtils.copyToClipboard("adb shell $shell")
            Toast.makeText(
                this@ServiceActivity,
                getString(R.string.copy_command_success),
                Toast.LENGTH_SHORT
            ).show()
        }
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity() // 关闭所有活动并退出应用
            }
        })
        onViewCreated()
        checkService()
    }

    private fun checkService() {
        val checkInterval = 1000L // 检查间隔，这里设置为5000毫秒（5秒）
        // 在一个新的协程中执行定时任务
        lifecycleScope.launch(Dispatchers.IO) { // 默认在主线程执行

            while (isActive) { // 循环直到协程被取消
                // 切换到IO线程执行耗时I/O操作
                val isServerStarted =
                    AutoAccountingServiceUtils.isServerStart(this@ServiceActivity)

                // 回到主线程执行UI操作
                if (isServerStarted) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ServiceActivity,
                            getString(R.string.service_started),
                            Toast.LENGTH_SHORT
                        ).show()
                        AppUtils.restart() // 假设这需要在主线程执行
                    }
                    delay(3000L) // 在循环中等待一段时间后再次检查
                } else {
                    delay(checkInterval) // 等待指定的检查间隔后再次检查
                }
            }


        }

    }


    private fun startServerByRoot() {


        val dialogBinding = DialogProgressBinding.inflate(layoutInflater)
        val textView = dialogBinding.progressText
        val scrollView = dialogBinding.scrollView
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_command)
            .setView(dialogBinding.root)
            .setCancelable(false) // 设置对话框不可关闭
            .show()

        // 在协程中检查 root 权限并执行命令
        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val process = Runtime.getRuntime().exec("su")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val bufferedWriter = OutputStreamWriter(process.outputStream)

                // 写入命令
                bufferedWriter.write(shell)
                bufferedWriter.flush()
                bufferedWriter.close()

                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        // 更新 TextView 来显示命令输出
                        textView.append(line + "\n")
                        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                    }
                }
                process.waitFor()
                bufferedReader.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.e("Error executing shell command", e)
                withContext(Dispatchers.Main) {
                    textView.append(getText(R.string.no_root_permission))
                }
            } finally {
                //等待5秒钟关闭对话框
                delay(5000L)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            }
        }
    }


    private fun copyAssetsShellFolderToCache() {
        val assetManager = assets
        val shellFolderPath = "shell"
        val destinationPath = cacheDir!!.path + File.separator + shellFolderPath
        Logger.i("Copying shell folder from assets to $destinationPath")
        copyFolderFromAssets(assetManager, shellFolderPath, destinationPath)
    }

    private fun copyFolderFromAssets(
        assetManager: AssetManager,
        sourceFolderPath: String,
        destinationFolderPath: String
    ) {
        try {
            val files = assetManager.list(sourceFolderPath) ?: return

            // Create the destination folder if it doesn't exist
            val destinationFolder = File(destinationFolderPath)
            if (!destinationFolder.exists()) {
                destinationFolder.mkdirs()
            }

            for (filename in files) {
                val sourceFilePath =
                    if (sourceFolderPath == "") filename else "$sourceFolderPath/$filename"
                val destinationFilePath = "$destinationFolderPath/$filename"

                try {
                    val inputStream = assetManager.open(sourceFilePath)
                    copyFile(inputStream, destinationFilePath)
                } catch (e: IOException) {
                    // If we encounter an IOException, it might be because it's a directory
                    copyFolderFromAssets(assetManager, sourceFilePath, destinationFilePath)
                }
            }
        } catch (e: IOException) {
            Logger.e("Error copying shell folder", e)
        }
    }

    private fun copyFile(inputStream: InputStream, destinationFilePath: String) {
        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(destinationFilePath)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
        } catch (e: IOException) {
            Logger.e("Error copying file", e)
        } finally {
            try {
                inputStream.close()
                outputStream?.close()
            } catch (e: IOException) {
                Logger.e("Error closing streams", e)
            }
        }
    }


}