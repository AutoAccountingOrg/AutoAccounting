/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.fragment.settings

import android.net.Uri
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import net.ankio.auto.R
import net.ankio.auto.ui.api.BasePreferenceFragment
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.SystemUtils

/**
 * 关于应用设置页面
 * 包含：应用信息、交流社区、作者信息
 */
class AboutPreferenceFragment : BasePreferenceFragment() {

    override fun getTitleRes(): Int = R.string.setting_title_about

    override fun getPreferencesRes(): Int = R.xml.settings_about

    override fun createDataStore(): PreferenceDataStore = AboutDataStore()

    /**
     * 设置偏好项点击事件
     */
    override fun setupPreferences() {
        super.setupPreferences()

        // 应用信息
        setPreferenceClickListener("openSourceCode") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_open_source_code_url))
        }

        setPreferenceClickListener("officialWebsite") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_official_website_url))
        }

        // 激活码：跳转到查询页面
        setPreferenceClickListener("activationCode") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_activation_code_url))
        }

        setPreferenceClickListener("updateLog") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_update_log_url))
        }

        // 交流社区
        setPreferenceClickListener("forum") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_forum_url))
        }

        setPreferenceClickListener("qqGroup") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_qq_group_url))
        }

        setPreferenceClickListener("telegram") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_telegram_url))
        }

        // 作者信息
        setPreferenceClickListener("authorBlog") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_author_blog_url))
        }

        setPreferenceClickListener("authorGithub") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_author_github_url))
        }

        setPreferenceClickListener("authorContact") {
            // 联系方式：打开邮箱客户端
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = getString(R.string.setting_author_contact_url).toUri()
            }
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            }
        }

        // 酷安：跳转到酷安主页
        setPreferenceClickListener("authorCoolapk") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_author_coolapk_url))
        }

        // 微信公众号：复制公众号名称到剪贴板
        setPreferenceClickListener("authorWechat") {
            val wechatName = getString(R.string.setting_author_wechat_name)
            SystemUtils.copyToClipboard(wechatName)
            ToastUtils.info(getString(R.string.copy_command_success))
        }

        // 哔哩哔哩：跳转到B站主页
        setPreferenceClickListener("authorBilibili") {
            CustomTabsHelper.launchUrlOrCopy(getString(R.string.setting_author_bilibili_url))
        }
    }

    /**
     * 设置preference点击监听器
     */
    private fun setPreferenceClickListener(key: String, action: () -> Unit) {
        findPreference<Preference>(key)?.apply {
            // 确保 Preference 可点击
            isSelectable = true
            setOnPreferenceClickListener {
                action()
                true
            }
        }
    }

    /**
     * 关于应用数据存储类
     */
    class AboutDataStore : PreferenceDataStore() {
        // 关于应用页面暂时没有设置项，数据存储类为空实现
    }
}

