package net.ankio.auto.ui.fragment.intro

import android.os.Bundle
import android.view.View
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.adapter.IAppAdapter
import net.ankio.auto.constant.BookFeatures
import net.ankio.auto.databinding.FragmentIntroPageFeatureBinding
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.utils.PrefManager

/**
 * 功能选择页（资产管理、多账本、报销等）
 */
class IntroPageFeatureFragment :
    BaseIntroPageFragment<FragmentIntroPageFeatureBinding>() {

    /** 卡片 <-> PrefManager 映射 */
    private val featureMap by lazy {
        mapOf(
            BookFeatures.ASSET_MANAGE to FeatureBinding(
                binding.cardAssetManage,
                { PrefManager.featureAssetManage },
                { PrefManager.featureAssetManage = it }
            ),
            BookFeatures.MULTI_BOOK to FeatureBinding(
                binding.cardMultiBook,
                { PrefManager.featureMultiBook },
                { PrefManager.featureMultiBook = it }
            ),
            BookFeatures.REIMBURSEMENT to FeatureBinding(
                binding.cardReimbursement,
                { PrefManager.featureReimbursement },
                { PrefManager.featureReimbursement = it }
            ),
            BookFeatures.LEADING to FeatureBinding(
                binding.cardLeading,
                { PrefManager.featureLeading },
                { PrefManager.featureLeading = it }
            ),
            BookFeatures.FEE to FeatureBinding(
                binding.cardFee,
                { PrefManager.featureFee },
                { PrefManager.featureFee = it }
            ),
            BookFeatures.MULTI_CURRENCY to FeatureBinding(
                binding.cardMultiCurrency,
                { PrefManager.featureMultiCurrency },
                { PrefManager.featureMultiCurrency = it }
            ),
            BookFeatures.TAG to FeatureBinding(
                binding.cardTag,
                { PrefManager.featureTag },
                { PrefManager.featureTag = it }
            )
        )
    }

    private lateinit var bookAdapter: IAppAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // “下一步” 按钮
        binding.btnContinue.setOnClickListener {
            if (bookAdapter.supportSyncAssets()) {
                vm.pageRequest.value = IntroPagerAdapter.IntroPage.SYNC
            } else {
                // 跳转默认配置页面
                vm.pageRequest.value = IntroPagerAdapter.IntroPage.AI
            }

        }

        // 初始化卡片状态 & 监听
        featureMap.forEach { (key, fb) ->
            fb.card.apply {
                setOnSwitchChanged { _, checked ->
                    fb.setter(checked)      // 写回 PrefManager
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bookAdapter = AppAdapterManager.adapter()
        // 初始化卡片状态 & 监听
        featureMap.forEach { (key, fb) ->
            fb.card.apply {
                val use = bookAdapter.features().contains(key)
                useSwitch(use)
                if (!use) {
                    setSwitch(false)
                } else {
                    setSwitch(fb.getter())
                }
            }
        }
    }

    /** 组合数据类，便于统一管理 */
    private data class FeatureBinding(
        val card: net.ankio.auto.ui.components.ExpandableCardView,
        val getter: () -> Boolean,
        val setter: (Boolean) -> Unit
    )
}
