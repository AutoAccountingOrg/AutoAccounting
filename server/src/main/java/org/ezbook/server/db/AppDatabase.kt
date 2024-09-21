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
package org.ezbook.server.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.ezbook.server.db.dao.AppDataDao
import org.ezbook.server.db.dao.AssetMapDao
import org.ezbook.server.db.dao.AssetsDao
import org.ezbook.server.db.dao.BillInfoDao
import org.ezbook.server.db.dao.BookBillDao
import org.ezbook.server.db.dao.BookNameDao
import org.ezbook.server.db.dao.CategoryDao
import org.ezbook.server.db.dao.CategoryMapDao
import org.ezbook.server.db.dao.CategoryRuleDao
import org.ezbook.server.db.dao.LogDao
import org.ezbook.server.db.dao.RuleDao
import org.ezbook.server.db.dao.SettingDao
import org.ezbook.server.db.model.AppDataModel
import org.ezbook.server.db.model.AssetsMapModel
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BookBillModel
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryMapModel
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.db.model.CategoryRuleModel
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.db.model.RuleModel
import org.ezbook.server.db.model.SettingModel

@Database(
    entities = [
        LogModel::class,
        RuleModel::class,
        SettingModel::class,
        AppDataModel::class,
        BillInfoModel::class,
        AssetsModel::class,
        BookNameModel::class,
        CategoryModel::class,
        AssetsMapModel::class,
        CategoryMapModel::class,
        CategoryRuleModel::class,
        BookBillModel::class
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun ruleDao(): RuleDao
    abstract fun settingDao(): SettingDao
    abstract fun dataDao(): AppDataDao
    abstract fun billInfoDao(): BillInfoDao
    abstract fun assetsDao(): AssetsDao
    abstract fun bookNameDao(): BookNameDao
    abstract fun categoryDao(): CategoryDao
    abstract fun assetsMapDao(): AssetMapDao
    abstract fun categoryMapDao(): CategoryMapDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun bookBillDao(): BookBillDao
}
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
    }
}
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 创建新的表 BillInfoModel_new，其中不包含 syncFromApp 字段，替换为 state 字段
        database.execSQL("""
            CREATE TABLE BillInfoModel_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                currency TEXT NOT NULL,
                money REAL NOT NULL,
                fee REAL NOT NULL,
                time INTEGER NOT NULL,
                shopName TEXT NOT NULL,
                shopItem TEXT NOT NULL,
                cateName TEXT NOT NULL,
                extendData TEXT NOT NULL,
                bookName TEXT NOT NULL,
                accountNameFrom TEXT NOT NULL,
                accountNameTo TEXT NOT NULL,
                app TEXT NOT NULL,
                groupId INTEGER NOT NULL,
                channel TEXT NOT NULL,
                state TEXT NOT NULL,    -- 新增的 state 字段
                remark TEXT NOT NULL,
                auto INTEGER NOT NULL,
                ruleName TEXT NOT NULL
            )
        """)

        // 2. 将旧表的数据迁移到新表，同时根据 syncFromApp 设置 state 字段
        database.execSQL("""
            INSERT INTO BillInfoModel_new (id, type, currency, money, fee, time, shopName, shopItem, cateName, extendData, bookName, accountNameFrom, accountNameTo, app, groupId, channel, state, remark, auto, ruleName)
            SELECT id, type, currency, money, fee, time, shopName, shopItem, cateName, extendData, bookName, accountNameFrom, accountNameTo, app, groupId, channel,
                CASE
                    WHEN syncFromApp = 1 THEN 'Synced'
                    ELSE 'Edited'
                END as state,
                remark, auto, ruleName
            FROM BillInfoModel
        """)

        // 3. 删除旧表
        database.execSQL("DROP TABLE BillInfoModel")

        // 4. 将新表重命名为旧表名
        database.execSQL("ALTER TABLE BillInfoModel_new RENAME TO BillInfoModel")

    }
}


val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 创建临时表
        database.execSQL("CREATE TABLE new_RuleModel (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, app TEXT NOT NULL, type TEXT NOT NULL, js TEXT NOT NULL, name TEXT NOT NULL, systemRuleName TEXT NOT NULL, creator TEXT NOT NULL, struct TEXT NOT NULL, autoRecord INTEGER NOT NULL, enabled INTEGER NOT NULL, updateAt INTEGER NOT NULL DEFAULT 0)")

        // 将旧数据迁移到新表
        database.execSQL("INSERT INTO new_RuleModel (id, app, type, js, name, systemRuleName, creator, struct, autoRecord, enabled) SELECT id, app, type, js, name, systemRuleName, creator, struct, autoRecord, enabled FROM RuleModel")

        // 删除旧表
        database.execSQL("DROP TABLE RuleModel")

        // 将新表重命名为旧表
        database.execSQL("ALTER TABLE new_RuleModel RENAME TO RuleModel")
    }
}
