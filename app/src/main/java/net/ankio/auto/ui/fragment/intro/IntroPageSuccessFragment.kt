package net.ankio.auto.ui.fragment.intro

import android.content.Intent
import android.os.Bundle
import android.view.View
import net.ankio.auto.databinding.FragmentIntroPageSuccessBinding
import net.ankio.auto.ui.activity.HomeActivity

class IntroPageSuccessFragment : BaseIntroPageFragment<FragmentIntroPageSuccessBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnStart.setOnClickListener {
            val intent = Intent(context, HomeActivity::class.java)
            startActivity(intent)
        }


    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
