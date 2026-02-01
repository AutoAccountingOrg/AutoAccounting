/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import net.ankio.auto.ui.theme.DynamicColors as ThemeColor
import net.ankio.auto.R
import net.ankio.auto.autoApp
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.http.api.CategoryAPI
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.CategoryModel

/**
 * 资源工具类，提供各种图标和图片的加载功能
 * 基于Glide实现，支持base64和网络图片
 *
 * 合并了原ResUtils.kt的功能：
 * - 主题颜色转换
 * - Drawable资源获取
 * - 账本封面加载
 */

// ================================
// 主题和资源扩展函数（来自ResUtils.kt）
// ================================

/**
 * 将Material Design颜色属性转换为当前主题下的实际颜色值
 * @return 当前主题下的颜色值
 */
fun Int.toThemeColor(): Int = ThemeColor.resolve(this)

fun Int.resToColor(): Int = ContextCompat.getColor(autoApp, this)
/**
 * 将资源ID转换为Drawable对象
 * @return Drawable对象，可能为null
 */
fun Int.toDrawable(): Drawable? {
    return autoApp.getDrawable(this)
}

/**
 * 为账本名称设置对应的封面图片
 * @param imageView 目标ImageView
 */
suspend fun String.toBookCover(imageView: ImageView) {
    val book = BookNameAPI.getBook(this)
    val image = book.icon
    Glide.with(autoApp).load(image)
        .placeholder(R.drawable.default_book)   // 加载中
        .error(R.drawable.default_book)           // 加载失败
        .fallback(R.drawable.default_book)
        .into(imageView)
}

// ================================
// 图片加载扩展函数
// ================================

/**
 * 给任意 ImageView 加载图片。
 *
 * @param src  图片源：可为 data:image;base64、http/https URL，或 null/空串
 * @param defaultResId 占位 & 错误图资源 id
 */
fun ImageView.load(
    src: String?,
    defaultResId: Int? = null,
) {

    val glide = Glide.with(this)
        .load(
            when {
                src.isNullOrBlank() -> defaultResId          // 空串直接用占位图
                else -> src                   // 普通 URL
            }
        )
        .error(defaultResId)
    if (defaultResId != null) {
        glide.fallback(defaultResId)
    }
    glide.into(this)
}

/**
 * ImageView扩展函数：设置分类图标
 * @param categoryModel 分类数据模型
 */
fun ImageView.setCategoryIcon(categoryModel: CategoryModel) {
    val icon = categoryModel.icon ?: ""
    this.load(icon, R.drawable.default_cate)
}

/**
 * ImageView扩展函数：根据分类名称设置分类图标
 * @param name 分类名称
 * @param bookId 账本ID
 * @param type 分类类型
 */
suspend fun ImageView.setCategoryIcon(
    name: String,
    bookId: String = "",
    type: String = ""
) {
    var cateName = name
    if (cateName.contains("-")) {
        cateName = cateName.split("-")[1].trim()
    }
    val item = CategoryAPI.getByName(cateName, bookId, type)
    val icon = item?.icon ?: ""
    this.load(icon, R.drawable.default_cate)
}

/**
 * ImageView扩展函数：设置资产图标
 * @param assetsModel 资产数据模型
 */
fun ImageView.setAssetIcon(assetsModel: AssetsModel) {
    val icon = assetsModel.icon
    this.load(icon, R.drawable.default_asset)
}

/**
 * ImageView扩展函数：根据URI设置资产图标
 * @param uri 图片URI
 */
fun ImageView.setAssetIcon(uri: String) {
    this.load(uri, R.drawable.default_asset)
}

/**
 * ImageView扩展函数：根据资产名称设置资产图标
 * @param name 资产名称
 */
suspend fun ImageView.setAssetIconByName(name: String) {
    val asset = AssetsAPI.getByName(name)
    val icon = asset?.icon ?: ""
    this.load(icon, R.drawable.default_asset)
}