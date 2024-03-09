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
import androidx.core.os.ConfigurationCompat
import androidx.core.text.HtmlCompat
import net.ankio.auto.R
import net.ankio.utils.LangList
import java.util.Locale


object LanguageUtils {
     fun initAppLanguage(context: Context): Context {
        val language = getAppLang()
         val locale = getLocale(language)
         Logger.i("App语言：$language 实际获取的语言：${locale.language}")
        Locale.setDefault(locale)
        return updateResourcesLocale(context, locale)
    }

    fun getAppLang(): String {
        return SpUtils.getString("setting_language","SYSTEM")
    }

    fun getAppLangName(context: Context): String {
        val language = getAppLang()
        return if (language == "SYSTEM") context.getString(R.string.lang_follow_system)
        else Locale.forLanguageTag(language).displayName
    }

    fun setAppLanguage(language: String){
        SpUtils.putString("setting_language",language)
    }

    fun getLangList(context: Context):HashMap<String,Any>{
        val hashMap = HashMap<String,Any>()
        getLangList().forEach {
            if (it == "SYSTEM") {
                hashMap[context.getString(R.string.lang_follow_system)] = it
            }else{
                val locale = Locale.forLanguageTag(it)
                hashMap[locale.getDisplayName(locale)] =  it
            }
        }
        return hashMap
    }
    fun getLangListName(context: Context): ArrayList<String> {
        val languages: ArrayList<String> = ArrayList()
        getLangList().forEach {
            if (it == "SYSTEM") {
                languages.add(context.getString(R.string.lang_follow_system))
            }else{
                val locale = Locale.forLanguageTag(it)
                languages.add(
                    HtmlCompat.fromHtml(locale.getDisplayName(locale), HtmlCompat.FROM_HTML_MODE_LEGACY)
                        .toString())
            }
        }
        return languages
    }
    fun getLangList(): Array<String> {
       return LangList.LOCALES.plus("zh")
    }

    fun getLocale(language: String): Locale {
        return if(language == "SYSTEM") getSystemLocale() else Locale(language)
    }

    private fun updateResourcesLocale(context: Context, locale: Locale): Context {
        val configuration: Configuration = context.resources.configuration
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    private fun getSystemLocale(): Locale {
        return ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)
            ?: Locale.CHINA
    }
}