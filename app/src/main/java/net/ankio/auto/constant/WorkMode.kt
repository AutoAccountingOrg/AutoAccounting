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

package net.ankio.auto.constant

import net.ankio.auto.utils.PrefManager

enum class WorkMode {
    Xposed,
    LSPatch,
    Ocr;


    companion object {
        fun isXposed() = PrefManager.workMode == WorkMode.Xposed

        /**
         * 当前是否为 LSPatch 工作模式。
         */
        fun isLSPatch() = PrefManager.workMode == WorkMode.LSPatch

        /**
         * 当前是否为 OCR 工作模式。
         */
        fun isOcr() = PrefManager.workMode == WorkMode.Ocr

        fun isOcrOrLSPatch() = isOcr() or isLSPatch()

        fun isXposedOrLSPatch() = isXposed() or isLSPatch()
    }

}