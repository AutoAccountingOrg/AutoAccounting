/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.hooks.qianji.hooks

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import de.robv.android.xposed.XposedBridge
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.logger.XposedLogger
import net.ankio.auto.xposed.hooks.qianji.helper.BillDbHelper
import net.ankio.auto.xposed.hooks.qianji.models.QjBillModel


class DbHooker : PartHooker() {


    override fun hook() {
        // 拦截 getWritableDatabase，获取数据库实例并初始化索引表
        Hooker.after("android.database.sqlite.SQLiteOpenHelper", "getWritableDatabase") {
            targetDb = it.result as SQLiteDatabase?
            initTable()
        }
    }

    /**
     * 初始化 $tableName 索引表。
     * bill_id 为主键（不可重复），auto_id 记录自动记账 ID。
     */
    private fun initTable() {
        safeExecuteSql(
            """
            CREATE TABLE IF NOT EXISTS $tableName (
                bill_id INTEGER NOT NULL UNIQUE,
                auto_id INTEGER NOT NULL UNIQUE
            )
            """.trimIndent()
        )
    }

    companion object {
        /** 钱迹应用的可写数据库实例 */
        var targetDb: SQLiteDatabase? = null

        var tableName = "auto_accounting_index2"

        /**
         * 将账单与自动记账关联。
         * 若 auto_id 已关联且账单仍存在，直接返回已关联的 bill_id；
         * 若已关联但账单已不存在，删除旧记录后重新插入；
         * 无关联时直接插入，返回 null。
         */
        fun link2Auto(bill_id: Long, auto_id: Int): QjBillModel? {
            val db = targetDb ?: return null
            if (!db.isOpen) return null
            return try {
                // 查询 auto_id 是否已有关联
                val existingBillId = db.rawQuery(
                    "SELECT bill_id FROM $tableName WHERE auto_id = ?",
                    arrayOf(auto_id.toString())
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else null
                }
                if (existingBillId != null) {
                    // 账单仍存在，直接返回
                    val bill = BillDbHelper.newInstance().findByBillId(existingBillId)

                    if (bill != null) {
                        return bill
                    }
                    // 账单已不存在，清除过期记录
                    db.execSQL(
                        "DELETE FROM $tableName WHERE auto_id = ?",
                        arrayOf(auto_id)
                    )
                }
                // 插入新关联
                db.execSQL(
                    "INSERT OR REPLACE INTO $tableName (bill_id, auto_id) VALUES (?, ?)",
                    arrayOf(bill_id, auto_id)
                )
                null
            } catch (e: Exception) {
                XposedBridge.log("关联失败: ${e.message}")
                null
            }
        }

        /**
         * 通过自动记账 ID 查找对应的钱迹账单。
         */
        fun convert2Bill(auto_id: Int): QjBillModel? {
            val db = targetDb ?: return null
            if (!db.isOpen) return null
            val billId = try {
                db.rawQuery(
                    "SELECT bill_id FROM $tableName WHERE auto_id = ?",
                    arrayOf(auto_id.toString())
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else null
                }
            } catch (e: Exception) {
                XposedBridge.log("查询失败: ${e.message}")
                null
            } ?: return null
            return BillDbHelper.newInstance().findByBillId(billId)
        }
    }


    /**
     * 安全执行 SQL 语句，数据库未就绪时静默跳过。
     */
    fun safeExecuteSql(sql: String?) {
        val db = targetDb ?: return
        if (!db.isOpen) return
        try {
            db.execSQL(sql)
        } catch (e: Exception) {
            XposedBridge.log("执行失败: ${e.message}")
        }
    }
}