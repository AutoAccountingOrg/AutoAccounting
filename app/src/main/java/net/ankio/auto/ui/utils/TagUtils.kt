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

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ankio.auto.R
import net.ankio.auto.autoApp
import org.ezbook.server.db.model.TagModel
import java.io.IOException
import java.io.InputStreamReader

/**
 * 标签工具类，用于处理标签数据
 */
class TagUtils {

    /**
     * JSON标签项数据模型，用于解析default_tags.json
     */
    private data class JsonTagItem(
        val name: String,
        val color: String
    )

    /**
     * 从 raw/default_tags.json 文件中读取默认标签数据并解析为 TagModel 列表
     * 按照分类顺序输出
     * @return 标签列表
     */
    suspend fun setDefaultTags(): List<TagModel> {
        return try {
            // 打开 raw 资源文件
            val inputStream = autoApp.resources.openRawResource(R.raw.default_tags)
            val reader = InputStreamReader(inputStream)

            // 使用 gson 解析 JSON 数据
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, List<JsonTagItem>>>() {}.type
            val tagMap: Map<String, List<JsonTagItem>> = gson.fromJson(reader, mapType)

            // 关闭资源
            reader.close()
            inputStream.close()

            // 解析并构建标签列表
            val tagList = mutableListOf<TagModel>()

            // 按照定义的顺序处理各个分类
            val categoryOrder =
                listOf("场景", "角色", "属性", "时间", "项目", "情绪", "地点", "平台", "游戏")

            categoryOrder.forEach { category ->
                tagMap[category]?.forEach { jsonTag ->
                    val tagModel = createTagModel(
                        name = jsonTag.name,
                        color = jsonTag.color,
                        group = category
                    )
                    tagList.add(tagModel)
                }
            }

            tagList
        } catch (e: IOException) {
            // 处理文件读取异常，返回空列表
            emptyList()
        } catch (e: Exception) {
            // 处理其他异常，返回空列表
            emptyList()
        }
    }

    /**
     * 创建TagModel实例
     * @param name 标签名称
     * @param color 标签颜色
     * @return TagModel实例
     */
    private fun createTagModel(
        name: String,
        color: String,
        group: String
    ): TagModel {
        return TagModel().apply {
            this.name = name
            this.color = color
            this.group = group
        }
    }
}