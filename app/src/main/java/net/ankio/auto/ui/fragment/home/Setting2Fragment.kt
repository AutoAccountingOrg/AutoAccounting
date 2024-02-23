/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.ui.fragment.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.quickersilver.themeengine.ThemeChooserDialogBuilder
import com.quickersilver.themeengine.ThemeEngine
import com.quickersilver.themeengine.ThemeMode
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentSetting2Binding
import net.ankio.auto.ui.fragment.BaseFragment
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.LanguageUtils
import net.ankio.auto.utils.ListPopupUtils
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.UpdateUtils


class Setting2Fragment:BaseFragment() {
    private lateinit var binding: FragmentSetting2Binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetting2Binding.inflate(layoutInflater)
        initView()
        return binding.root
    }

    private fun initView(){
        //匿名分析
        initCrash()
        //语言配置
        initLanguage()
        //主题
        initTheme()
        //更新
        initUpdate()
        //其他
        initOthers()

    }

    private fun initCrash(){
        SpUtils.getBoolean("sendToAppCenter",true).apply { binding.analyze.isChecked = this }
        binding.AnalyzeView.setOnClickListener {
            binding.analyze.isChecked = !binding.analyze.isChecked
            SpUtils.putBoolean("sendToAppCenter",binding.analyze.isChecked )
        }
        binding.analyze.setOnCheckedChangeListener { _, isChecked ->  SpUtils.putBoolean("sendToAppCenter",isChecked ) }
    }

    private fun initLanguage(){
        val languages: ArrayList<String> = LanguageUtils.getLangListName(requireContext())

        val langListArray = LanguageUtils.getLangList()


        binding.settingLangDesc.text = LanguageUtils.getAppLangName(requireContext())


        val listPopupWindow = ListPopupUtils(requireContext(), binding.settingLangDesc,languages){
            val value = langListArray[it]
            binding.settingLangDesc.text = languages[it]
            LanguageUtils.setAppLanguage(value)
            recreateInit()
        }

        binding.settingLang.setOnClickListener{ listPopupWindow.toggle() }
        //翻译
        binding.settingTranslate.setOnClickListener{
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.translation_url))
        }

    }

    private fun initTheme(){
        val color = ThemeEngine.getInstance(requireContext()).staticTheme.primaryColor

        binding.colorSwitch.setCardBackgroundColor(requireActivity().getColor(color))

        binding.settingTheme.setOnClickListener {
            ThemeChooserDialogBuilder(requireContext())
                .setTitle(R.string.choose_theme)
                .setPositiveButton(getString(R.string.ok)) { _, theme ->
                    ThemeEngine.getInstance(requireContext()).staticTheme = theme
                    recreateInit()
                }
                .setNegativeButton(getString(R.string.close))
                .setNeutralButton(getString(R.string.default_theme)) { _, _ ->
                    ThemeEngine.getInstance(requireContext()).resetTheme()
                    recreateInit()
                }
                .setIcon(R.drawable.ic_theme)
                .create()
                .show()
        }

        val stringList = arrayListOf(getString(R.string.always_off),getString(R.string.always_on),getString(R.string.lang_follow_system))

        binding.settingDarkTheme.text = when(ThemeEngine.getInstance(requireContext()).themeMode){
            ThemeMode.DARK -> stringList[1]
            ThemeMode.LIGHT -> stringList[0]
            else  -> stringList[2]
        }

        val listPopupThemeWindow = ListPopupUtils(requireContext(), binding.settingDarkTheme,stringList){
            binding.settingDarkTheme.text = stringList[it]
            ThemeEngine.getInstance(requireContext()).themeMode = when(it){
                1 -> ThemeMode.DARK
                0 -> ThemeMode.LIGHT
                else -> ThemeMode.AUTO
            }
        }

        binding.settingDark.setOnClickListener{ listPopupThemeWindow.toggle() }


        binding.alwaysDark.isChecked = ThemeEngine.getInstance(requireContext()).isTrueBlack
        binding.settingUseDarkTheme.setOnClickListener {
            ThemeEngine.getInstance(requireContext()).isTrueBlack = !binding.alwaysDark.isChecked
            binding.alwaysDark.isChecked = !binding.alwaysDark.isChecked
        }
        binding.alwaysDark.setOnCheckedChangeListener { _, isChecked ->
            ThemeEngine.getInstance(requireContext()).isTrueBlack =  isChecked
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S){
            binding.settingUseSystemTheme.visibility = View.GONE
        }

        fun isUseSystemColor(systemColor:Boolean){
            if (systemColor){
                binding.settingTheme.visibility = View.GONE
            }else{
                binding.settingTheme.visibility = View.VISIBLE
            }
        }

        binding.systemColor.isChecked = ThemeEngine.getInstance(requireContext()).isDynamicTheme
        isUseSystemColor(binding.systemColor.isChecked)



        binding.settingUseSystemTheme.setOnClickListener {
            binding.systemColor.isChecked = !binding.systemColor.isChecked
        }
        binding.systemColor.setOnCheckedChangeListener { _, isChecked ->
            isUseSystemColor(binding.systemColor.isChecked)
            ThemeEngine.getInstance(requireContext()).isDynamicTheme = isChecked
            recreateInit()
        }
        //圆角弹窗，不需要重建Activity
        SpUtils.getBoolean("cardRound",false).apply {
            binding.roundStyle.isChecked = this
        }

        binding.settingUseRoundStyle.setOnClickListener {
            binding.roundStyle.isChecked = !binding.roundStyle.isChecked
        }

        binding.roundStyle.setOnCheckedChangeListener{_,isChecked ->
            SpUtils.putBoolean("cardRound",isChecked)
        }
    }

    private fun initOthers(){

        AppUtils.getDebug().apply {
            binding.systemDebug.isChecked = this
        }

        binding.settingDebug.setOnClickListener {
            binding.systemDebug.isChecked = !binding.systemDebug.isChecked
        }
        binding.systemDebug.setOnCheckedChangeListener { _, isChecked ->
            AppUtils.setDebug(isChecked)
        }
    }

    /**
     * 更新部分
     */
    private fun initUpdate() {

        //url

        UpdateUtils.getUrl().apply {
            binding.url.setText(this)
        }

        binding.url.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                UpdateUtils.setUrl(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        //更新类型
        val stringList = arrayListOf(getString(R.string.stable_version),
          getString(R.string.continuous_build_version))

        UpdateUtils.getUpdateType().apply {
            binding.settingUpdateTypeDesc.text = if (this) stringList[1] else stringList[0]
        }

        binding.settingUpdateType.setOnClickListener {
            val listPopupWindow = ListPopupUtils(requireContext(), binding.settingUpdateTypeDesc,stringList){
                binding.settingUpdateTypeDesc.text = stringList[it]
                UpdateUtils.setUpdateType(it == 1)
            }
            listPopupWindow.toggle()
        }

        //检查更新

        SpUtils.getBoolean("checkApp",true).apply {
            binding.systemApp.isChecked = this
        }

        binding.settingApp.setOnClickListener {
            binding.systemApp.isChecked = !binding.systemApp.isChecked
        }

        binding.systemApp.setOnCheckedChangeListener{_,isChecked ->
            SpUtils.putBoolean("checkApp",isChecked)
        }


        //检查Rule

        SpUtils.getBoolean("checkRule",true).apply {
            binding.systemRule.isChecked = this
        }

        binding.settingRule.setOnClickListener {
            binding.systemRule.isChecked = !binding.systemRule.isChecked
        }

        binding.systemRule.setOnCheckedChangeListener{_,isChecked ->
            SpUtils.putBoolean("checkRule",isChecked)
        }
    }
    /**
     * 页面重新初始化
     */
    private fun recreateInit() {
        activity?.recreate()
    }




}