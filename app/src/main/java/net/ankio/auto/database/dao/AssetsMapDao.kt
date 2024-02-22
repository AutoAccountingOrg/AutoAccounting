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
package net.ankio.auto.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import net.ankio.auto.database.table.AssetsMap
import net.ankio.auto.utils.Logger

@Dao
interface AssetsMapDao {
    @Query("SELECT * FROM AssetsMap")
    suspend fun loadAll(): List<AssetsMap>
    @Insert
    suspend fun insert(accountMap: AssetsMap):Long
    @Update
    suspend fun update(accountMap: AssetsMap)
    @Delete
    suspend fun delete(accountMap: AssetsMap)
    @Query("DELETE FROM AssetsMap")
    suspend fun deleteAll()
    @Query("SELECT * FROM AssetsMap where regex=:regex")
    suspend fun getRegex(regex: Boolean): List<AssetsMap>

    /**
     * 提供原始账户名，获取映射的账户名
     * @param name 原始账户名
     * @return 映射的账户名
     */
    suspend fun getMapName(name:String):String{
        getRegex(true).forEach {
            runCatching {
                //如果原始账户名符合正则表达式，则返回映射的账户名
                if (name.matches(Regex(it.name))) {
                    return it.mapName
                }
            }.onFailure {
                Logger.e("正则匹配出错",it)
            }
        }

        val map= getRegex(false).find { it.name==name }
        return map?.mapName?:name
    }



}
