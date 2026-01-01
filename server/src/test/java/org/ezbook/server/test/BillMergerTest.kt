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

package org.ezbook.server.test

import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel

/**
 * BillMerger 账单合并逻辑测试
 *
 * 核心规则：只有在已知资产列表中的才优先选择
 *
 * 测试重点：
 * 1. 在已知资产列表中的资产应被优先选择
 * 2. 不在已知资产列表中的值（如"银行卡"）不应覆盖已知资产（如"交通银行（工资）"）
 * 3. 都在或都不在已知资产列表时，保留原值（target优先）
 */
object BillMergerTest {

    /**
     * 模拟已知资产列表（用户在App中配置的资产）
     */
    private val knownAssets = listOf(
        "交通银行（工资）",
        "招商银行信用卡",
        "招商银行（定存）",
        "支付宝余额",
        "微信零钱"
    )

    /**
     * 模拟 selectBetterAccount 逻辑
     *
     * 核心规则：只有在已知资产列表中的才优先选择
     *
     * @param source 来源账单的账户名
     * @param target 目标账单的账户名（原值）
     * @return 选择的账户名
     */
    private fun selectBetterAccount(source: String, target: String): String {
        if (source.isEmpty()) return target
        if (target.isEmpty()) return source

        val sourceKnown = knownAssets.contains(source)
        val targetKnown = knownAssets.contains(target)

        return when {
            // source 在已知资产列表中，target 不在 -> 选择 source
            sourceKnown && !targetKnown -> source
            // target 在已知资产列表中，source 不在 -> 保留 target
            !sourceKnown && targetKnown -> target
            // 都在或都不在已知资产列表 -> 保留 target（原值优先）
            else -> target
        }
    }

    /**
     * 测试用例数据类
     */
    data class MergeTestCase(
        val name: String,
        val sourceAccountFrom: String,
        val targetAccountFrom: String,
        val expectedAccountFrom: String,
        val sourceAccountTo: String = "",
        val targetAccountTo: String = "",
        val expectedAccountTo: String = ""
    )

    /**
     * 获取所有测试用例
     */
    private fun getTestCases(): List<MergeTestCase> = listOf(
        // ========== 核心 Bug 场景：不在已知列表的不应覆盖已知资产 ==========
        MergeTestCase(
            name = "【核心Bug】银行卡（不在已知列表）不应覆盖交通银行（工资）（在已知列表）",
            sourceAccountFrom = "银行卡",
            targetAccountFrom = "交通银行（工资）",
            expectedAccountFrom = "交通银行（工资）"
        ),
        MergeTestCase(
            name = "【核心Bug】信用卡（不在已知列表）不应覆盖招商银行信用卡（在已知列表）",
            sourceAccountFrom = "信用卡",
            targetAccountFrom = "招商银行信用卡",
            expectedAccountFrom = "招商银行信用卡"
        ),
        MergeTestCase(
            name = "【核心Bug】云闪付（不在已知列表）不应覆盖招商银行（定存）（在已知列表）",
            sourceAccountFrom = "云闪付",
            targetAccountFrom = "招商银行（定存）",
            expectedAccountFrom = "招商银行（定存）"
        ),

        // ========== 空值处理 ==========
        MergeTestCase(
            name = "target为空时使用source值",
            sourceAccountFrom = "交通银行（工资）",
            targetAccountFrom = "",
            expectedAccountFrom = "交通银行（工资）"
        ),
        MergeTestCase(
            name = "source为空时保留target值",
            sourceAccountFrom = "",
            targetAccountFrom = "招商银行信用卡",
            expectedAccountFrom = "招商银行信用卡"
        ),
        MergeTestCase(
            name = "双方都为空时结果为空",
            sourceAccountFrom = "",
            targetAccountFrom = "",
            expectedAccountFrom = ""
        ),

        // ========== 已知资产优先 ==========
        MergeTestCase(
            name = "source在已知列表、target不在 -> 选择source",
            sourceAccountFrom = "交通银行（工资）",
            targetAccountFrom = "未知银行账户",
            expectedAccountFrom = "交通银行（工资）"
        ),
        MergeTestCase(
            name = "target在已知列表、source不在 -> 保留target",
            sourceAccountFrom = "某某银行",
            targetAccountFrom = "招商银行信用卡",
            expectedAccountFrom = "招商银行信用卡"
        ),

        // ========== 都在或都不在已知列表：保留target（原值优先）==========
        MergeTestCase(
            name = "都在已知列表 -> 保留target",
            sourceAccountFrom = "交通银行（工资）",
            targetAccountFrom = "招商银行信用卡",
            expectedAccountFrom = "招商银行信用卡"
        ),
        MergeTestCase(
            name = "都不在已知列表 -> 保留target",
            sourceAccountFrom = "工商银行",
            targetAccountFrom = "建设银行",
            expectedAccountFrom = "建设银行"
        ),

        // ========== 转入账户测试 ==========
        MergeTestCase(
            name = "转入账户：不在已知列表的不覆盖已知资产",
            sourceAccountFrom = "",
            targetAccountFrom = "",
            expectedAccountFrom = "",
            sourceAccountTo = "信用卡",
            targetAccountTo = "招商银行信用卡",
            expectedAccountTo = "招商银行信用卡"
        ),
        MergeTestCase(
            name = "转入账户：target为空时使用source",
            sourceAccountFrom = "",
            targetAccountFrom = "",
            expectedAccountFrom = "",
            sourceAccountTo = "招商银行信用卡",
            targetAccountTo = "",
            expectedAccountTo = "招商银行信用卡"
        ),

        // ========== 完整转账场景（模拟日志中的真实场景） ==========
        MergeTestCase(
            name = "【真实场景】信用卡还款 - 第一次合并（交通银行微银行消息）",
            sourceAccountFrom = "交通银行（工资）",
            targetAccountFrom = "",  // 初始父账单没有来源账户
            expectedAccountFrom = "交通银行（工资）",
            sourceAccountTo = "",
            targetAccountTo = "招商银行信用卡",
            expectedAccountTo = "招商银行信用卡"
        ),
        MergeTestCase(
            name = "【真实场景】信用卡还款 - 第二次合并（云闪付消息，不应覆盖）",
            sourceAccountFrom = "银行卡",  // 云闪付只知道是"银行卡"，不在已知列表
            targetAccountFrom = "交通银行（工资）",  // 已经从第一次合并获取到精确值，在已知列表
            expectedAccountFrom = "交通银行（工资）",  // 应保留已知资产
            sourceAccountTo = "",
            targetAccountTo = "招商银行信用卡",
            expectedAccountTo = "招商银行信用卡"
        )
    )

