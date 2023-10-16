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

package net.ankio.auto.hooks.android

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.ankio.auto.database.table.AccountMap
import net.ankio.auto.database.table.AppData
import net.ankio.auto.utils.toContentValues
import net.ankio.auto.utils.toObjects

class DbHelper(context: Context,dbPath: String) : SQLiteOpenHelper(context, dbPath, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        val tableList = arrayListOf(
            """
                CREATE TABLE AccountMap (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                     regex INTEGER NOT NULL,
                     name TEXT,
                     mapName TEXT
                     );
            """.trimIndent(),
            """
               CREATE  TABLE  AppData  (   
     id  INTEGER  PRIMARY  KEY  AUTOINCREMENT,   
     data  TEXT,   
      type  INTEGER,   
     source  TEXT,   
      time  INTEGER,   
     match  INTEGER,   
     rule  TEXT,   
     issue  INTEGER   
);   
            """.trimIndent()
        )
        for (table in tableList){
            db.execSQL(table)
        }
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }
    val db = writableDatabase

    fun insert(table:String,data:Any):Long{
        return db.insert(table, null, data.toContentValues())
    }
    fun update(table:String,data:Any){

        val values = data.toContentValues()

        db.update(table, values, "id = ?", arrayOf(values.getAsInteger("id").toString()))

    }
    fun delete(table:String,data:Any){
        val values = data.toContentValues()
        db.delete(table, "id = ?", arrayOf(values.getAsInteger("id").toString()))
    }

    fun getTotalAppData():List<AppData>{

        val cursor: Cursor = db.query("AppData", null, null, null, null, null, null)

        val appData = cursor.toObjects<AppData>()
        cursor.close()

        return appData
    }
    fun getTotalAccountMap():List<AccountMap>{

        val cursor: Cursor = db.query("AccountMap", null, null, null, null, null, null)

        val appData = cursor.toObjects<AccountMap>()
        cursor.close()

        return appData
    }

    fun empty(table: String) {
        db.execSQL("DELETE  FROM  $table")
    }

}