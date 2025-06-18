package net.ankio.auto.ui.fragment.intro

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import net.ankio.auto.databinding.FragmentIntroPageAiBinding
import net.ankio.auto.http.api.AiAPI
import net.ankio.auto.ui.utils.LoadingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.ui.adapter.IntroPagerAdapter

class IntroPageAIFragment : BaseIntroPageFragment<FragmentIntroPageAiBinding>() {

    private suspend fun fetchProvidersWithRetry(): List<String> = withTimeoutOrNull(30_000) {
        var list: List<String>
        do {
            list = AiAPI.getProviders()
            if (list.isEmpty()) {
                // 列表为空，等 500ms 后重试
                delay(500)
            }
        } while (list.isEmpty())
        // 跳出循环时 list 已经非空，或超时抛 null
        list
    } ?: emptyList()  // 超时或返回 null 时，给个空列表

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始状态：禁用提供者、模型、刷新按钮
        binding.actAiProvider.isEnabled = false
        binding.actAiModel.isEnabled = false
        binding.btnRefreshModels.isEnabled = false

        // 监听Token输入
        binding.etAiToken.doOnTextChanged { text, _, _, _ ->
            val hasToken = !text.isNullOrBlank()
            binding.actAiProvider.isEnabled = hasToken
            binding.actAiModel.setText("")
            binding.actAiModel.isEnabled = hasToken
            binding.btnRefreshModels.isEnabled = hasToken
        }

        lifecycleScope.launch {
            val providerList = fetchProvidersWithRetry()
            binding.actAiProvider.bindItems(providerList)
            // 提供者选择后，启用刷新按钮
            binding.actAiProvider.setOnItemClickListener { _, _, pos, _ ->
                binding.actAiModel.setText("")
                binding.actAiModel.isEnabled = false
                binding.tilAiToken.error = null
            }

            // 刷新模型点击事件
            binding.btnRefreshModels.setOnClickListener {
                val token = binding.etAiToken.text?.toString()?.trim().orEmpty()
                if (token.isBlank()) {
                    binding.tilAiToken.error = getString(R.string.error_token_required)
                    return@setOnClickListener
                }

                // 加载模型
                lifecycleScope.launch {
                    AiAPI.setCurrentProvider(binding.actAiProvider.text.toString())
                    AiAPI.setApiKey(token)
                    val loader = LoadingUtils(requireActivity())
                    loader.show()
                    fetchModels()
                    loader.close()
                }
            }
        }

        binding.btnNext.setOnClickListener {
            // 完成
            PrefManager.aiFeatureOCR = binding.chipUsageOcr.isChecked
            PrefManager.aiFeatureAutoDetection = binding.chipUsageAutoDetect.isChecked
            PrefManager.aiFeatureCategory = binding.chipUsageAutoCategory.isChecked
            lifecycleScope.launch {
                AiAPI.setCurrentModel(binding.actAiModel.text.toString())
            }
            vm.pageRequest.value = IntroPagerAdapter.IntroPage.SUCCESS
        }

        binding.btnGetToken.setOnClickListener {


            val uri = runBlocking {
                AiAPI.getCreateKeyUri()
            }

            CustomTabsHelper.launchUrlOrCopy(requireContext(), uri)

        }

        binding.btnBack.setOnClickListener {

            if (AppAdapterManager.adapter().supportSyncAssets()) {
                vm.pageRequest.value = IntroPagerAdapter.IntroPage.SYNC
            } else {
                vm.pageRequest.value = IntroPagerAdapter.IntroPage.FEATURE
            }


        }
    }

    private suspend fun fetchModels() {
        val models = AiAPI.getModels()
        withContext(Dispatchers.Main) {
            binding.actAiModel.bindItems(models)
            binding.actAiModel.isEnabled = true
            // 选中模型后保存
            binding.actAiModel.setOnItemClickListener { _, _, pos, _ ->
                val model = models[pos]
                lifecycleScope.launch(Dispatchers.IO) { AiAPI.setCurrentModel(model) }
            }
        }
    }

    private fun MaterialAutoCompleteTextView.bindItems(items: List<String>) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, items)
        setAdapter(adapter)
    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            binding.chipUsageOcr.isChecked = PrefManager.aiFeatureOCR
            binding.chipUsageAutoDetect.isChecked = PrefManager.aiFeatureAutoDetection
            binding.chipUsageAutoCategory.isChecked = PrefManager.aiFeatureCategory
            runCatching {
                binding.actAiProvider.setText(AiAPI.getCurrentProvider())
                binding.actAiModel.setText(AiAPI.getCurrentModel())
                binding.etAiToken.setText(AiAPI.getApiKey())
            }
        }


    }
}
