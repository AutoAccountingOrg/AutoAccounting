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

import android.content.Context
import android.content.pm.ApplicationInfo
import net.ankio.auto.constant.DataType
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.AppData
import net.ankio.auto.database.table.Category


object Mock {
     suspend fun init(context: Context){
        if(!isDebugBuild(context))return
        if(SpUtils.getBoolean("mock_init2",false))return
        SpUtils.putBoolean("mock_init2",true)

        var data = AppData()
        data.data  = "{'id':'测试'}"
        data.from = "net.ankio.auto.xposed"
        data.time = 1691570381
        data.type = DataType.App

        Db.get().AppDataDao().add(data)

        data.data  = "{'id':'测试'}"
        data.from = "188888181818"
        data.time = 1691570381
        data.type = DataType.Sms

        Db.get().AppDataDao().add(data)

        data.data  = "{'id':'测试'}"
        data.from = "net.ankio.auto.xposed"
        data.time = 1691570381
        data.match = true
        data.rule = "测试规则"
        data.type = DataType.Notice

        Db.get().AppDataDao().add(data)

        data.data  = "{'id':'测试'}"
        data.from = "net.ankio.auto.xposed"
        data.time = 1691570381
        data.type = DataType.Helper

        Db.get().AppDataDao().add(data)

        var cate = Category()


        cate.name = "测试1"
        cate.parent = -1
        cate.book = 1
        Db.get().CategoryDao().add(cate)

        cate.name = "测试2"
        cate.parent = 1
        Db.get().CategoryDao().add(cate)

        cate.name = "测试3"
        cate.parent = 1
        Db.get().CategoryDao().add(cate)


        cate.name = "测试4"
        cate.parent = -1
        Db.get().CategoryDao().add(cate)


        cate.name = "测试5"
        Db.get().CategoryDao().add(cate)


        cate.name = "测试7"
        Db.get().CategoryDao().add(cate)


        cate.name = "测试5"
        Db.get().CategoryDao().add(cate)


        cate.name = "测试7"
        Db.get().CategoryDao().add(cate)



        cate.name = "测试5"
        Db.get().CategoryDao().add(cate)


        cate.name = "测试7"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)



        cate.name = "测试5"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)


        cate.name = "测试7"
        cate.parent = -1
        Db.get().CategoryDao().add(cate)


        cate.name = "测试5"
        Db.get().CategoryDao().add(cate)


        cate.name = "测试7"
        Db.get().CategoryDao().add(cate)


        cate.name = "测试7"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)



        cate.name = "测试5"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)


        cate.name = "测试7"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)



        cate.name = "测试5"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)

        cate.name = "测试7"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)



        cate.name = "测试5"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)

        cate.name = "测试7"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)



        cate.name = "测试5"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)

        cate.name = "测试7"
        cate.parent = -1
        Db.get().CategoryDao().add(cate)



        cate.name = "测试5"
        cate.parent = 4
        Db.get().CategoryDao().add(cate)

        //添加账本测试用例


    }
     fun isDebugBuild(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
}
