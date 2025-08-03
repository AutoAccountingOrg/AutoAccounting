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
import java.io.IOException
import java.io.InputStreamReader

/**
 * 分类工具类，用于处理分类数据
 */
class CategoryUtils {

    /**
     * 从 raw/category.json 文件中读取分类数据并解析为 CategoryItem 列表
     * @param context 上下文对象
     * @return 分类项目列表
     */
    fun list(context: Context): List<CategoryItem> {
        return try {
            // 打开 raw 资源文件
            val inputStream = context.resources.openRawResource(R.raw.category)
            val reader = InputStreamReader(inputStream)

            // 使用 gson 解析 JSON 数据
            val gson = Gson()
            val listType = object : TypeToken<List<CategoryItem>>() {}.type
            val categories: List<CategoryItem> = gson.fromJson(reader, listType)

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
}

/**
 * 分类数据项
 * @param name 分类名称
 * @param url 分类图标URL
 */
data class CategoryItem(val name: String, val url: String)