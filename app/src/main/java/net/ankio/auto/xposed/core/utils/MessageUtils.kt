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

package net.ankio.auto.xposed.core.utils

import android.widget.Toast
import com.hjq.toast.Toaster
import net.ankio.auto.xposed.core.App.Companion.TAG
import net.ankio.auto.xposed.core.App.Companion.application
import net.ankio.auto.xposed.core.logger.Logger

object MessageUtils {

    /**
     * 弹出提示
     * @param msg String
     */
    fun toast(msg: String) {
        if (application == null) {
            return
        }
        try {
            Toaster.show(msg)
        } catch (e: Throwable) {
            Toast.makeText(application, msg, Toast.LENGTH_LONG).show()
        }finally {
            Logger.log(TAG, msg)
        }


    }
}