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

import android.app.Activity
import android.content.res.Configuration
import android.text.TextUtils
import net.ankio.auto.ui.activity.BaseActivity
import java.util.Locale

class LocaleDelegate {

    /** locale of this instance  */
    private var locale = Locale.getDefault()

    /**
     * Return if current locale is different from default.
     *
     * Call this in [Activity.onResume] and if true you should recreate activity.
     *
     * @return locale changed
     */
    val isLocaleChanged: Boolean
        get() = defaultLocale != locale

    /**
     * Update locale of given configuration, call in [Activity.attachBaseContext].
     *
     * @param configuration Configuration
     */
    fun updateConfiguration(configuration: Configuration) {
        locale = defaultLocale

        configuration.setLocale(locale)
    }

    /**
     * A dirty fix for wrong layout direction after switching locale between LTR and RLT language,
     * call in [Activity.onCreate].
     *
     * @param activity Activity
     */
    fun onCreate(activity: Activity) {
        activity.window.decorView.layoutDirection = TextUtils.getLayoutDirectionFromLocale(locale)
    }

    companion object {

        /** current locale  */
        @JvmStatic
        var defaultLocale: Locale? = Locale.getDefault()

        /** system locale  */
        @JvmStatic
        var systemLocale: Locale? = Locale.getDefault()

        var changedList = HashMap<Class<BaseActivity>,Boolean>()
    }
}