package net.ankio.auto.ui.fragment.intro

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import net.ankio.auto.databinding.FragmentIntroPage1Binding
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.vm.IntroSharedVm
import net.ankio.auto.utils.PrefManager

class IntroPage1Fragment : BaseFragment<FragmentIntroPage1Binding>() {
    private val vm: IntroSharedVm by activityViewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnStart.setOnClickListener {
            vm.pageRequest.value = 1
        }


    }

    override fun onResume() {
        super.onResume()
        PrefManager.introIndex = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
