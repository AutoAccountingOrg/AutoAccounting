/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * You may not use this file except in compliance with the License.
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
package org.ezbook.server.db

import android.content.Context
import androidx.room.Room.databaseBuilder
import java.io.File
import java.io.IOException

object Db {
    private const val DATABASE_NAME = "autoAccount.db"
    private lateinit var db: AppDatabase

    /**
     * 初始化数据库
     */
    fun init(context: Context) {
        context.let {
            db = databaseBuilder(
                it,
                AppDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration().build()
        }
    }

    /**
     * 获取数据库实例
     */
    fun get(): AppDatabase {
        if (!this::db.isInitialized) {
            throw IllegalStateException("Database not initialized. Call init() first.")
        }
        return db
    }

    /**
     * 复制数据库文件
     */
    fun copy(context: Context): File {
        db.close()

        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val dbNewFile = File(dbFile.parent, "autoAccount_new.db")

        try {
            // 确保父目录存在
            dbNewFile.parentFile?.apply { if (!exists()) mkdirs() }
            // 复制数据库文件
            dbFile.copyTo(dbNewFile, overwrite = true)
            println("Database copied to: ${dbNewFile.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error copying database: ${e.message}")
        } finally {
            init(context)
        }
        return dbNewFile
    }

    fun import(context: Context, file: File) {
        db.close()
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        try {
            // 确保父目录存在
            dbFile.parentFile?.apply { if (!exists()) mkdirs() }
            // 复制数据库文件
            file.copyTo(dbFile, overwrite = true)
            println("Database copied to: ${dbFile.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error copying database: ${e.message}")
        } finally {
            init(context)
        }

    }
}
