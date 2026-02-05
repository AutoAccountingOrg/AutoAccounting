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

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
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
object TagUtils {

    /**
     * 动态标签视图标记，用于区分自动渲染的标签与其他子视图
     */
    private val tagLabelMarkerKey = R.id.tagLabelMarker

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
     * 渲染标签列表到容器，风格与基础信息组件保持一致
     * @param container 标签容器
     * @param tagNames 标签名称列表
     * @param emptyPlaceholder 空列表时显示的占位文本，传 null 则直接隐藏
     * @param textSizeSp 文本大小，单位 sp
     */
    fun renderTagLabels(
        container: ViewGroup,
        tagNames: List<String>,
        emptyPlaceholder: String? = null,
        textSizeSp: Float = 12f
    ) {
        removeTagLabelViews(container)

        // 处理空态占位文本
        val isEmpty = tagNames.isEmpty()
        val labelTexts = if (isEmpty) {
            listOf(emptyPlaceholder ?: "")
        } else {
            tagNames
        }

        // 基础配色与基础信息组件保持一致
        val defaultTextColor = MaterialColors.getColor(
            container,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
        val defaultBackgroundColor = MaterialColors.getColor(
            container,
            com.google.android.material.R.attr.colorSurfaceContainerLow
        )
        val surfaceStrongColor = MaterialColors.getColor(
            container,
            com.google.android.material.R.attr.colorSurfaceContainerHighest
        )
        val emptyTextColor = applyAlpha(defaultTextColor, 0.6f)
        val emptyBackgroundColor = applyAlpha(defaultBackgroundColor, 0.6f)

        // 无标签且无需占位时，只隐藏纯标签容器，避免影响其他控件
        if (isEmpty && emptyPlaceholder == null) {
            return
        }

        val paddingHorizontal = dpToPx(container, 8)
        val paddingVertical = dpToPx(container, 3)

        labelTexts.forEach { text ->
            // 空态弱化显示，非空态使用标签配色
            val (textColor, backgroundColor) = if (isEmpty) {
                emptyTextColor to emptyBackgroundColor
            } else {
                val (tColor, bgColor, _) = PaletteManager.getSelectorTagColors(
                    container.context,
                    text,
                    defaultTextColor,
                    defaultBackgroundColor,
                    surfaceStrongColor,
                    true
                )
                tColor to bgColor
            }

            val label = MaterialTextView(container.context).apply {
                // 标签文本保持原样，不增加图标等装饰
                this.text = text
                setTextColor(textColor)
                textSize = textSizeSp
                setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                background = container.context.getDrawable(R.drawable.currency_label_background)
                background?.setTint(backgroundColor)
                // 标记为工具渲染的标签，便于下次局部清理
                setTag(tagLabelMarkerKey, true)
            }
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            container.addView(label, params)
        }
    }


    /**
     * 移除当前工具渲染的标签视图，保留其他子视图
     * @param container 标签容器
     */
    private fun removeTagLabelViews(container: ViewGroup) {
        for (index in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(index)
            if (child.getTag(tagLabelMarkerKey) == true) {
                container.removeViewAt(index)
            }
        }
    }

    /**
     * 判断容器是否包含非标签子视图
     * @param container 标签容器
     * @return true 表示存在非标签子视图
     */
    private fun hasNonTagChildren(container: ViewGroup): Boolean {
        for (index in 0 until container.childCount) {
            val child = container.getChildAt(index)
            if (child.getTag(tagLabelMarkerKey) != true) {
                return true
            }
        }
        return false
    }

    /**
     * 创建TagModel实例
     * @param name 标签名称
     * @return TagModel实例
     */
    private fun createTagModel(
        name: String,
        group: String
    ): TagModel {
        return TagModel().apply {
            this.name = name
            this.group = group
        }
    }

    /**
     * dp 转 px，避免硬编码
     * @param view 参考视图
     * @param dp dp 数值
     * @return px 数值
     */
    private fun dpToPx(view: View, dp: Int): Int {
        return (dp * view.resources.displayMetrics.density).toInt()
    }

    /**
     * 透明度处理，保持风格统一
     * @param color 原始颜色
     * @param alpha 透明度比例
     * @return 应用透明度后的颜色
     */
    private fun applyAlpha(color: Int, alpha: Float): Int {
        val clampedAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return Color.argb(
            clampedAlpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}