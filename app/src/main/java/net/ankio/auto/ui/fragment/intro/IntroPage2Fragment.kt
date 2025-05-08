package net.ankio.auto.ui.fragment.intro

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.databinding.FragmentIntroPage2Binding
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.vm.IntroSharedVm
import net.ankio.auto.utils.PrefManager

class IntroPage2Fragment : BaseFragment<FragmentIntroPage2Binding>() {

    private val vm: IntroSharedVm by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNext.setOnClickListener {
            val index = binding.cardGroup.selectedIndex
            PrefManager.workMode = WorkMode.entries[index]
            vm.pageRequest.value = 2
        }

    }

    override fun onResume() {
        super.onResume()
        PrefManager.introIndex = 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
