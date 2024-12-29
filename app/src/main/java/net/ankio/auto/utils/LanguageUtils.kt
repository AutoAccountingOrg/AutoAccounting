/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.XmlResourceParser
import androidx.core.os.ConfigurationCompat
import net.ankio.auto.R
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import org.ezbook.server.constant.Setting
import java.util.Locale

object LanguageUtils {
    fun initAppLanguage(context: Context): Context {
        val language = getAppLang()
        val locale = getLocale(language)
        Logger.i("App Language: $language => ${locale.language}")
        Locale.setDefault(locale)
        return updateResourcesLocale(context, locale)
    }

    fun getAppLang(): String {
        return ConfigUtils.getString(Setting.SYSTEM_LANGUAGE, "SYSTEM")
    }

    fun setAppLanguage(language: String) {
        ConfigUtils.putString(Setting.SYSTEM_LANGUAGE, language)
    }

    fun getLangList(context: Context): HashMap<String, Any> {
        val hashMap = HashMap<String, Any>()
        getAllLangList(context).forEach {
            if (it == "SYSTEM") {
                hashMap[context.getString(R.string.lang_follow_system)] = it
            } else {
                val locale = Locale.forLanguageTag(it)
                hashMap[locale.getDisplayName(locale)] = it
            }
        }
        return hashMap
    }

    private fun getAllLangList(context: Context): Array<String> {
        val langList = mutableListOf<String>()

        // 打开 XML 文件
        val parser = context.resources.getXml(R.xml.locale_config)

        try {
            var eventType = parser.eventType

            // 遍历 XML
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG && parser.name == "locale") {
                    // 获取 android:name 属性值
                    val name = parser.getAttributeValue(
                        "http://schemas.android.com/apk/res/android",
                        "name"
                    )
                    if (name != null) {
                        langList.add(name)
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            parser.close()
        }

        // 返回语言列表
        return langList.toTypedArray()
    }


    private fun getLocale(language: String): Locale {
        return if (language == "SYSTEM") getSystemLocale() else Locale(language)
    }

    private fun updateResourcesLocale(
        context: Context,
        locale: Locale,
    ): Context {
        val configuration: Configuration = context.resources.configuration
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    private fun getSystemLocale(): Locale {
        return ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)
            ?: Locale.CHINA
    }
}
