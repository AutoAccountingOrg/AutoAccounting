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

package net.ankio.auto.storage

object Constants {
    // 10分钟
    val BACKUP_TIME = 10 * 60 * 1000L
    // 6小时
    val CHECK_INTERVAL = 6 * 60 * 60 * 1000L

    val DONATE_INTERVAL = 365L * 24 * 60 * 60 * 1000

    val INTENT_TIMEOUT = 1000L * 60 * 1
}