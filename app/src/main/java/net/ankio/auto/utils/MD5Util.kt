/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.utils

import java.math.BigInteger
import java.security.MessageDigest

object MD5Util {

    fun get(input: String): String {
        val md5Digest = MessageDigest.getInstance("MD5")
        val messageDigest = md5Digest.digest(input.toByteArray())
        val number = BigInteger(1, messageDigest)
        var md5Hash = number.toString(16)
        while (md5Hash.length < 32) {
            md5Hash = "0$md5Hash"
        }
        return md5Hash
    }
}