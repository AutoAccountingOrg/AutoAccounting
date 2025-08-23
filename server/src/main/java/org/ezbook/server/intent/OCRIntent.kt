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

package org.ezbook.server.intent

import android.content.ComponentName
import android.content.Intent
import com.google.gson.Gson
import org.ezbook.server.Server
import org.ezbook.server.db.model.BillInfoModel

class OCRIntent : BaseIntent(IntentType.OCR) {

    companion object {
        fun parse(intent: Intent): OCRIntent {
            return OCRIntent()
        }
    }

    override fun toIntent(): Intent {
        val intent = super.toIntent()
        return intent
    }


}