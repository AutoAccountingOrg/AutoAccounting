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

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

/**
 * 并发账单分析测试
 *
 * 使用真实日志数据测试账单分析的并发处理能力
 * 测试重点：去重逻辑、并发安全性、性能指标
 */
object ConcurrentBillTest {

    // 服务器配置
    private const val SERVER_HOST = "127.0.0.1"
    private const val SERVER_PORT = 52045
    private const val API_PATH = "/js/analysis"

    // 测试数据文件路径（相对于test/resources）
    private const val TEST_DATA_MEITUAN = "test_data_meituan.json"
    private const val TEST_DATA_DIDI = "test_data_didi.json"

    private const val TEST_DATA_TRANSFER = "test_data_transfer.json"

    private const val TEST_DATA_TRANSFER2 = "test_data_transfer2.json"

    private val gson = Gson()

    /**
     * 测试数据模型
     */
    data class TestDataItem(
        val name: String,
        val timestamp: String,
        val app: String,
        val type: String,
        val fromAppData: Boolean,
        val forceAI: Boolean,
        val data: JsonObject
    )

    /**
     * 测试结果
     */
    data class TestResult(
        val name: String,
        val success: Boolean,
        val duration: Long,
        val response: String?,
        val error: String?
    )

    /**
     * 发送HTTP POST请求到服务器
     */
    private fun sendAnalysisRequest(item: TestDataItem): TestResult {
        val startTime = System.currentTimeMillis()

        return try {
            val urlString = "http://$SERVER_HOST:$SERVER_PORT$API_PATH" +
                    "?app=${item.app}" +
                    "&type=${item.type}" +
                    "&fromAppData=${item.fromAppData}" +
                    "&forceAI=${item.forceAI}"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 30000 // 30秒超时
            connection.readTimeout = 30000

            // 发送请求体
            val dataJson = gson.toJson(item.data)
            connection.outputStream.use { os ->
                os.write(dataJson.toByteArray(Charsets.UTF_8))
            }

            // 读取响应
            val responseCode = connection.responseCode
            val response = if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Empty response"
            }

            val duration = System.currentTimeMillis() - startTime

            TestResult(
                name = item.name,
                success = responseCode == 200,
                duration = duration,
                response = response,
                error = if (responseCode != 200) "HTTP $responseCode" else null
            )

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            TestResult(
                name = item.name,
                success = false,
                duration = duration,
                response = null,
                error = e.message
            )
        }
    }

    /**
     * 从资源文件加载测试数据
     */
    private fun loadTestData(fileName: String): List<TestDataItem> {
        // 尝试多种方式加载资源文件
        val jsonText = try {
            // 方式1: 使用classLoader.getResourceAsStream (推荐)
            this::class.java.classLoader?.getResourceAsStream(fileName)?.bufferedReader()
                ?.use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: try {
            // 方式2: 使用getResource
            val resourceUrl = this::class.java.classLoader?.getResource(fileName)
            if (resourceUrl != null) {
                File(resourceUrl.toURI()).readText()
            } else null
        } catch (e: Exception) {
            null
        } ?: try {
            // 方式3: 直接从test/resources目录读取（适用于IDE运行）
            val projectRoot = System.getProperty("user.dir")
            val resourcePath = File(projectRoot, "server/src/test/resources/$fileName")
            if (resourcePath.exists()) {
                resourcePath.readText()
            } else {
                // 尝试不带server前缀
                val altPath = File(projectRoot, "src/test/resources/$fileName")
                if (altPath.exists()) {
                    altPath.readText()
                } else null
            }
        } catch (e: Exception) {
            null
        } ?: throw IllegalStateException(
            "找不到测试数据文件: $fileName\n" +
                    "已尝试的路径:\n" +
                    "  1. ClassLoader.getResourceAsStream($fileName)\n" +
                    "  2. ClassLoader.getResource($fileName)\n" +
                    "  3. ${System.getProperty("user.dir")}/server/src/test/resources/$fileName\n" +
                    "  4. ${System.getProperty("user.dir")}/src/test/resources/$fileName\n" +
                    "请确保文件存在于 src/test/resources/ 目录中"
        )

        val jsonArray = gson.fromJson(jsonText, com.google.gson.JsonArray::class.java)

        return jsonArray.map { element ->
            gson.fromJson(element, TestDataItem::class.java)
        }
    }

    /**
     * 测试美团订单去重逻辑（7个数据源同时到达）
     *
     * 测试重点：
     * 1. 7个请求能否正确识别为同一笔交易
     * 2. 最终是否只生成1个父账单
     * 3. 是否有子账单正确关联到父账单
     * 4. 所有原始数据是否都被正确归档
     */
    suspend fun testMeituanDeduplication() {
        println("=".repeat(80))
        println("美团订单去重逻辑测试（7个数据源并发）")
        println("=".repeat(80))
        println()

        val testData = loadTestData(TEST_DATA_MEITUAN)

        println("【测试场景】")
        println("交易金额: 42.10元")
        println("支付方式: 招商银行信用卡(1356) -> 微信支付 -> 美团")
        println("数据来源: ${testData.size}个不同渠道")
        println()

        println("【数据源列表】")
        testData.forEachIndexed { index, item ->
            println("  ${index + 1}. ${item.name}")
            println("     时间: ${item.timestamp}")
            println("     App: ${item.app}")
            println("     类型: ${item.type}")
        }
        println()

        println("【预期结果】")
        println("  ✓ 请求成功: 7/7")
        println("  ✓ 生成账单: 1个父账单 + N个子账单")
        println("  ✓ 去重正确: 7条数据识别为同一笔交易")
        println("  ✓ 数据归档: 7条原始数据全部保存")
        println()

        println("-".repeat(80))
        println("开始并发测试...")
        println("-".repeat(80))

        val results = mutableListOf<TestResult>()
        val totalTime = measureTimeMillis {
            coroutineScope {
                testData.mapIndexed { index, item ->
                    async(Dispatchers.IO) {
                        val threadName = Thread.currentThread().name
                        println("[$threadName] [${index + 1}/7] 发送: ${item.name}")
                        sendAnalysisRequest(item)
                    }
                }.awaitAll().also { results.addAll(it) }
            }
        }

        println("-".repeat(80))
        println()

        // 详细分析结果
        analyzeDeduplicationResults(results, totalTime)
    }

    /**
     * 测试场景2：testTransfer（2个数据源）
     */
    suspend fun testTransfer() {
        println()
        println("=".repeat(80))
        println("测试场景3：还款测试（3个数据源）")
        println("=".repeat(80))
        println()

        val testData = loadTestData(TEST_DATA_TRANSFER)
        println("加载测试数据: ${testData.size}条")
        testData.forEachIndexed { index, item ->
            println("  ${index + 1}. ${item.name} (${item.timestamp})")
        }
        println()

        println("开始并发测试...")
        val results = mutableListOf<TestResult>()

        val totalTime = measureTimeMillis {
            coroutineScope {
                testData.map { item ->
                    async(Dispatchers.IO) {
                        println("[${Thread.currentThread().name}] 发送请求: ${item.name}")
                        sendAnalysisRequest(item)
                    }
                }.awaitAll().also { results.addAll(it) }
            }
        }

        printResults(results, totalTime)
    }

    suspend fun testDidiLowConcurrency() {
        println()
        println("=".repeat(80))
        println("测试场景2：滴滴订单低并发测试（2个数据源）")
        println("=".repeat(80))
        println()

        val testData = loadTestData(TEST_DATA_DIDI)
        println("加载测试数据: ${testData.size}条")
        testData.forEachIndexed { index, item ->
            println("  ${index + 1}. ${item.name} (${item.timestamp})")
        }
        println()

        println("开始并发测试...")
        val results = mutableListOf<TestResult>()

        val totalTime = measureTimeMillis {
            coroutineScope {
                testData.map { item ->
                    async(Dispatchers.IO) {
                        println("[${Thread.currentThread().name}] 发送请求: ${item.name}")
                        sendAnalysisRequest(item)
                    }
                }.awaitAll().also { results.addAll(it) }
            }
        }

        printResults(results, totalTime)
    }

    /**
     * 分析去重测试结果
     */
    private fun analyzeDeduplicationResults(results: List<TestResult>, totalTime: Long) {
        println("=".repeat(80))
        println("测试结果分析")
        println("=".repeat(80))
        println()

        val successCount = results.count { it.success }
        val failureCount = results.count { !it.success }
        val avgDuration = if (results.isNotEmpty()) results.map { it.duration }.average() else 0.0
        val maxDuration = results.maxOfOrNull { it.duration } ?: 0
        val minDuration = results.filter { it.success }.minOfOrNull { it.duration } ?: 0

        // 1. 基础统计
        println("【基础统计】")
        println("  总耗时: ${totalTime}ms")
        println("  并发请求数: ${results.size}")
        println("  成功: $successCount")
        println("  失败: $failureCount")
        println("  成功率: %.2f%%".format(successCount * 100.0 / results.size))
        println()

        // 2. 性能分析
        println("【性能分析】")
        println("  平均耗时: %.2fms".format(avgDuration))
        println("  最长耗时: ${maxDuration}ms")
        println("  最短耗时: ${minDuration}ms")
        println("  理论总耗时(顺序): ~${avgDuration * results.size}ms")
        println("  实际总耗时(并发): ${totalTime}ms")
        println("  并发效率: %.2f%%".format((avgDuration * results.size / totalTime) * 100))
        println()

        // 3. 详细结果
        println("【请求详情】")
        results.forEachIndexed { index, result ->
            val status = if (result.success) "✓" else "✗"
            val color = if (result.success) "" else "[失败] "
            println("  $status ${index + 1}. ${color}${result.name}")
            println("      耗时: ${result.duration}ms")

            if (result.error != null) {
                println("      ⚠️  错误: ${result.error}")
            }

            // 尝试解析响应中的去重信息
            if (result.response != null) {
                when {
                    result.response.contains("parentId") -> {
                        println("      ✓ 响应包含parentId（去重成功）")
                    }

                    result.response.contains("groupId") -> {
                        println("      ✓ 响应包含groupId（账单分组）")
                    }
                }
            }
        }
        println()

        // 4. 去重验证
        println("【去重验证】")
        if (successCount == results.size) {
            println("  ✓ 所有请求成功处理")
            println()
            println("  ⚠️  去重验证需要检查服务器日志：")
            println("     1. 查找 '自动去重找到父账单' 日志")
            println("     2. 确认最终生成的账单数量")
            println("     3. 验证子账单的groupId指向父账单")
            println()
            println("  预期日志关键词：")
            println("     - '自动去重找到父账单：parentId=XXX'")
            println("     - '账单入库成功：billId=XXX'")
            println("     - '原始数据归档更新：id=XXX, match=true'")
        } else {
            println("  ✗ 有${failureCount}个请求失败，无法完成去重测试")
            println("  请先解决请求失败的问题")
        }
        println()

        // 5. 测试结论
        println("=".repeat(80))
        println("【测试结论】")
        println("=".repeat(80))

        val pass = successCount == results.size && totalTime < 5000
        if (pass) {
            println("✓ 测试通过")
            println()
            println("  - 所有请求成功处理")
            println("  - 并发性能良好 (${totalTime}ms < 5000ms)")
            println("  - 请手动验证服务器日志中的去重逻辑")
        } else {
            println("✗ 测试未通过")
            println()
            if (failureCount > 0) {
                println("  - 有${failureCount}个请求失败")
            }
            if (totalTime >= 5000) {
                println("  - 性能不达标 (${totalTime}ms >= 5000ms)")
            }
        }
        println("=".repeat(80))
    }

    /**
     * 打印测试结果统计（通用版本）
     */
    private fun printResults(results: List<TestResult>, totalTime: Long) {
        println()
        println("-".repeat(80))
        println("测试结果统计")
        println("-".repeat(80))

        val successCount = results.count { it.success }
        val failureCount = results.count { !it.success }
        val avgDuration = if (results.isNotEmpty()) results.map { it.duration }.average() else 0.0
        val maxDuration = results.maxOfOrNull { it.duration } ?: 0
        val minDuration = results.filter { it.success }.minOfOrNull { it.duration } ?: 0

        println("总耗时: ${totalTime}ms")
        println("请求总数: ${results.size}")
        println("成功: $successCount")
        println("失败: $failureCount")
        println("成功率: %.2f%%".format(successCount * 100.0 / results.size))
        println()
        println("耗时统计:")
        println("  平均: %.2fms".format(avgDuration))
        println("  最长: ${maxDuration}ms")
        println("  最短: ${minDuration}ms")
        println()

        // 详细结果
        println("详细结果:")
        results.forEachIndexed { index, result ->
            val status = if (result.success) "✓" else "✗"
            println("  $status ${index + 1}. ${result.name}")
            println("      耗时: ${result.duration}ms")
            if (result.error != null) {
                println("      错误: ${result.error}")
            }
            if (result.response != null && result.response.length < 200) {
                println("      响应: ${result.response}")
            }
        }
        println("-".repeat(80))
    }
}

