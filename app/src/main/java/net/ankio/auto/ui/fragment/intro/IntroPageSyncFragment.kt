package net.ankio.auto.ui.fragment.intro

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.ankio.auto.R
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.databinding.FragmentIntroPageSyncBinding
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.ui.theme.DynamicColors

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
        launch {
            while (isActive) {                       // 视图销毁自动退出
                val synced = BookNameAPI.list().isNotEmpty()

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
                        ColorStateList.valueOf(DynamicColors.Primary)
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
