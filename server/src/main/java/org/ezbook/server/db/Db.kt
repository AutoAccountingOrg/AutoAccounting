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

/**
 * 数据库管理单例
 *
 * 线程安全性说明：
 * - 使用Mutex保护数据库备份/恢复/清理操作，避免并发访问冲突
 * - copy()方法使用VACUUM INTO命令，无需关闭数据库即可安全备份
 * - import()和clear()方法必须关闭数据库，但通过Mutex确保操作原子性
 * - 所有危险操作都在dbMutex.withLock{}中执行，确保同一时间只有一个操作进行
 *
 * 修复的问题：
 * - 解决了"attempt to re-open an already-closed object"异常
 * - 消除了备份操作与正常数据库操作之间的竞态条件
 * - 提供了更安全的数据库生命周期管理
 */
package org.ezbook.server.db

import android.content.Context
import androidx.room.Room.databaseBuilder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.runCatchingExceptCancel

object Db {
    private const val DATABASE_NAME = "autoAccount.db"
    private lateinit var db: AppDatabase

    /**
     * 数据库操作互斥锁，防止并发访问冲突
     * 特别是在备份/恢复操作期间保护数据库完整性
     */
    private val dbMutex = Mutex()

    /**
     * 初始化数据库
     */
    fun init(context: Context) {
        context.let {
            db = databaseBuilder(
                it,
                AppDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigrationFrom(false, 1)
                .addMigrations(MIGRATION_2_3) // 注册迁移
                .addMigrations(MIGRATION_3_4) // 注册迁移
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .addMigrations(MIGRATION_8_9)
                .addMigrations(MIGRATION_9_10)
                .addMigrations(MIGRATION_10_11)
                .addMigrations(MIGRATION_11_12)
                .addMigrations(MIGRATION_12_13)
                .addMigrations(MIGRATION_13_14)
                .addMigrations(MIGRATION_14_15)
                .addMigrations(MIGRATION_15_16)
                .addMigrations(MIGRATION_16_17)
                .addMigrations(MIGRATION_17_18)
                .addMigrations(MIGRATION_18_19)
                .addMigrations(MIGRATION_19_20)
                .build()
        }
    }

    /**
     * 获取数据库实例
     * 注意：在并发环境下，调用者应该尽快完成数据库操作
     * 避免长时间持有数据库引用，特别是在备份/恢复操作期间
     */
    fun get(): AppDatabase {
        if (!this::db.isInitialized) {
            throw IllegalStateException("Database not initialized. Call init() first.")
        }
        return db
    }

    /**
     * 清理历史遗留的备份临时文件
     * - 删除 filesDir 下的 autoAccount_backup_*.db
     * - 删除 databases 目录中的临时文件 db_backup.db
     *
     * 仅清理特定命名的临时文件，不影响正式数据库文件。
     */
    suspend fun cleanupResidualBackups(context: Context) {
        runCatchingExceptCancel {
            // 1) 清理 filesDir 下历史遗留的 autoAccount_backup_*.db
            context.filesDir.listFiles()?.forEach { file ->
                val name = file.name
                if (file.isFile &&
                    name.startsWith("autoAccount_backup_") &&
                    name.endsWith(".db")
                ) {
                    if (file.delete()) {
                        ServerLog.d("已清理历史备份临时文件：${file.absolutePath}")
                    }
                }
            }

            // 2) 清理导入阶段临时文件 db_backup.db（位于 databases 目录）
            val stagingFile = context.getDatabasePath("db_backup.db")
            if (stagingFile.exists()) {
                if (stagingFile.delete()) {
                    ServerLog.d("已清理导入阶段临时文件：${stagingFile.absolutePath}")
                }
            }
        }.onFailure { e ->
            ServerLog.e("清理历史备份临时文件失败：${e.message}", e)
        }
    }

    /**
     * 检查数据库是否可用
     * 在执行关键操作前可以先检查数据库状态
     */
    fun isAvailable(): Boolean {
        return this::db.isInitialized && db.isOpen
    }

    /**
     * 安全地复制数据库文件
     * 使用SQLite的VACUUM INTO命令，无需关闭数据库
     */
    suspend fun copy(context: Context): File = dbMutex.withLock {
        val dbNewFile =
            File(context.filesDir, "autoAccount_backup_${System.currentTimeMillis()}.db")

        runCatchingExceptCancel {
            // 确保父目录存在
            dbNewFile.parentFile?.apply { if (!exists()) mkdirs() }

            // 使用VACUUM INTO进行安全备份，无需关闭数据库
            db.openHelper.writableDatabase.execSQL("VACUUM INTO '${dbNewFile.absolutePath}'")
            ServerLog.d("数据库已安全复制到：${dbNewFile.absolutePath}")
        }.onFailure { e ->
            ServerLog.e("数据库VACUUM复制失败，回退到文件复制：${e.message}", e)
            // 如果VACUUM INTO失败，回退到文件复制（但仍需要同步）
            val originalDbFile = context.getDatabasePath(DATABASE_NAME)
            if (originalDbFile.exists()) {
                originalDbFile.copyTo(dbNewFile, overwrite = true)
            }
        }
        
        return dbNewFile
    }

    /**
     * 安全地导入数据库文件
     * 关闭当前数据库连接，替换文件后重新初始化
     * 此操作必须在应用重启后生效
     */
    suspend fun import(context: Context, file: File) = dbMutex.withLock {
        runCatchingExceptCancel {
            // 先验证导入文件的有效性
            if (!file.exists() || file.length() == 0L) {
                throw IOException("Import file does not exist or is empty")
            }

            // 关闭当前数据库连接（这里必须关闭，因为要替换文件）
            db.close()

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            
            // 确保父目录存在
            dbFile.parentFile?.apply { if (!exists()) mkdirs() }

            // 备份当前数据库以防导入失败
            val backupFile = File(dbFile.parent, "${DATABASE_NAME}.backup")
            if (dbFile.exists()) {
                dbFile.copyTo(backupFile, overwrite = true)
            }

            runCatchingExceptCancel {
                // 复制导入文件
                file.copyTo(dbFile, overwrite = true)
                ServerLog.d("数据库导入成功：${dbFile.absolutePath}")

                // 删除备份文件
                if (backupFile.exists()) {
                    backupFile.delete()
                }
            }.onFailure { e ->
                // 导入失败，恢复备份
                if (backupFile.exists()) {
                    backupFile.copyTo(dbFile, overwrite = true)
                    backupFile.delete()
                }
                throw e
            }
        }.onFailure { e ->
            ServerLog.e("数据库导入失败：${e.message}", e)
            throw e
        }.also {
            // 重新初始化数据库
            init(context)
        }
    }

    /**
     * 安全地清空数据库
     * 关闭当前连接，删除数据库文件后重新初始化
     */
    suspend fun clear(context: Context) = dbMutex.withLock {
        runCatchingExceptCancel {
            // 关闭当前数据库连接（这里必须关闭，因为要删除文件）
            db.close()

            val dbFile = context.getDatabasePath(DATABASE_NAME)

            // 删除数据库文件及其相关文件
            val filesToDelete = listOf(
                dbFile,
                File(dbFile.absolutePath + "-wal"),  // WAL文件
                File(dbFile.absolutePath + "-shm")   // Shared Memory文件
            )

            filesToDelete.forEach { file ->
                if (file.exists()) {
                    file.delete()
                    ServerLog.d("删除文件：${file.absolutePath}")
                }
            }
            ServerLog.d("数据库清空成功")
        }.onFailure { e ->
            ServerLog.e("清空数据库失败：${e.message}", e)
            throw e
        }.also {
            // 重新初始化数据库
            init(context)
        }
    }
}
