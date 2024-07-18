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

package net.ankio.auto.tests.tests

import net.ankio.auto.tests.TestObject
import net.ankio.auto.tests.iTest

class AppDataTest: iTest {
    override fun cases(): MutableList<TestObject>  = mutableListOf(
        TestObject(
            name = "新增",
            message = """
                {
                    "type":"data/add",
                    "data":{
                        "data":"这是App收到的数据",
                        "source":"net.ankio.auto.xposed",
                        "time": 1630848000000,
                        "type": 0,
                        "issue": 0,
                        "match":0,
                        "rule":"命中测试规则"
                    }
                }
            """.trimIndent(),
            expect = """
{
	"data" : 
	{
		"message" : "success",
		"status" : 0
	},
	"id" : "",
	"type" : "data/add"
}
            """.trimIndent()
        ),
        TestObject(
            name = "更新",
            message = """
                {
                    "type":"data/update",
                    "data":{
                        "data":"这是App收到的数据",
                        "source":"net.ankio.auto.xposed",
                        "time": 1630848000000,
                        "type": 0,
                        "issue": 0,
                        "match":0,
                        "rule":"命中测试规则"
                    }
                }
            """.trimIndent(),
            expect = """
{
	"data" : 
	{
		"message" : "success",
		"status" : 0
	},
	"id" : "",
	"type" : "data/add"
}
            """.trimIndent()
        ),
        TestObject(
            name = "删除",
            message = """
                {
                    "type":"data/del",
                    "data":{
                        "id":1
                    }
                }
            """.trimIndent(),
            expect = """
{
	"data" : 
	{
		"message" : "success",
		"status" : 0
	},
	"id" : "",
	"type" : "data/add"
}
            """.trimIndent()
        ),
        TestObject(
            name = "列表",
            message = """
                {
                    "type":"data/list",
                    "data":{
                        "page":1,
                        "size":5,
                        "match":-1,
                        "data":""
                    }
                }
            """.trimIndent(),
            expect = ""
        ),
        TestObject(
            name = "清空",
            message = """
                {
                    "type":"data/clear"
                }
            """.trimIndent(),
            expect = ""
        ),
    )
    override fun name(): String = "App数据测试"
}