package net.ankio.auto.ui.fragment.intro

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.databinding.FragmentIntroPageSyncBinding
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import org.ezbook.server.db.model.BookNameModel

class IntroPageSyncFragment : BaseIntroPageFragment<FragmentIntroPageSyncBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNext.setOnClickListener {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.AI
        }

        binding.btnBack.setOnClickListener {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.FEATURE
        }


        binding.btnSync.setOnClickListener {
            val adapter = AppAdapterManager.adapter()
            adapter.syncAssets()
        }


    }

    private val POLL_INTERVAL = 1_000L
    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {                       // 视图销毁自动退出
                val synced = BookNameModel.list().isNotEmpty()

                if (synced) {
                    binding.syncState.setText(R.string.sync_state_success)
                    binding.syncImage.setImageResource(R.drawable.setting_icon_success)
                    ImageViewCompat.setImageTintList(
                        binding.syncImage,
                        ContextCompat.getColorStateList(requireContext(), R.color.log_warning)
                    )
                } else {
                    binding.syncState.setText(R.string.sync_state_not)
                    binding.syncImage.setImageResource(R.drawable.ic_warning)
                    ImageViewCompat.setImageTintList(
                        binding.syncImage,
                        ContextCompat.getColorStateList(
                            requireContext(),
                            App.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary)
                        )
                    )
                }

                if (synced) break                    // 成功后就不再轮询
                delay(POLL_INTERVAL)                 // 等下次
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
