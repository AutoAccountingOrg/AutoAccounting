package net.ankio.auto.ui.fragment.intro

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentIntroPage5Binding
import net.ankio.auto.models.AutoApp
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.components.ExpandableCardView
import net.ankio.auto.ui.vm.IntroSharedVm
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.isAppInstalled
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

class IntroPage5Fragment : BaseFragment<FragmentIntroPage5Binding>() {
    private val vm: IntroSharedVm by activityViewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        readApp()

        /*  binding.nex.setOnClickListener {
              vm.pageRequest.value = 1
          }*/

        binding.btnContinue.setOnClickListener {
            PrefManager.bookApp = apps[binding.ledgerCardGroup.selectedIndex].pkg
            //同步数据
            if (PrefManager.bookApp == BuildConfig.APPLICATION_ID) {
                vm.pageRequest.value = 6 //跳过数据同步
            } else {
                vm.pageRequest.value = 5 //执行数据同步
            }

        }


    }

    lateinit var apps:
            List<AutoApp>

    private fun readApp() {
        // 1. 读取 raw 目录下的文件
        val stream = resources.openRawResource(R.raw.app)
        val text = stream.bufferedReader().use { it.readText() }

// 2. 把 YAML 加载为 List<Map<String, Any>>
        // 1. 准备设置
        val settings = LoadSettings.builder().build()
// 2. 创建 loader
        val loader = Load(settings)
// 3. 加载
        @Suppress("UNCHECKED_CAST")
        val list = loader.loadFromString(text) as List<Map<String, Any>>

// 3. 转换为 AutoApp 对象
        apps = list.map { m ->
            AutoApp(
                name = m["name"] as String,
                icon = m["icon"] as String,
                pkg = m["package"] as String,
                website = m["website"] as String,
                description = m["description"] as String
            )
        }

        // 4. 遍历绑定到 UI
        apps.forEachIndexed { index, app ->
            val card = ExpandableCardView(requireContext()).apply {
                // icon 可能是 URL，也可能是本地资源名
                if (app.icon.startsWith("http")) {
                    Glide.with(this).load(app.icon).into(icon)
                } else {
                    icon.setImageResource(R.mipmap.ic_launcher_foreground)
                }
                setTitle(app.name)
                setDescription(app.description)
                setOnCardClickListener {
                    //判断App是否安装
                    if (!context.isAppInstalled(app.pkg)) {
                        CustomTabsHelper.launchUrl(context, Uri.parse(app.website))
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
        PrefManager.introIndex = 4
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
