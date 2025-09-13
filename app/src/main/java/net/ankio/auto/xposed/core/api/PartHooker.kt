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

package net.ankio.auto.xposed.core.api

import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AnalysisUtils
import net.ankio.auto.xposed.core.utils.AppRuntime
import org.ezbook.server.constant.DataType

abstract class PartHooker {


    protected val manifest by lazy { AppRuntime.manifest }


    fun log(msg: String) {
        Logger.log(manifest.packageName, msg)
    }

    fun logD(msg: String) {
        Logger.logD(manifest.packageName, msg)
    }

    fun logE(e: Throwable) {
        Logger.logE(manifest.packageName, e)
    }

    fun analysisData(type: DataType, data: String) {
        AnalysisUtils.analysisData(manifest.packageName, type, data)
    }

    open fun hook() {}

}