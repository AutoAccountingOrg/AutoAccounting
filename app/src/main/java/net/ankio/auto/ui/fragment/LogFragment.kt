package net.ankio.auto.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentLogBinding
import net.ankio.auto.http.api.LogAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.LogAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.ListPopupUtilsGeneric
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.adapterBottom
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.model.LogModel
import java.io.File

/**
 * 日志管理Fragment
 *
 * 该Fragment负责显示应用程序的日志信息，提供以下功能：
 * - 分页显示日志列表，支持应用和日志级别筛选
 * - 分享日志文件
 * - 清空日志数据
 *
 * 继承自BasePageFragment，支持分页加载和下拉刷新
 */
class LogFragment : BasePageFragment<LogModel, FragmentLogBinding>() {

    /** 应用筛选：空表示所有应用 */
    private var appFilter = ""

    /** 日志级别筛选：空表示所有级别 */
    private var levelFilter = mutableListOf<String>()

    /**
     * 加载日志数据
     */
    override suspend fun loadData(): List<LogModel> = LogAPI.list(
        page, pageSize, appFilter, levelFilter
    )

    /**
     * 创建RecyclerView适配器
     *
     * 设置RecyclerView的布局管理器和分割线装饰器，
     * 创建并返回LogAdapter实例
     *
     * @return 配置好的RecyclerView适配器
     */
    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        // 获取RecyclerView实例
        val recyclerView = binding.statusPage.contentView

        // 设置布局管理器为垂直线性布局
        recyclerView?.layoutManager = WrapContentLinearLayoutManager(requireContext())

        // 添加垂直分割线装饰器
        recyclerView?.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

        // 设置筛选器
        setupFilters()
        
        // 返回日志适配器
        return LogAdapter()
    }

    /**
     * 设置筛选器
     */
    private fun setupFilters() {
        levelFilter.clear()

        // 设置应用筛选
        binding.inputAppButton.text = getString(R.string.filter_type_all)
        binding.inputAppChevron.setOnClickListener { anchorView ->
            launch {
                val apps = LogAPI.getApps()
                val appItems = linkedMapOf<String, String>().apply {
                    put(getString(R.string.filter_type_all), "")
                    apps.forEach { app ->
                        val displayName = app.substringAfterLast(".")
                        put(displayName, app)
                    }
                }

                ListPopupUtilsGeneric.create<Map.Entry<String, String>>(requireContext())
                    .setAnchor(anchorView)
                    .setList(appItems.entries.associateBy({ it.key }, { it }))
                    .setOnItemClick { _, key, entry ->
                        appFilter = entry.value
                        binding.inputAppButton.text = key
                        reload()
                    }.setOnDismiss {
                        binding.inputAppChevron.isChecked = false
                    }
                    .show()
            }
        }

        // 设置级别筛选
        val levelItems = linkedMapOf(
            getString(R.string.filter_type_all) to null,
            getString(R.string.log_level_debug) to LogLevel.DEBUG,
            getString(R.string.log_level_info) to LogLevel.INFO,
            getString(R.string.log_level_warn) to LogLevel.WARN,
            getString(R.string.log_level_error) to LogLevel.ERROR
        )
        binding.inputLevelButton.text = levelItems.keys.first()
        binding.inputLevelChevron.setOnClickListener { anchorView ->
            ListPopupUtilsGeneric.create<Map.Entry<String, LogLevel?>>(requireContext())
                .setAnchor(anchorView)
                .setList(levelItems.entries.associateBy({ it.key }, { it }))
                .setOnItemClick { _, key, entry ->
                    levelFilter.clear()
                    entry.value?.let { levelFilter.add(it.name) }
                    binding.inputLevelButton.text = key
                    reload()
                }.setOnDismiss {
                    binding.inputLevelChevron.isChecked = false
                }
                .show()
        }
    }

    /**
     * Fragment视图创建完成后的初始化
     *
     * 设置顶部应用栏的菜单项点击监听器，
     * 处理分享日志和清空日志的操作
     *
     * @param view 创建的视图
     * @param savedInstanceState 保存的实例状态
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置顶部应用栏菜单项点击监听器
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // 清空日志数据
                R.id.item_clear -> {
                    // 显示确认对话框
                    BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
                        .setTitle(requireActivity().getString(R.string.delete_data))
                        .setMessage(requireActivity().getString(R.string.delete_msg))
                        .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                            // 用户确认后清空日志并重新加载
                            launch {
                                LogAPI.clear()
                                reload()
                            }
                        }
                        .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
                        .show()
                    true
                }

                // 其他菜单项不处理
                else -> false
            }
        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.statusPage.adapterBottom(requireContext())

        // 设置右下角悬浮按钮分享日志
        binding.shareButton.setOnClickListener {
            val loadingUtils = LoadingUtils(requireActivity())
            loadingUtils.show(R.string.loading_logs)
            launch {
                val logFile = Logger.packageLogs(requireContext())
                shareLogFile(logFile)
                loadingUtils.close()
            }
        }
    }

    /**
     * 分享日志文件
     *
     * 创建分享意图，使用FileProvider提供文件访问权限，
     * 启动系统分享选择器
     *
     * @param file 要分享的日志文件
     */
    private fun shareLogFile(file: File) {
        // 创建分享意图
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            // 设置MIME类型为二进制流
            type = "application/octet-stream"

            // 使用FileProvider获取文件URI，确保安全访问
            val fileUri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            // 设置分享的文件流
            putExtra(Intent.EXTRA_STREAM, fileUri)

            // 添加读取权限标志
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // 设置分享主题
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_file))
        }

        // 启动系统分享选择器
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_file)))
    }
}