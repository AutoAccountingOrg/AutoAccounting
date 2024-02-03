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
import android.graphics.Color
import android.os.Process
import androidx.annotation.AttrRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.MaterialColors
import com.quickersilver.themeengine.ThemeEngine
import net.ankio.auto.BuildConfig
import net.ankio.auto.ui.activity.LauncherActivity
import java.math.BigInteger
import java.security.MessageDigest


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
    /**
     * 获取主题色
     */
    fun getThemeAttrColor( @AttrRes attrResId: Int): Int {
        return MaterialColors.getColor(ContextThemeWrapper(application, ThemeEngine.getInstance(application).getTheme()), attrResId, Color.WHITE)
    }

    fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    fun getVersionName():String{
        return BuildConfig.VERSION_NAME
    }

    fun getApplicationId():String{
        return BuildConfig.APPLICATION_ID
    }

    fun md5(input:String):String{
        val md5Digest = MessageDigest.getInstance("MD5")
        val messageDigest = md5Digest.digest(input.toByteArray())
        val number = BigInteger(1, messageDigest)
        var md5Hash = number.toString(16)
        while (md5Hash.length < 32) {
            md5Hash = "0$md5Hash"
        }
        return md5Hash
    }


}