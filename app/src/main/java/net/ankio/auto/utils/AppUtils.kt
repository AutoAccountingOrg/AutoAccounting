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

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Process
import android.text.TextUtils
import net.ankio.auto.ui.activity.LauncherActivity
import java.util.Locale


object  AppUtils {
    private lateinit var application: Application
    private lateinit var service: AutoAccountingServiceUtils

    fun getApplication(): Application {
        return application
    }

    fun setApplication(application: Application) {

        this.application = application
    }

    fun setService(){
        this.service = AutoAccountingServiceUtils(application)
    }

    fun getService():AutoAccountingServiceUtils{
        return service
    }

    fun getProcessName(): String {
        return Application.getProcessName()
    }

    /**
     * 重启应用
     */
    fun restart(){
        val intent = Intent(application, LauncherActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    /**
     * 复制到剪切板
     */
    fun copyToClipboard(text: String?) {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", text)
        clipboard.setPrimaryClip(clip)
    }

    fun getLocale(tag: String): Locale? {
        return if (TextUtils.isEmpty(tag) || "SYSTEM" == tag) {
            LocaleDelegate.systemLocale
        } else Locale.forLanguageTag(tag)
    }

    fun getLocale(): Locale? {
        val tag: String = SpUtils.getString("setting_language", "SYSTEM")
        return getLocale(tag)
    }


}