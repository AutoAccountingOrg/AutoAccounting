package net.ankio.auto.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.ankio.auto.ui.fragment.intro.IntroPage1Fragment
import net.ankio.auto.ui.fragment.intro.IntroPage2Fragment

class IntroPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 3 // 页数

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> IntroPage1Fragment()
            1 -> IntroPage2Fragment()
            2 -> IntroPage1Fragment()
            else -> throw IllegalArgumentException("Invalid page")
        }
    }
}