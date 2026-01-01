/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.test

/**
 * 转账目标账户测试
 *
 * 测试场景：Income + Expend 合并时 accountNameTo 是否正确设置
 * 预期结果：accountNameFrom 和 accountNameTo 都正确
 */
class TransferAccountTest : BaseTest() {
    override fun getConfig() = TestConfig(
        title = "转账目标账户测试",
        description = "验证 Income + Expend 合并时 accountNameTo 是否正确设置",
        dataFile = "test_data_transfer_account.json",
        expectedResults = listOf(
            "合并后 type = Transfer",
            "accountNameFrom = 转出账户",
            "accountNameTo = 转入账户"
        ),
        verifyCommands = listOf(
            "adb logcat | grep -E 'TransferRecognizer|规范化账户位置|accountTo'"
        )
    )
}

/**
 * 主函数
 */
suspend fun main() {
    try {
        TransferAccountTest().runTest()
        println()
        println("测试完成！")
    } catch (e: Exception) {
        println()
        println("!".repeat(80))
        println("测试执行失败: ${e.message}")
        println("!".repeat(80))
        e.printStackTrace()
    }
}

