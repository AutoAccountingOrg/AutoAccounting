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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogProgressBinding
import net.ankio.auto.databinding.FragmentServiceBinding
import net.ankio.auto.exceptions.UnsupportedDeviceException
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.AutoAccountingServiceUtils
import net.ankio.auto.utils.Logger
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ServiceFragment : BaseFragment() {
    private lateinit var binding: FragmentServiceBinding
    private lateinit var shell: String
    private var cacheDir: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentServiceBinding.inflate(layoutInflater)

        initView()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activityBinding.toolbar.visibility = View.GONE
    }

    private fun initView() {
        cacheDir = AppUtils.getApplication().externalCacheDir
        if (cacheDir === null) {
            throw UnsupportedDeviceException(getString(R.string.unsupport_device))
        }
        val loading = LoadingUtils(requireActivity())
        loading.setText(R.string.wait_shell_update)
        lifecycleScope.launch {
            AppUtils.getService().copyAssets()
            withContext(Dispatchers.Main) {
                loading.close()
                checkService()
            }
        }
        shell = "sh ${cacheDir!!.path}/shell/starter.sh"

        binding.start.setOnClickListener {
            // 启动服务
            startServerByRoot()
        }
        binding.copyCommand.setOnClickListener {
            // 复制命令
            AppUtils.copyToClipboard("adb shell $shell")
            Toaster.show(getString(R.string.copy_command_success))
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAffinity(requireActivity()) // 关闭所有活动并退出应用
                }
            },
        )
    }

    private fun checkService() {
        val checkInterval = 1000L // 检查间隔，这里设置为5000毫秒（5秒）
        // 在一个新的协程中执行定时任务
        lifecycleScope.launch(Dispatchers.IO) { // 默认在主线程执行

            while (isActive && !AutoAccountingServiceUtils.isServerStart(5)) { // 循环直到协程被取消
                delay(checkInterval)
            }
            withContext(Dispatchers.Main) {
                findNavController().navigate(R.id.homeFragment)
                requireActivity().recreate()
            }
        }
    }

    private fun startServerByRoot() {
        val dialogBinding = DialogProgressBinding.inflate(layoutInflater)
        val textView = dialogBinding.progressText
        val scrollView = dialogBinding.scrollView
        val progressDialog =
            MaterialAlertDialogBuilder(requireActivity())
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

                Logger.i("Executing shell command: $shell")

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
                // 等待5秒钟关闭对话框
                delay(5000L)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            }
        }
    }
}
