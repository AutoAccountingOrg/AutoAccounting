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
 * 账户合并Bug测试
 *
 * 测试场景：验证通用值（银行卡）不覆盖已知资产（交通银行（工资））
 * 预期结果：accountNameFrom 保持为已知资产名称
 */
class MergeAccountTest : BaseTest() {
    override fun getConfig() = TestConfig(
        title = "账户合并Bug测试",
        description = "验证通用值（银行卡）不应覆盖已知资产（交通银行（工资））",
        dataFile = "test_data_merge_account.json",
        expectedResults = listOf(
            "accountNameFrom = 交通银行（工资）（已知资产优先）",
            "accountNameTo = 招商银行信用卡",
            "银行卡（通用值）不覆盖已知资产"
        ),
        verifyCommands = listOf(
            "adb logcat | grep -E 'BillMerger|selectBetterAccount|accountFrom'"
        )
    )
}

/**
 * 主函数
 */
suspend fun main() {
    try {
        MergeAccountTest().runTest()
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

