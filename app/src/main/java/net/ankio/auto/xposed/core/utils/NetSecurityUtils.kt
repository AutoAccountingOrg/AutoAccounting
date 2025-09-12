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

package net.ankio.auto.xposed.core.utils

import android.security.NetworkSecurityPolicy
import de.robv.android.xposed.XposedHelpers

/**
 * NetSecurityUtils
 * 调整网络安全策略（仅在运行时需要时）。
 */
object NetSecurityUtils {
    fun allowCleartextTraffic() {
        val policy = NetworkSecurityPolicy.getInstance()
        if (policy != null && !policy.isCleartextTrafficPermitted) {
            XposedHelpers.callMethod(policy, "setCleartextTrafficPermitted", true)
        }
    }
}