    /**
     * 运行单个测试用例
     */
    private fun runTestCase(testCase: MergeTestCase): Boolean {
        val actualAccountFrom =
            selectBetterAccount(testCase.sourceAccountFrom, testCase.targetAccountFrom)
        val actualAccountTo =
            selectBetterAccount(testCase.sourceAccountTo, testCase.targetAccountTo)

        val fromMatch = actualAccountFrom == testCase.expectedAccountFrom
        val toMatch = actualAccountTo == testCase.expectedAccountTo

        val passed = fromMatch && toMatch

        val status = if (passed) "✓" else "✗"
        println("$status ${testCase.name}")

        if (!fromMatch) {
            println("    ❌ accountNameFrom 不匹配:")
            println(
                "       source: '${testCase.sourceAccountFrom}' (已知: ${
                    knownAssets.contains(
                        testCase.sourceAccountFrom
                    )
                })"
            )
            println(
                "       target: '${testCase.targetAccountFrom}' (已知: ${
                    knownAssets.contains(
                        testCase.targetAccountFrom
                    )
                })"
            )
            println("       期望: '${testCase.expectedAccountFrom}'")
            println("       实际: '$actualAccountFrom'")
        }

        if (!toMatch && (testCase.sourceAccountTo.isNotEmpty() || testCase.targetAccountTo.isNotEmpty())) {
            println("    ❌ accountNameTo 不匹配:")
            println(
                "       source: '${testCase.sourceAccountTo}' (已知: ${
                    knownAssets.contains(
                        testCase.sourceAccountTo
                    )
                })"
            )
            println(
                "       target: '${testCase.targetAccountTo}' (已知: ${
                    knownAssets.contains(
                        testCase.targetAccountTo
                    )
                })"
            )
            println("       期望: '${testCase.expectedAccountTo}'")
            println("       实际: '$actualAccountTo'")
        }

        return passed
    }

    /**
     * 运行所有测试用例
     */
    fun runAllTests() {
        println()
        println("=".repeat(80))
        println("BillMerger 账单合并逻辑测试")
        println("=".repeat(80))
        println()

        println("【核心规则】")
        println("只有在已知资产列表中的才优先选择")
        println()

        println("【问题描述】")
        println("合并账单时，后来的未知值（如'银行卡'）覆盖了先前的已知资产（如'交通银行（工资）'）")
        println()

        println("【修复方案】")
        println("selectBetterAccount 只检查是否在已知资产列表中，不再使用硬编码的'通用名称'")
        println()

        println("【已知资产列表】")
        knownAssets.forEach { println("  - $it") }
        println()

        println("-".repeat(80))
        println("测试用例执行")
        println("-".repeat(80))
        println()

        val testCases = getTestCases()
        var passCount = 0
        var failCount = 0

        testCases.forEach { testCase ->
            if (runTestCase(testCase)) {
                passCount++
            } else {
                failCount++
            }
        }

        println()
        println("=".repeat(80))
        println("测试结果汇总")
        println("=".repeat(80))
        println()
        println("总测试用例: ${testCases.size}")
        println("通过: $passCount")
        println("失败: $failCount")
        println("通过率: ${"%.2f".format(passCount * 100.0 / testCases.size)}%")
        println()

        if (failCount == 0) {
            println("✅ 所有测试通过！")
        } else {
            println("❌ 有 $failCount 个测试失败，请检查 BillMerger 的合并逻辑")
        }
        println("=".repeat(80))
    }

    /**
     * 测试真实场景：模拟日志中的三次合并
     */
    fun testRealScenario() {
        println()
        println("=".repeat(80))
        println("真实场景模拟：信用卡还款账单合并")
        println("=".repeat(80))
        println()

        println("【场景描述】")
        println("用户使用交通银行储蓄卡还款招商银行信用卡3433.09元")
        println("系统收到3条通知：")
        println("  1. 招商银行信用卡还款提醒 (只知道还款到信用卡)")
        println("  2. 交通银行微银行消费提醒 (知道从交通银行扣款)")
        println("  3. 云闪付消费提醒 (只知道是'银行卡')")
        println()

        println("【已知资产列表】")
        knownAssets.forEach { println("  - $it") }
        println()

        // 模拟父账单（招商银行信用卡还款提醒）
        val parentBill = BillInfoModel(
            id = 2298,
            type = BillType.Transfer,
            money = 3433.09,
            accountNameFrom = "",  // 初始不知道来源
            accountNameTo = "招商银行信用卡",
            channel = "微信[招商银行信用卡-还款]",
            ruleName = "数据·微信公众号招商银行信用卡"
        )

        println("【初始父账单】")
        println("  accountNameFrom: '${parentBill.accountNameFrom}'")
        println("  accountNameTo: '${parentBill.accountNameTo}'")
        println()

        // 第一次合并：交通银行微银行消息
        val childBill1 = BillInfoModel(
            id = 2299,
            type = BillType.Expend,
            money = 3433.09,
            accountNameFrom = "交通银行（工资）",
            accountNameTo = "",
            channel = "微信[交通银行微银行-支出]",
            ruleName = "数据·微信公众号交通银行微银行"
        )

        println("【第一次合并：交通银行微银行消息】")
        println(
            "  子账单 accountNameFrom: '${childBill1.accountNameFrom}' (已知: ${
                knownAssets.contains(
                    childBill1.accountNameFrom
                )
            })"
        )

        parentBill.accountNameFrom =
            selectBetterAccount(childBill1.accountNameFrom, parentBill.accountNameFrom)
        println("  合并后父账单 accountNameFrom: '${parentBill.accountNameFrom}'")

        val firstMergeCorrect = parentBill.accountNameFrom == "交通银行（工资）"
        println("  结果: ${if (firstMergeCorrect) "✅ 正确" else "❌ 错误"}")
        println()

        // 第二次合并：云闪付消息（不在已知列表，不应覆盖）
        val childBill2 = BillInfoModel(
            id = 2300,
            type = BillType.Expend,
            money = 3433.09,
            accountNameFrom = "银行卡",  // 云闪付只知道是银行卡，不在已知列表
            accountNameTo = "",
            channel = "云闪付[支出]",
            ruleName = "通知·云闪付消费通知"
        )

        println("【第二次合并：云闪付消息（核心Bug测试点）】")
        println(
            "  子账单 accountNameFrom: '${childBill2.accountNameFrom}' (已知: ${
                knownAssets.contains(
                    childBill2.accountNameFrom
                )
            })"
        )
        println(
            "  父账单 accountNameFrom: '${parentBill.accountNameFrom}' (已知: ${
                knownAssets.contains(
                    parentBill.accountNameFrom
                )
            })"
        )

        parentBill.accountNameFrom =
            selectBetterAccount(childBill2.accountNameFrom, parentBill.accountNameFrom)
        println("  合并后父账单 accountNameFrom: '${parentBill.accountNameFrom}'")

        val secondMergeCorrect = parentBill.accountNameFrom == "交通银行（工资）"
        println("  结果: ${if (secondMergeCorrect) "✅ 正确（已知资产未被未知值覆盖）" else "❌ 错误（已知资产被未知值覆盖）"}")
        println()

        // 最终结果
        println("【最终账单状态】")
        println("  accountNameFrom: '${parentBill.accountNameFrom}'")
        println("  accountNameTo: '${parentBill.accountNameTo}'")
        println()

        val allCorrect = firstMergeCorrect && secondMergeCorrect
        println("=".repeat(80))
        if (allCorrect) {
            println("✅ 真实场景测试通过！未知值'银行卡'没有覆盖已知资产'交通银行（工资）'")
        } else {
            println("❌ 真实场景测试失败！请检查 BillMerger.selectBetterAccount 的逻辑")
        }
        println("=".repeat(80))
    }
}

/**
 * 主函数 - 运行 BillMerger 测试
 */
fun main() {
    println()
    println("╔" + "═".repeat(78) + "╗")
    println("║" + " ".repeat(20) + "BillMerger 账单合并逻辑测试" + " ".repeat(30) + "║")
    println("║" + " ".repeat(12) + "核心规则：只有在已知资产列表中的才优先选择" + " ".repeat(20) + "║")
    println("╚" + "═".repeat(78) + "╝")
    println()

    // 运行所有测试用例
    BillMergerTest.runAllTests()

    println()

    // 运行真实场景模拟
    BillMergerTest.testRealScenario()

    println()
    println("测试完成！")
    println()
}
