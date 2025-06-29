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

package net.ankio.auto.http.license

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.App
import net.ankio.auto.utils.PrefManager
import java.io.File

object AppAPI {
    suspend fun lastVer(): JsonObject? {
        val result = App.licenseNetwork.get(
            "/app/latest", hashMapOf(
                "channel" to PrefManager.appChannel
            )
        )

        return runCatching {
            return Gson().fromJson(result, JsonObject::class.java)
        }.getOrNull()
    }

    suspend fun download(version: String, file: File): Boolean {
        return App.licenseNetwork.download(
            "/app/download/", file, hashMapOf(
                "channel" to PrefManager.appChannel,
                "version" to version
            )
        )
    }
}