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
import org.ezbook.server.db.dao.AppDataDao
import org.ezbook.server.db.dao.AssetMapDao
import org.ezbook.server.db.dao.AssetsDao
import org.ezbook.server.db.dao.BillInfoDao
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
import org.ezbook.server.db.model.LogModel
import org.ezbook.server.db.model.RuleModel
import org.ezbook.server.db.model.SettingModel
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryMapModel
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.db.model.CategoryRuleModel

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
    CategoryRuleModel::class
               ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun ruleDao(): RuleDao
    abstract fun settingDao(): SettingDao
    abstract fun dataDao():AppDataDao
    abstract fun billInfoDao(): BillInfoDao
    abstract fun assetsDao(): AssetsDao
    abstract fun bookNameDao(): BookNameDao
    abstract fun categoryDao(): CategoryDao
    abstract fun assetsMapDao(): AssetMapDao
    abstract fun categoryMapDao(): CategoryMapDao
    abstract fun categoryRuleDao(): CategoryRuleDao
}
