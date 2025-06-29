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
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.utils.LoadingUtils
import org.ezbook.server.db.model.LogModel
import java.io.File

/**
 * 日志管理Fragment
 *
 * 该Fragment负责显示应用程序的日志信息，提供以下功能：
 * - 分页显示日志列表
 * - 分享日志文件
 * - 清空日志数据
 *
 * 继承自BasePageFragment，支持分页加载和下拉刷新
 */
class LogFragment : BasePageFragment<LogModel, FragmentLogBinding>() {

    /**
     * 加载日志数据
     *
     * 从Logger中分页读取日志数据并转换为LogModel列表
     *
     * @return 当前页的日志数据列表
     */
    override suspend fun loadData(): List<LogModel> = Logger.readLogsAsModelsPaged(page, pageSize)

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

        // 返回日志适配器
        return LogAdapter()
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
                // 分享日志文件
                R.id.item_share -> {
                    // 显示加载对话框
                    val loadingUtils = LoadingUtils(requireActivity())
                    loadingUtils.show(R.string.loading_logs)

                    // 在协程中执行分享操作
                    lifecycleScope.launch {
                        try {
                            // 打包日志文件
                            val logFile = Logger.packageLogs(requireContext())
                            // 分享日志文件
                            shareLogFile(logFile)
                        } catch (e: Exception) {
                            // 记录分享错误
                            Logger.e("Share Log Error", e)
                        } finally {
                            // 关闭加载对话框
                            loadingUtils.close()
                        }
                    }
                    true
                }

                // 清空日志数据
                R.id.item_clear -> {
                    // 显示确认对话框
                    BottomSheetDialogBuilder(this)
                        .setTitle(requireActivity().getString(R.string.delete_data))
                        .setMessage(requireActivity().getString(R.string.delete_msg))
                        .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                            // 用户确认后清空日志并重新加载
                            lifecycleScope.launch {
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