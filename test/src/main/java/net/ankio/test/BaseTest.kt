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

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

/**
 * 测试基础类
 *
 * 提供账单分析测试的公共功能：
 * - 加载测试数据
 * - 发送HTTP请求
 * - 结果分析和输出
 */
abstract class BaseTest {

    // 服务器配置
    companion object {
        const val SERVER_HOST = "127.0.0.1"
        const val SERVER_PORT = 52045
        const val API_PATH = "/js/analysis"

        val gson = Gson()
    }

    /**
     * 测试数据模型
     */
    data class TestDataItem(
        val name: String,
        val timestamp: String,
        val app: String,
        val type: String,
        val fromAppData: Boolean,
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
     * 测试配置
     */
    data class TestConfig(
        val title: String,            // 测试标题
        val description: String,      // 测试描述
        val dataFile: String,         // 测试数据文件名
        val expectedResults: List<String> = emptyList(),  // 预期结果描述
        val verifyCommands: List<String> = emptyList()    // 验证命令
    )

    /**
     * 获取测试配置（子类实现）
     */
    abstract fun getConfig(): TestConfig

    /**
     * 发送HTTP POST请求到服务器
     */
    protected fun sendAnalysisRequest(item: TestDataItem): TestResult {
        val startTime = System.currentTimeMillis()

        return try {
            val urlString = "http://$SERVER_HOST:$SERVER_PORT$API_PATH" +
                    "?app=${item.app}" +
                    "&type=${item.type}" +
                    "&fromAppData=${item.fromAppData}"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            val dataJson = gson.toJson(item.data)
            connection.outputStream.use { os ->
                os.write(dataJson.toByteArray(Charsets.UTF_8))
            }

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
    protected fun loadTestData(fileName: String): List<TestDataItem> {
        val jsonText = try {
            this::class.java.classLoader?.getResourceAsStream(fileName)?.bufferedReader()
                ?.use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: try {
            val resourceUrl = this::class.java.classLoader?.getResource(fileName)
            if (resourceUrl != null) {
                File(resourceUrl.toURI()).readText()
            } else null
        } catch (e: Exception) {
            null
        } ?: try {
            val projectRoot = System.getProperty("user.dir")
            val resourcePath = File(projectRoot, "test/src/main/resources/$fileName")
            if (resourcePath.exists()) {
                resourcePath.readText()
            } else null
        } catch (e: Exception) {
            null
        } ?: throw IllegalStateException(
            "找不到测试数据文件: $fileName\n" +
                    "已尝试的路径:\n" +
                    "  1. ClassLoader.getResourceAsStream($fileName)\n" +
                    "  2. ClassLoader.getResource($fileName)\n" +
                    "  3. ${System.getProperty("user.dir")}/test/src/main/resources/$fileName\n" +
                    "请确保文件存在于 test/src/main/resources/ 目录中"
        )

        val jsonArray = gson.fromJson(jsonText, com.google.gson.JsonArray::class.java)

        return jsonArray.map { element ->
            gson.fromJson(element, TestDataItem::class.java)
        }
    }

    /**
     * 运行测试
     */
    suspend fun runTest() {
        val config = getConfig()

        printHeader(config)

        val testData = loadTestData(config.dataFile)

        printTestData(testData, config)

        println("-".repeat(80))
        println("开始并发测试...")
        println("-".repeat(80))

        val results = mutableListOf<TestResult>()
        val totalTime = measureTimeMillis {
            coroutineScope {
                testData.mapIndexed { index, item ->
                    async(Dispatchers.IO) {
                        val threadName = Thread.currentThread().name
                        println("[$threadName] [${index + 1}/${testData.size}] 发送: ${item.name}")
                        sendAnalysisRequest(item)
                    }
                }.awaitAll().also { results.addAll(it) }
            }
        }

        println("-".repeat(80))
        println()

        analyzeResults(results, totalTime, config)
    }

    /**
     * 打印测试头部信息
     */
    private fun printHeader(config: TestConfig) {
        println()
        println("╔" + "═".repeat(78) + "╗")
        println("║" + centerText(config.title, 78) + "║")
        println("╚" + "═".repeat(78) + "╝")
        println()
        println("服务器地址: http://$SERVER_HOST:$SERVER_PORT")
        println("测试时间: ${java.time.LocalDateTime.now()}")
        println()
        println("【重要提示】")
        println("  - 请确保手机端自动记账App正在运行")
        println("  - 已执行: adb forward tcp:$SERVER_PORT tcp:$SERVER_PORT")
        println()
    }

    /**
     * 打印测试数据信息
     */
    private fun printTestData(testData: List<TestDataItem>, config: TestConfig) {
        println("=".repeat(80))
        println("测试场景")
        println("=".repeat(80))
        println()

        println("【测试描述】")
        println("  ${config.description}")
        println()

        println("【数据来源】${testData.size}条")
        testData.forEachIndexed { index, item ->
            println("  ${index + 1}. ${item.name}")
            println("     时间: ${item.timestamp}")
            println("     App: ${item.app}")
            println("     类型: ${item.type}")
        }
        println()

        if (config.expectedResults.isNotEmpty()) {
            println("【预期结果】")
            config.expectedResults.forEach { result ->
                println("  ✓ $result")
            }
            println()
        }
    }

    /**
     * 分析测试结果
     */
    private fun analyzeResults(results: List<TestResult>, totalTime: Long, config: TestConfig) {
        println("=".repeat(80))
        println("测试结果分析")
        println("=".repeat(80))
        println()

        val successCount = results.count { it.success }
        val failureCount = results.count { !it.success }
        val avgDuration = if (results.isNotEmpty()) results.map { it.duration }.average() else 0.0
        val maxDuration = results.maxOfOrNull { it.duration } ?: 0
        val minDuration = results.filter { it.success }.minOfOrNull { it.duration } ?: 0

        // 基础统计
        println("【基础统计】")
        println("  总耗时: ${totalTime}ms")
        println("  并发请求数: ${results.size}")
        println("  成功: $successCount")
        println("  失败: $failureCount")
        println("  成功率: %.2f%%".format(successCount * 100.0 / results.size))
        println()

        // 性能分析
        println("【性能分析】")
        println("  平均耗时: %.2fms".format(avgDuration))
        println("  最长耗时: ${maxDuration}ms")
        println("  最短耗时: ${minDuration}ms")
        println()

        // 响应详情
        println("【响应详情】")
        var hasTransfer = false
        var hasParentId = false
        var hasGroupId = false

        results.forEachIndexed { index, result ->
            val status = if (result.success) "✓" else "✗"
            println("  $status ${index + 1}. ${result.name}")
            println("      耗时: ${result.duration}ms")

            if (result.error != null) {
                println("      ⚠️ 错误: ${result.error}")
            }

            if (result.response != null) {
                // 检查关键字段
                if (result.response.contains("\"type\":\"Transfer\"") ||
                    result.response.contains("type=Transfer")
                ) {
                    hasTransfer = true
                    println("      ✓ 类型: Transfer")
                }
                if (result.response.contains("parentId")) {
                    hasParentId = true
                    println("      ✓ 已关联父账单 (parentId)")
                }
                if (result.response.contains("groupId")) {
                    hasGroupId = true
                    println("      ✓ 已设置分组 (groupId)")
                }

                // 提取账户信息
                val accountFromMatch =
                    Regex("accountNameFrom[\"=:]+([^,\"\\)]+)").find(result.response)
                val accountToMatch = Regex("accountNameTo[\"=:]+([^,\"\\)]+)").find(result.response)

                accountFromMatch?.let {
                    val value = it.groupValues[1].trim()
                    if (value.isNotEmpty() && value != "''") {
                        println("      → accountNameFrom: $value")
                    }
                }
                accountToMatch?.let {
                    val value = it.groupValues[1].trim()
                    if (value.isNotEmpty() && value != "''") {
                        println("      → accountNameTo: $value")
                    }
                }
            }
        }
        println()

        // 测试结论
        println("=".repeat(80))
        println("【测试结论】")
        println("=".repeat(80))
        println()

        if (successCount == results.size) {
            println("✅ 所有请求成功处理")
            println()

            if (hasTransfer) {
                println("  ✓ 检测到 Transfer 类型账单")
            }
            if (hasParentId || hasGroupId) {
                println("  ✓ 检测到账单合并（parentId/groupId）")
            }

            if (!hasTransfer && !hasParentId && !hasGroupId) {
                println("  ⚠️ 未检测到账单合并或转账类型")
                println("     可能原因：")
                println("     1. 转账识别功能未启用（检查App设置）")
                println("     2. 时间窗口不匹配")
                println("     3. 规则未匹配")
            }

            if (config.verifyCommands.isNotEmpty()) {
                println()
                println("【验证命令】")
                config.verifyCommands.forEach { cmd ->
                    println("  $cmd")
                }
            }
        } else {
            println("❌ 有 $failureCount 个请求失败")
            println()
            println("【常见问题】")
            println("  1. 连接被拒绝 → 确保手机App正在运行")
            println("  2. 请求超时 → 检查手机性能")
            println("  3. adb forward 未执行 → adb forward tcp:$SERVER_PORT tcp:$SERVER_PORT")
        }
        println("=".repeat(80))
    }

    /**
     * 文本居中
     */
    private fun centerText(text: String, width: Int): String {
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text + " ".repeat(width - padding - text.length)
    }
}

