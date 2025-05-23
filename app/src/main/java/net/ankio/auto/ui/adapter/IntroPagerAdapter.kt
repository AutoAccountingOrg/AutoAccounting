package net.ankio.auto.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.ankio.auto.ui.fragment.intro.IntroPageAppFragment
import net.ankio.auto.ui.fragment.intro.IntroPageHomeFragment
import net.ankio.auto.ui.fragment.intro.IntroPageKeepFragment
import net.ankio.auto.ui.fragment.intro.IntroPageModeFragment
import net.ankio.auto.ui.fragment.intro.IntroPagePermissionFragment
import net.ankio.auto.ui.fragment.intro.IntroPageSyncFragment

class IntroPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = IntroPage.entries.size

    override fun createFragment(position: Int): Fragment =
        IntroPage.entries[position].create()      // ← 调用工厂函数

    enum class IntroPage(private val factory: () -> Fragment) {
        HOME({ IntroPageHomeFragment() }),
        MODE({ IntroPageModeFragment() }),
        PERMISSION({ IntroPagePermissionFragment() }),
        KEEP({ IntroPageKeepFragment() }),
        APP({ IntroPageAppFragment() }),
        SYNC({ IntroPageSyncFragment(); })

        ;

        fun create(): Fragment = factory()         // 每次都拿到“全新”实例
    }
}
