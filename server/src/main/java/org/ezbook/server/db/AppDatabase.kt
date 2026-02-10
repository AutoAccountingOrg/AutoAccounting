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
import org.ezbook.server.db.dao.AnalysisTaskDao
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
import org.ezbook.server.db.dao.TagDao
import org.ezbook.server.db.model.AnalysisTaskModel
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
import org.ezbook.server.db.model.TagModel

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
        BookBillModel::class,
        TagModel::class,
        AnalysisTaskModel::class
    ],
    version = 20,
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
    abstract fun tagDao(): TagDao
    abstract fun analysisTaskDao(): AnalysisTaskDao
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
    }
}
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 创建新的表 BillInfoModel_new，其中不包含 syncFromApp 字段，替换为 state 字段
        database.execSQL(
            """
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
        """
        )

        // 2. 将旧表的数据迁移到新表，同时根据 syncFromApp 设置 state 字段
        database.execSQL(
            """
            INSERT INTO BillInfoModel_new (id, type, currency, money, fee, time, shopName, shopItem, cateName, extendData, bookName, accountNameFrom, accountNameTo, app, groupId, channel, state, remark, auto, ruleName)
            SELECT id, type, currency, money, fee, time, shopName, shopItem, cateName, extendData, bookName, accountNameFrom, accountNameTo, app, groupId, channel,
                CASE
                    WHEN syncFromApp = 1 THEN 'Synced'
                    ELSE 'Edited'
                END as state,
                remark, auto, ruleName
            FROM BillInfoModel
        """
        )

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

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 创建一个新的临时表
        database.execSQL(
            """
            CREATE TABLE new_AppDataModel (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                data TEXT NOT NULL,
                type TEXT NOT NULL,
                app TEXT NOT NULL,
                time INTEGER NOT NULL,
                `match` INTEGER NOT NULL,
                rule TEXT NOT NULL,
                issue INTEGER NOT NULL,
                version TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent()
        )

        // 2. 将旧表中的数据迁移到新表
        database.execSQL(
            """
            INSERT INTO new_AppDataModel (id, data, type, app, time, `match`, rule, issue)
            SELECT id, data, type, app, time, `match`, rule, issue FROM AppDataModel
        """.trimIndent()
        )

        // 3. 删除旧表
        database.execSQL("DROP TABLE AppDataModel")

        // 4. 将临时表重命名为旧表名
        database.execSQL("ALTER TABLE new_AppDataModel RENAME TO AppDataModel")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // BookBill表添加type字段
        database.execSQL("ALTER TABLE BookBillModel ADD COLUMN type TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 创建标签表
        database.execSQL(
            """
            CREATE TABLE TagModel (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL DEFAULT '#2196F3',
                UNIQUE(name)
            )
        """.trimIndent()
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 为BillInfoModel表添加tags字段
        database.execSQL("ALTER TABLE BillInfoModel ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 为TagModel表添加group字段，支持标签分组
        database.execSQL("ALTER TABLE TagModel ADD COLUMN `group` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 修复AssetsType枚举问题：将所有FINANCIAL类型的资产转换为NORMAL类型
        // 这是因为FINANCIAL枚举值已被移除，但数据库中可能仍有使用该值的记录
        database.execSQL("UPDATE AssetsModel SET type = 'NORMAL' WHERE type = 'FINANCIAL'")

        // 同样处理可能存在的VIRTUAL类型（也被注释掉了）
        database.execSQL("UPDATE AssetsModel SET type = 'NORMAL' WHERE type = 'VIRTUAL'")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 采用“新表拷贝 + 聚合插入 + 重命名 + 建索引”的安全迁移策略
        // 1) 创建临时表（无唯一约束）
        database.execSQL(
            """
            CREATE TABLE new_SettingModel (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `key` TEXT NOT NULL,
                `value` TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 2) 将每个 key 的最新记录（按最大 id）插入到新表
        database.execSQL(
            """
            INSERT INTO new_SettingModel (id, `key`, `value`)
            SELECT s.id, s.`key`, s.`value`
            FROM SettingModel s
            WHERE s.id IN (
                SELECT MAX(id) FROM SettingModel GROUP BY `key`
            )
            """.trimIndent()
        )

        // 3) 删除旧表
        database.execSQL("DROP TABLE SettingModel")

        // 4) 临时表重命名为正式表
        database.execSQL("ALTER TABLE new_SettingModel RENAME TO SettingModel")

        // 5) 补建与实体匹配的唯一索引（Room 校验期望该索引名）
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_SettingModel_key ON SettingModel(`key`)"
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 采用“新表拷贝 + 聚合插入 + 重命名 + 建索引”的安全迁移策略
        // 1) 创建临时表（保持与实体一致的列定义）
        database.execSQL(
            """
            CREATE TABLE new_AssetsMapModel (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                regex INTEGER NOT NULL DEFAULT 0,
                name TEXT NOT NULL,
                mapName TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 2) 将每个 name 的最新记录（按最大 id）插入到新表
        database.execSQL(
            """
            INSERT INTO new_AssetsMapModel (id, regex, name, mapName)
            SELECT a.id, a.regex, a.name, a.mapName
            FROM AssetsMapModel a
            WHERE a.id IN (
                SELECT MAX(id) FROM AssetsMapModel GROUP BY name
            )
            """.trimIndent()
        )

        // 3) 删除旧表
        database.execSQL("DROP TABLE AssetsMapModel")

        // 4) 临时表重命名为正式表
        database.execSQL("ALTER TABLE new_AssetsMapModel RENAME TO AssetsMapModel")

        // 5) 为 name 建立唯一索引（与实体索引名匹配）
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_AssetsMapModel_name ON AssetsMapModel(name)"
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 为 BookNameModel 添加唯一索引（name），在此之前进行去重，保留每个 name 的最新记录
        database.execSQL(
            """
            CREATE TABLE new_BookNameModel (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                name TEXT NOT NULL,
                icon TEXT NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO new_BookNameModel (id, remoteId, name, icon)
            SELECT b.id, b.remoteId, b.name, b.icon
            FROM BookNameModel b
            WHERE b.id IN (
                SELECT MAX(id) FROM BookNameModel GROUP BY name
            )
            """.trimIndent()
        )

        database.execSQL("DROP TABLE BookNameModel")
        database.execSQL("ALTER TABLE new_BookNameModel RENAME TO BookNameModel")

        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_BookNameModel_name ON BookNameModel(name)"
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 创建AI分析任务表
        database.execSQL(
            """
            CREATE TABLE AnalysisTaskModel (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL,
                status TEXT NOT NULL,
                createTime INTEGER NOT NULL,
                updateTime INTEGER NOT NULL,
                resultHtml TEXT,
                errorMessage TEXT,
                progress INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 为 AssetsMapModel 添加 sort 字段，用于支持拖拽排序
        database.execSQL("ALTER TABLE AssetsMapModel ADD COLUMN sort INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // TagModel 移除 color 字段，采用新表迁移
        database.execSQL(
            """
            CREATE TABLE TagModel_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                `group` TEXT NOT NULL DEFAULT '',
                UNIQUE(name)
            )
            """.trimIndent()
        )
        // 迁移时按 name 去重，保留最大 id 的标签，避免 UNIQUE(name) 冲突。
        database.execSQL(
            """
            INSERT INTO TagModel_new (id, name, `group`)
            SELECT t.id, t.name, t.`group`
            FROM TagModel t
            INNER JOIN (
                SELECT name, MAX(id) AS max_id
                FROM TagModel
                GROUP BY name
            ) latest ON latest.name = t.name AND latest.max_id = t.id
            """.trimIndent()
        )
        database.execSQL("DROP TABLE TagModel")
        database.execSQL("ALTER TABLE TagModel_new RENAME TO TagModel")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // BookBillModel 添加 flag 字段，用于“不计收支/不计预算”标志位
        database.execSQL("ALTER TABLE BookBillModel ADD COLUMN flag INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // BillInfoModel 添加 flag 字段，用于"不计收支/不计预算"标志位
        database.execSQL("ALTER TABLE BillInfoModel ADD COLUMN flag INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // BillInfoModel 添加原始账户名字段，用于"记住资产映射"功能
        database.execSQL("ALTER TABLE BillInfoModel ADD COLUMN rawAccountNameFrom TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE BillInfoModel ADD COLUMN rawAccountNameTo TEXT NOT NULL DEFAULT ''")
    }
}
