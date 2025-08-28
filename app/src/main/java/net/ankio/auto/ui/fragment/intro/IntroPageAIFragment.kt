package net.ankio.auto.ui.fragment.intro

import android.os.Bundle
import android.view.View
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.databinding.FragmentIntroPageAiBinding
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.fragment.settings.AiComponent

/**
 * AI设置引导页面 - Linus式极简设计
 *
 * 设计原则：
 * 1. 消除参数冗余 - bindAs自动推断生命周期，无需手动传递
 * 2. 清晰的职责分离 - 只负责页面导航逻辑，AI配置交给AiComponent
 * 3. 简洁的代码结构 - 消除不必要的空行和注释
 */
class IntroPageAIFragment : BaseIntroPageFragment<FragmentIntroPageAiBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 使用新的bindAs API - 自动推断生命周期，无需额外参数
        binding.ai.bindAs<AiComponent>()

        // 设置导航按钮事件
        binding.btnNext.setOnClickListener {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.SUCCESS
        }

        binding.btnBack.setOnClickListener {
            vm.pageRequest.value = if (AppAdapterManager.adapter().supportSyncAssets()) {
                IntroPagerAdapter.IntroPage.SYNC
            } else {
                IntroPagerAdapter.IntroPage.FEATURE
            }
        }
    }
}
