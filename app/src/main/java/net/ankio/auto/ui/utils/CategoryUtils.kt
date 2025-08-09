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

package net.ankio.auto.ui.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ankio.auto.R
import net.ankio.auto.autoApp
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryModel
import java.io.IOException
import java.io.InputStreamReader

/**
 * 分类工具类，用于处理分类数据
 */
class CategoryUtils {

    /**
     * JSON分类项数据模型，用于解析default_category.json
     */
    private data class JsonCategoryItem(
        val name: String,
        val icon: String,
        val sub: List<JsonSubCategoryItem>? = null
    )

    /**
     * JSON子分类项数据模型
     */
    private data class JsonSubCategoryItem(
        val name: String,
        val icon: String
    )

    /**
     * 从 raw/category.json 文件中读取分类数据并解析为 CategoryItem 列表
     * @param context 上下文对象
     * @return 分类项目列表
     */
    fun list(context: Context): List<CategoryModel> {
        return try {
            // 打开 raw 资源文件
            val inputStream = context.resources.openRawResource(R.raw.category)
            val reader = InputStreamReader(inputStream)

            // 使用 gson 解析 JSON 数据
            val gson = Gson()
            val listType = object : TypeToken<List<CategoryModel>>() {}.type
            val categories: List<CategoryModel> = gson.fromJson(reader, listType)

            // 关闭资源
            reader.close()
            inputStream.close()

            categories
        } catch (e: IOException) {
            // 处理文件读取异常，返回空列表
            emptyList()
        } catch (e: Exception) {
            // 处理其他异常，返回空列表
            emptyList()
        }
    }

    /**
     * 从 raw/default_category.json 文件中读取默认分类数据并解析为 CategoryModel 列表
     * 按照父级、子级的顺序输出
     * @param bookNameModel 账本模型，用于设置分类所属账本
     * @return 按父级、子级顺序排列的分类列表
     */
    fun setDefaultCategory(bookNameModel: BookNameModel): List<CategoryModel> {
        return try {
            // 打开 raw 资源文件
            val inputStream = autoApp.resources.openRawResource(R.raw.default_category)
            val reader = InputStreamReader(inputStream)

            // 使用 gson 解析 JSON 数据
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, List<JsonCategoryItem>>>() {}.type
            val categoryMap: Map<String, List<JsonCategoryItem>> = gson.fromJson(reader, mapType)

            // 关闭资源
            reader.close()
            inputStream.close()

            // 解析并构建分类列表
            val categoryList = mutableListOf<CategoryModel>()
            var sortIndex = 0

            // 处理支出分类
            categoryMap["支出"]?.let { expendCategories ->
                expendCategories.forEachIndexed { parentIndex, jsonCategory ->
                    // 创建父级分类
                    val parentCategory = createCategoryModel(
                        name = jsonCategory.name,
                        icon = jsonCategory.icon,
                        type = BillType.Expend,
                        bookNameModel = bookNameModel,
                        parentId = "-1", // 父级分类的parentId为-1
                        sort = sortIndex++
                    )
                    categoryList.add(parentCategory)

                    // 创建子级分类
                    jsonCategory.sub?.forEachIndexed { subIndex, subCategory ->
                        val childCategory = createCategoryModel(
                            name = subCategory.name,
                            icon = subCategory.icon,
                            type = BillType.Expend,
                            bookNameModel = bookNameModel,
                            parentId = parentCategory.remoteId, // 子级分类关联到父级的remoteId
                            sort = sortIndex++
                        )
                        categoryList.add(childCategory)
                    }
                }
            }

            // 处理收入分类
            categoryMap["收入"]?.let { incomeCategories ->
                incomeCategories.forEachIndexed { parentIndex, jsonCategory ->
                    // 创建父级分类
                    val parentCategory = createCategoryModel(
                        name = jsonCategory.name,
                        icon = jsonCategory.icon,
                        type = BillType.Income,
                        bookNameModel = bookNameModel,
                        parentId = "-1", // 父级分类的parentId为-1
                        sort = sortIndex++
                    )
                    categoryList.add(parentCategory)

                    // 创建子级分类
                    jsonCategory.sub?.forEachIndexed { subIndex, subCategory ->
                        val childCategory = createCategoryModel(
                            name = subCategory.name,
                            icon = subCategory.icon,
                            type = BillType.Income,
                            bookNameModel = bookNameModel,
                            parentId = parentCategory.remoteId, // 子级分类关联到父级的remoteId
                            sort = sortIndex++
                        )
                        categoryList.add(childCategory)
                    }
                }
            }

            categoryList
        } catch (e: IOException) {
            // 处理文件读取异常，返回空列表
            emptyList()
        } catch (e: Exception) {
            // 处理其他异常，返回空列表
            emptyList()
        }
    }

    /**
     * 创建CategoryModel实例
     * @param name 分类名称
     * @param icon 分类图标
     * @param type 分类类型（支出/收入）
     * @param bookNameModel 账本模型
     * @param parentId 父级分类ID
     * @param sort 排序索引
     * @return CategoryModel实例
     */
    private fun createCategoryModel(
        name: String,
        icon: String,
        type: BillType,
        bookNameModel: BookNameModel,
        parentId: String,
        sort: Int
    ): CategoryModel {
        return CategoryModel().apply {
            this.name = name
            this.icon = icon
            this.type = type
            this.book = bookNameModel.id.toInt()
            this.remoteBookId = bookNameModel.remoteId
            this.remoteParentId = parentId
            this.remoteId = "${name}_${type.name.lowercase()}_${System.currentTimeMillis()}"
            this.sort = sort
        }
    }
}
