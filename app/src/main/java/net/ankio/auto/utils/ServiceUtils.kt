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

package net.ankio.auto.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogProgressBinding
import net.ankio.auto.exceptions.UnsupportedDeviceException
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ServiceUtils(private val context: Context) {
    private var shell: String
    private var cacheDir: File? = null
    init {
        cacheDir = AppUtils.getApplication().externalCacheDir
        if (cacheDir === null) {
            throw UnsupportedDeviceException(context.getString(R.string.unsupport_device))
        }

        AppUtils.getScope().launch {
            AppUtils.getService().copyAssets()
        }

        shell = "sh ${cacheDir!!.path}/shell/starter.sh"
    }

    fun hashRoot():Boolean{
      return runCatching { Runtime.getRuntime().exec("su")
          true }.getOrDefault(false)
    }

    fun startServerByRoot(showUi: Boolean = true) {
        var textView: TextView? = null
        var scrollView: ScrollView? = null
        var progressDialog: AlertDialog? =  null
        if(showUi){
            val layoutInflater = LayoutInflater.from(context)
            val dialogBinding = DialogProgressBinding.inflate(layoutInflater)
             textView = dialogBinding.progressText
             scrollView = dialogBinding.scrollView
            progressDialog =
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.title_command)
                    .setView(dialogBinding.root)
                    .setCancelable(false) // 设置对话框不可关闭
                    .show()
        }


        // 在协程中检查 root 权限并执行命令
        AppUtils.getScope().launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("su")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val bufferedWriter = OutputStreamWriter(process.outputStream)

                Logger.i("Executing shell command: $shell")

                // 写入命令
                bufferedWriter.write(shell)
                bufferedWriter.flush()
                bufferedWriter.close()

                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    if(showUi){
                        withContext(Dispatchers.Main) {
                            // 更新 TextView 来显示命令输出
                            textView!!.append(line + "\n")
                            scrollView!!.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                        }
                    }else{
                        line?.let { Logger.d(it) }
                    }

                }
                process.waitFor()
                bufferedReader.close()
            } catch (e: Exception) {
                Logger.e("Error executing shell command", e)
                if(showUi) {
                    withContext(Dispatchers.Main) {
                        textView!!.append(context.getString(R.string.no_root_permission))
                    }
                }else{
                    Logger.e(context.getString(R.string.no_root_permission))
                }
            } finally {
                // 等待5秒钟关闭对话框
                delay(5000L)
                if(showUi) {
                    withContext(Dispatchers.Main) {
                        progressDialog!!.dismiss()
                    }
                }
            }
        }
    }

    fun copyAdbCommand() {
        AppUtils.copyToClipboard("adb shell $shell")
        Toaster.show(context.getString(R.string.copy_command_success))
    }

}