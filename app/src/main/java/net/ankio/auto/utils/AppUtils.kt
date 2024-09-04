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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Process
import androidx.annotation.AttrRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.color.MaterialColors
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.quickersilver.themeengine.ThemeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.app.model.AppInfo
import net.ankio.auto.storage.SpUtils
import net.ankio.auto.ui.activity.MainActivity
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

object AppUtils {
    private lateinit var application: Application
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun getJob(): Job {
        return job
    }

    fun getScope(): CoroutineScope {
        return scope
    }

    fun getApplication(): Application {
        return App.app
    }

    fun setApplication(application: Application) {
        this.application = application
    }

    fun getProcessName(): String {
        return Application.getProcessName()
    }

    fun runOnUiThread(action: () -> Unit) {
        App.app.mainExecutor.execute(action)
    }

    /**
     * 重启应用
     */
    fun restart() {
        val intent = Intent(App.app, MainActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        App.app.startActivity(intent)
        Process.killProcess(Process.myPid())
    }





    fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    fun getVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    fun getApplicationId(): String {
        return BuildConfig.APPLICATION_ID
    }

    fun md5(input: String): String {
        val md5Digest = MessageDigest.getInstance("MD5")
        val messageDigest = md5Digest.digest(input.toByteArray())
        val number = BigInteger(1, messageDigest)
        var md5Hash = number.toString(16)
        while (md5Hash.length < 32) {
            md5Hash = "0$md5Hash"
        }
        return md5Hash
    }

    fun getAppInfoFromPackageName(
        packageName: String,
        context: Context,
    ): AppInfo? {
        try {
            val packageManager: PackageManager = context.packageManager

            val app: ApplicationInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0),
                    )
                } else {
                    context.packageManager
                        .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                }

            val appName = packageManager.getApplicationLabel(app).toString()

            val appIcon =
                try {
                    val resources: Resources =
                        context.packageManager.getResourcesForApplication(app.packageName)
                    ResourcesCompat.getDrawable(resources, app.icon, context.theme)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    null
                }

            val packageInfo: PackageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0),
                    )
                } else {
                    context.packageManager
                        .getPackageInfo(packageName, PackageManager.GET_META_DATA)
                }
            val appVersion = packageInfo.versionName
            return AppInfo(appName, appIcon, appVersion)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取debug状态
     */
    fun getDebug(): Boolean {
        return BuildConfig.DEBUG || SpUtils.getBoolean("debug", false)
    }

    /**
     * 设置debug状态
     */
    fun setDebug(debug: Boolean = false) {
        SpUtils.putBoolean("debug", debug)
       // SpUtils.putBooleanRemote("debug", debug)
    }

    /**
     * 获取屏幕宽度
     */
    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    fun dp2px(dp: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    fun measureTextWidth(
        text: String,
        textSize: Float,
        typeface: Typeface,
    ): Int {
        val paint = Paint()
        paint.textSize = textSize
        paint.typeface = typeface
        return paint.measureText(text).toInt()
    }

    /**
     * 打开记账软件应用
     */
    fun startBookApp() {
        val packageName = SpUtils.getString("bookApp", "")
        val launchIntent = App.app.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            App.app.startActivity(launchIntent)
        }
    }

    fun toPrettyFormat(jsonString: String): String {
        runCatching {
            val json = JsonParser.parseString(jsonString)
            val gson = GsonBuilder().setPrettyPrinting().create()
            val prettyJson = gson.toJson(json)
            return prettyJson
        }
        return jsonString
    }

    fun readTail(
        file: File,
        numLines: Int,
    ): String {
        if (!file.exists())return ""
        return file.readLines().takeLast(numLines).joinToString("\n")
    }
}
