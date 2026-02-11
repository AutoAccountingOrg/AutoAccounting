package net.ankio.auto.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.ankio.auto.ui.fragment.intro.IntroPageAIFragment
import net.ankio.auto.ui.fragment.intro.IntroPageAppFragment
import net.ankio.auto.ui.fragment.intro.IntroPageFeatureFragment
import net.ankio.auto.ui.fragment.intro.IntroPageHomeFragment
import net.ankio.auto.ui.fragment.intro.IntroPageKeepFragment
import net.ankio.auto.ui.fragment.intro.IntroPageModeFragment
import net.ankio.auto.ui.fragment.intro.IntroPagePermissionFragment
import net.ankio.auto.ui.fragment.intro.IntroPageSuccessFragment
import net.ankio.auto.ui.fragment.intro.IntroPageSyncFragment

class IntroPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = IntroPage.entries.size

    override fun createFragment(position: Int): Fragment =
        IntroPage.entries[position].create()      // ← 调用工厂函数

    enum class IntroPage(private val factory: () -> Fragment) {
        HOME({ IntroPageHomeFragment() }), // 1
        MODE({ IntroPageModeFragment() }), // 2
        PERMISSION({ IntroPagePermissionFragment() }), // 3
        KEEP({ IntroPageKeepFragment() }), // 4
        APP({ IntroPageAppFragment() }),
        FEATURE({ IntroPageFeatureFragment(); }),
        SYNC({ IntroPageSyncFragment(); }),
        AI({ IntroPageAIFragment(); }),
        SUCCESS({ IntroPageSuccessFragment(); })
        ;

        fun create(): Fragment = factory()         // 每次都拿到"全新"实例
    }
}
