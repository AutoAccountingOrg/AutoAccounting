package net.ankio.auto.ui.fragment.intro

import android.os.Bundle
import android.view.View
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.FragmentIntroPageModeBinding
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.utils.PrefManager

class IntroPageModeFragment : BaseIntroPageFragment<FragmentIntroPageModeBinding>() {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNext.setOnClickListener {
            val index = binding.cardGroup.selectedIndex
            PrefManager.workMode = WorkMode.entries[index]
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.PERMISSION
        }

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
