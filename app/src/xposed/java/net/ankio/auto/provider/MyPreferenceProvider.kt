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

package net.ankio.auto.provider

import com.crossbowffs.remotepreferences.RemotePreferenceProvider
import net.ankio.auto.R
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger


class MyPreferenceProvider : RemotePreferenceProvider("net.ankio.preferences", arrayOf("main_prefs")){
    override fun checkAccess(prefFileName: String?, prefKey: String, write: Boolean): Boolean {
        //获取res数组
        AppUtils.getApplication().let {
            val resources = it.resources
            val myArray = resources.getStringArray(R.array.xposed_scope)
            Logger.i("prefFileName:$prefFileName,prefKey:$prefKey,write:$write")
            Logger.i("callingPackage:${callingPackage}")
            if(myArray.contains(callingPackage)){
                return true
            }
        }

        return false
    }
}