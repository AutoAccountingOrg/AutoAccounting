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

package net.ankio.auto.adapter

import net.ankio.auto.constant.WorkMode
import net.ankio.auto.utils.PrefManager

object AppAdapterManager {

    //是否使用OCR模式
    fun ocrMode(): Boolean = PrefManager.workMode === WorkMode.Ocr

    //是否使用Xposed模式
    fun xposedMode(): Boolean = PrefManager.workMode === WorkMode.Xposed


    fun adapterList(): List<IAppAdapter> {
        return listOf(
            AutoAdapter(), //自动记账
            QianJiAdapter() //钱迹
        )
    }

    //获取记账软件适配器
    fun adapter(): IAppAdapter {
        return adapterList().firstOrNull { it.pkg == PrefManager.bookApp } ?: AutoAdapter()
    }
}