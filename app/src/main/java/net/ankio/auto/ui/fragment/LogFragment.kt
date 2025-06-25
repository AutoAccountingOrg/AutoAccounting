package net.ankio.auto.ui.fragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
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


class LogFragment : BasePageFragment<LogModel, FragmentLogBinding>() {
    override suspend fun loadData(): List<LogModel> = Logger.readLogsAsModelsPaged(page, pageSize)

    override fun onCreateAdapter(): RecyclerView.Adapter<*> {
        val recyclerView = binding.statusPage.contentView
        recyclerView?.layoutManager = WrapContentLinearLayoutManager(requireContext())
        recyclerView?.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

        return LogAdapter()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_share -> {
                    val loadingUtils = LoadingUtils(requireActivity())
                    loadingUtils.show(R.string.loading_logs)
                    lifecycleScope.launch {
                        try {
                            val logFile = Logger.packageLogs(requireContext())
                            shareLogFile(logFile)
                        } catch (e: Exception) {
                            Logger.e("Share Log Error", e)
                        } finally {
                            loadingUtils.close()
                        }
                    }
                    true
                }

                R.id.item_clear -> {
                    BottomSheetDialogBuilder(this)
                        .setTitle(requireActivity().getString(R.string.delete_data))
                        .setMessage(requireActivity().getString(R.string.delete_msg))
                        .setPositiveButton(requireActivity().getString(R.string.sure_msg)) { _, _ ->
                            lifecycleScope.launch {
                                LogAPI.clear()
                                reload()
                            }
                        }
                        .setNegativeButton(requireActivity().getString(R.string.cancel_msg)) { _, _ -> }
                        .show()
                    true
                }

                else -> false
            }
        }
    }



    private fun shareLogFile(file: File) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            val fileUri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_file))
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_file)))
    }
}