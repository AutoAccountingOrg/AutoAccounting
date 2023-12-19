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

package net.ankio.auto

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.quickersilver.themeengine.ThemeEngine
import net.ankio.auto.database.Db


open class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
    
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        //主题初始化
        ThemeEngine.applyToActivities(this)
        //数据库初始化
        Db.init(this)
    }
}