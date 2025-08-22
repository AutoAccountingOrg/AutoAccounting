package net.ankio.auto.ui.fragment.intro

import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import net.ankio.auto.R
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.adapter.IAppAdapter
import net.ankio.auto.databinding.FragmentIntroPageAppBinding
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.ui.components.ExpandableCardView
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.isAppInstalled

class IntroPageAppFragment : BaseIntroPageFragment<FragmentIntroPageAppBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        readApp()

        /*  binding.nex.setOnClickListener {
              vm.pageRequest.value = 1
          }*/

        binding.btnNext.setOnClickListener {
            PrefManager.bookApp = apps[binding.ledgerCardGroup.selectedIndex].pkg
            //选择功能
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.FEATURE

        }
        binding.btnBack.setOnClickListener {
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.KEEP
        }

    }

    lateinit var apps:
            List<IAppAdapter>

    private fun readApp() {

// 3. 转换为 AutoApp 对象
        apps = AppAdapterManager.adapterList()

        // 4. 遍历绑定到 UI
        apps.forEachIndexed { index, app ->
            val card = ExpandableCardView(requireContext()).apply {
                enableTint(false)
                // icon 可能是 URL，也可能是本地资源名
                if (app.icon.startsWith("http")) {
                    Glide.with(this).load(app.icon).into(icon)
                } else {
                    icon.setImageResource(R.mipmap.ic_launcher)
                }
                setTitle(app.name)
                setDescription(app.desc)
                setOnCardClickListener {
                    //判断App是否安装
                    if (!context.isAppInstalled(app.pkg) && app.link.isNotEmpty()) {
                        CustomTabsHelper.launchUrl(context, app.link.toUri())
                    }
                }
                if (index == 0) {
                    isExpanded = true
                }
            }
            binding.ledgerCardGroup.addView(card)
        }

    }

    override fun onResume() {
        super.onResume()
        binding.ledgerCardGroup.selectedIndex = apps.indexOfFirst {
            it.pkg == PrefManager.bookApp
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