/**
 * 主函数 - 专注测试美团订单去重逻辑
 */
suspend fun main() {
    println()
    println("╔" + "═".repeat(78) + "╗")
    println("║" + " ".repeat(23) + "美团订单去重测试" + " ".repeat(39) + "║")
    println("║" + " ".repeat(15) + "7个数据源并发 → 1个账单（去重逻辑验证）" + " ".repeat(24) + "║")
    println("╚" + "═".repeat(78) + "╝")
    println()
    println("服务器地址: http://127.0.0.1:52045")
    println("测试时间: ${java.time.LocalDateTime.now()}")
    println()
    println("【重要提示】")
    println("  - 请确保手机端自动记账App正在运行")
    println("  - 已执行: adb forward tcp:52045 tcp:52045")
    println("  - 测试后请查看服务器日志验证去重逻辑")
    println()

    try {
        // 只运行美团订单去重测试
        ConcurrentBillTest.testTransfer()

        println()
        println("╔" + "═".repeat(78) + "╗")
        println("║" + " ".repeat(30) + "测试完成" + " ".repeat(38) + "║")
        println("╚" + "═".repeat(78) + "╝")
        println()
        println("【下一步】请检查服务器日志，验证以下内容：")
        println("  1. 执行: adb logcat | grep -E 'BillService|去重|parentId'")
        println("  2. 确认看到 '自动去重找到父账单' 日志（应该有6次）")
        println("  3. 确认最终只生成1个父账单")
        println("  4. 确认7条原始数据全部归档（match=true）")
        println()

        // 如果想运行其他测试，取消下面的注释
        // println("运行其他测试场景...")
        // delay(2000)
        // ConcurrentBillTest.testDidiLowConcurrency()
        // ConcurrentBillTest.testStressTest()
        // ConcurrentBillTest.testSequentialVsConcurrent()

    } catch (e: Exception) {
        println()
        println("!".repeat(80))
        println("测试执行失败: ${e.message}")
        println("!".repeat(80))
        println()
        e.printStackTrace()
        println()
        println("【常见问题】")
        println("  1. 找不到测试数据文件")
        println("     → 确保 server/src/test/resources/test_data_meituan.json 存在")
        println()
        println("  2. 连接被拒绝 (Connection refused)")
        println("     → 确保手机App正在运行")
        println("     → 确保执行了: adb forward tcp:52045 tcp:52045")
        println()
        println("  3. 请求超时")
        println("     → 检查手机性能")
        println("     → 服务器可能在处理大量数据")
        println("!".repeat(80))
    }
}

