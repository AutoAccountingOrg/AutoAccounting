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

package org.ezbook.server.ai.tools

import org.ezbook.server.ai.AiManager
import org.ezbook.server.constant.BillType
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.DefaultData
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.log.ServerLog
import org.ezbook.server.tools.SettingUtils
import org.ezbook.server.tools.runCatchingExceptCancel

class CategoryTool {

    /**
     * 获取分类识别提示词
     * 优先使用用户自定义的提示词，如果为空则使用默认值
     */
    private suspend fun getPrompt(): String {
        val customPrompt = SettingUtils.aiCategoryRecognitionPrompt()
        return customPrompt.ifBlank {
            DefaultData.AI_CATEGORY_RECOGNITION_PROMPT
        }
    }

    /**
     * 执行分类识别
     * @param data 原始账单数据(JSON字符串)
     * @param app 来源应用
     * @param dataType 数据来源类型
     * @param categories 当前账本和账单类型下的分类
     */
    suspend fun execute(
        data: String,
        app: String,
        dataType: DataType,
        categories: List<CategoryModel>
    ): String? {
        val prompt = getPrompt()
        ServerLog.d("分类匹配请求：dataType=$dataType")

        val categoryPaths = categoryPaths(categories)
        if (categoryPaths.isEmpty()) {
            ServerLog.d("当前账本和账单类型没有可用分类，跳过AI分类")
            return null
        }
        val fallback = fallbackCategory(categoryPaths)
        // 记录分类候选规模
        ServerLog.d("分类候选统计：total=${categoryPaths.size}")
        // 组装上下文信息，帮助 AI 进行语义分类
        val user = """
Input:
- Context:
  - Source App: $app
  - Data Type: $dataType
- Raw Data: 
  ```
  $data
  ```
- Category Data:
  ```
  ${categoryPaths.joinToString(",")}
  ```      
        """.trimIndent()

        // 调用 AI 进行分类选择
        ServerLog.d("调用AI进行分类匹配...")

        val response = runCatchingExceptCancel {
            val resp = AiManager.getInstance().request(prompt, user).getOrThrow()
            if (resp.isEmpty()) {
                // AI 无返回
                ServerLog.d("AI分类返回空响应")
            }
            resp
        }.getOrElse {
            ServerLog.d("AI分类请求失败，使用当前账本的兜底分类")
            return fallback
        }

        val selected = resolveCategoryPath(response, categories)
        if (selected == null) {
            ServerLog.d("AI分类结果不在当前账本候选中，使用兜底分类")
        }
        return selected ?: fallback
    }

    suspend fun loadCategories(bookRemoteId: String, billType: BillType): List<CategoryModel> {
        val categoryType = categoryType(billType) ?: return emptyList()
        return Db.get().categoryDao().loadByBookAndType(bookRemoteId, categoryType.name)
    }

    internal companion object {
        fun categoryType(billType: BillType): BillType? = when {
            billType.name.startsWith(BillType.Expend.name) -> BillType.Expend
            billType.name.startsWith(BillType.Income.name) -> BillType.Income
            else -> null
        }

        fun categoryNames(categories: List<CategoryModel>): List<String> =
            categories.mapNotNull { it.name?.trim()?.takeIf(String::isNotEmpty) }.distinct()

        fun categoryPaths(categories: List<CategoryModel>): List<String> {
            val namesByRemoteId = categories
                .filter { it.remoteId.isNotBlank() }
                .associate { it.remoteId to it.name?.trim().orEmpty() }
            return categories.mapNotNull { category ->
                val name = category.name?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
                val parentName = namesByRemoteId[category.remoteParentId].orEmpty()
                if (parentName.isNotEmpty()) "$parentName - $name" else name
            }.distinct()
        }

        fun categoryExists(categoryName: String, categories: List<CategoryModel>): Boolean {
            return categoryName.trim() in categoryPaths(categories)
        }

        fun resolveCategoryPath(categoryName: String, categories: List<CategoryModel>): String? {
            val name = categoryName.trim()
            if (name.isEmpty()) return null

            val paths = categoryPaths(categories)
            paths.firstOrNull { it == name }?.let { return it }

            val namesByRemoteId = categories
                .filter { it.remoteId.isNotBlank() }
                .associate { it.remoteId to it.name?.trim().orEmpty() }
            return categories.mapNotNull { category ->
                val childName = category.name?.trim()?.takeIf(String::isNotEmpty)
                    ?: return@mapNotNull null
                val parentName = namesByRemoteId[category.remoteParentId].orEmpty()
                if (childName == name && parentName.isNotEmpty()) "$parentName - $childName" else null
            }.distinct().singleOrNull()
        }

        fun selectCategory(response: String, categoryNames: List<String>): String? =
            response.trim().takeIf(categoryNames::contains)

        fun fallbackCategory(categoryNames: List<String>): String? =
            categoryNames.firstOrNull { it == "其他" || it == "其它" }
    }
}
