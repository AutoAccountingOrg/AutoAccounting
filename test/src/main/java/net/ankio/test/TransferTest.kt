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
 * 信用卡还款测试（转账识别）
 *
 * 测试场景：使用储蓄卡还款信用卡
 * 预期结果：多条通知合并为1条 Transfer 类型账单
 */
class TransferTest : BaseTest() {
    override fun getConfig() = TestConfig(
        title = "信用卡还款测试（转账识别）",
        description = "使用交通银行储蓄卡还款招商银行信用卡 3895.55元",
        dataFile = "test_data_transfer.json",
        expectedResults = listOf(
            "3条账单合并为1条 Transfer",
            "accountNameFrom = 交通银行（转出账户）",
            "accountNameTo = 招商银行信用卡（转入账户）"
        ),
        verifyCommands = listOf(
            "adb logcat | grep -E 'TransferRecognizer|转账识别|转账处理'"
        )
    )
}

/**
 * 主函数
 */
suspend fun main() {
    try {
        TransferTest().runTest()
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

