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

class LoginTest:iTest {
    override fun cases(): MutableList<TestObject>  = mutableListOf(
        TestObject(
            name = "登录",
            message = """
        {
            "type":"login/login",
            "data":{
                "app":"net.ankio.auto.xposed",
                "token":"9086124753"
            }
        }
    """.trimIndent(),
            expect = """
                {
	"data" : 
	{
		"msg" : "login success",
		"status" : 0
	},
	"id" : "",
	"type" : "login/login"
} 
""".trimIndent()
        )
    )

    override fun name(): String = "登录测试"

}