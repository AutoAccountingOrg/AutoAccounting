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
 * 美团订单去重测试
 *
 * 测试场景：多个数据源同时报告同一笔消费
 * 预期结果：去重为1个父账单
 */
class ConcurrentBillTest : BaseTest() {
    override fun getConfig() = TestConfig(
        title = "美团订单去重测试（7个数据源并发）",
        description = "交易金额 42.10元，招商银行信用卡 -> 微信支付 -> 美团",
        dataFile = "test_data_meituan.json",
        expectedResults = listOf(
            "请求成功: 7/7",
            "生成账单: 1个父账单 + N个子账单",
            "去重正确: 7条数据识别为同一笔交易"
        ),
        verifyCommands = listOf(
            "adb logcat | grep -E 'BillService|去重|parentId'"
        )
    )
}

/**
 * 主函数
 */
suspend fun main() {
    try {
        ConcurrentBillTest().runTest()
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
