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

package net.ankio.auto.utils

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

class FileLogger(private val logFile: File) {

    private val maxFileSize = 1024 * 1024 // 1MB

    init {
        if (!logFile.exists()) {
            logFile.parentFile?.mkdirs()
            logFile.createNewFile()
        }
    }

    @Synchronized
    fun log(message: String) {
        val logMessage = "${System.currentTimeMillis()}: $message\n"
        RandomAccessFile(logFile, "rw").use { raf ->
            if (raf.length() + logMessage.length > maxFileSize) {
                val fileChannel: FileChannel = raf.channel
                val sizeToKeep = raf.length() / 2
                val positionToStart = raf.length() - sizeToKeep
                fileChannel.position(positionToStart)
                val buffer = ByteArray(sizeToKeep.toInt())
                fileChannel.read(java.nio.ByteBuffer.wrap(buffer))
                fileChannel.position(0)
                fileChannel.write(java.nio.ByteBuffer.wrap(buffer))
                fileChannel.truncate(sizeToKeep)
            }
            raf.seek(raf.length())
            raf.write(logMessage.toByteArray())
        }
    }
}