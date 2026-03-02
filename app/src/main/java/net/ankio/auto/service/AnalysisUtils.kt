/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package net.ankio.auto.service

import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager

object AnalysisUtils {
    fun inWhitelist(data: String): Boolean {
        val whitelist = PrefManager.dataFilter
            .lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (whitelist.none { data.contains(it) }) {
            Logger.d("AnalysisUtils: whitelist filter, no keyword matched")
            return false
        }
        return true
    }

    fun inBlackList(data: String): Boolean {
        val blacklist = PrefManager.dataFilterBlacklist
            .lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (blacklist.any { data.contains(it) }) {
            Logger.d("AnalysisUtils: blacklist filter matched")
            return true
        }
        return false
    }
}

