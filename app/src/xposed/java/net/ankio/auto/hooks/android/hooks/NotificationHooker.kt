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

package net.ankio.auto.hooks.android.hooks

import android.app.Application
import android.app.Notification
import com.google.gson.Gson
import com.google.gson.JsonObject
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import net.ankio.auto.hooks.android.utils.MD5HashTable
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.SettingModel


class NotificationHooker : PartHooker() {
    private var selectedApps = listOf<String>()
    private var lastTime = 0L
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        val notificationManagerService = XposedHelpers.findClass(
            "com.android.server.notification.NotificationManagerService",
            classLoader
        )
        val hashTable = MD5HashTable()

        XposedBridge.hookAllMethods(
            notificationManagerService,
            "enqueueNotificationInternal",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {


                    val app = param.args[0] as String
                    val opkg = param.args[1] as String

                    var notification: Notification? = null

                    for (i in 0 until param.args.size) {
                        if (param.args[i] is Notification) {
                            notification = param.args[i] as Notification
                            break
                        }
                    }

                    if (notification == null) {
                        return
                    }


                    val originalTitle =
                        notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
                    val originalText = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""

                    val hash = hashTable.md5("$app$originalTitle$originalText")
                    if (hashTable.contains(hash)){
                        return
                    }
                    hashTable.add(hash)
/*
                    hookerManifest.logD("Notification App: $opkg")
                    hookerManifest.logD("Notification App2: $app")
                    hookerManifest.logD("Notification Title: $originalTitle")
                    hookerManifest.logD("Notification Content: $originalText")*/


                    // 5分钟内不重复请求数据，加快识别速度
                    if (System.currentTimeMillis() - lastTime < 1000 * 60 * 5) {
                        checkNotification(
                            app,
                            originalTitle,
                            originalText,
                            selectedApps,
                            hookerManifest
                        )
                    } else {
                        lastTime = System.currentTimeMillis()
                        App.scope.launch {
                            val data = SettingModel.get(Setting.LISTENER_APP_LIST, "")
                            selectedApps = data.split(",")
                            withContext(Dispatchers.Main) {
                                checkNotification(
                                    app,
                                    originalTitle,
                                    originalText,
                                    selectedApps,
                                    hookerManifest
                                )
                            }
                        }
                    }


                }
            }
        )
    }

    /**
     * 检查通知
     */
    private fun checkNotification(
        pkg: String,
        title: String,
        text: String,
        selectedApps: List<String>,
        hookerManifest: HookerManifest
    ) {
        if (title.isEmpty() || text.isEmpty()) {
            hookerManifest.logD("Notification title or text is empty")
            return
        }

        if (!selectedApps.contains(pkg)) {
            hookerManifest.logD("Notification not in selected apps: $pkg, $selectedApps")
            return
        }


        val json = JsonObject()
        json.addProperty("title", title)
        json.addProperty("text", text)

        hookerManifest.analysisData(DataType.NOTICE, Gson().toJson(json), pkg)
    }



}