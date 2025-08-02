package net.ankio.auto.ui.fragment.intro

import android.os.Bundle
import android.view.View
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.databinding.FragmentIntroPageAiBinding
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.settings.AiComponent

class IntroPageAIFragment : BaseIntroPageFragment<FragmentIntroPageAiBinding>() {



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ai.bindAs<AiComponent>(lifecycle, requireActivity())


        binding.btnNext.setOnClickListener {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.SUCCESS
        }


        binding.btnBack.setOnClickListener {

            if (AppAdapterManager.adapter().supportSyncAssets()) {
                vm.pageRequest.value = IntroPagerAdapter.IntroPage.SYNC
            } else {
                vm.pageRequest.value = IntroPagerAdapter.IntroPage.FEATURE
            }


        }
    }



}
