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

package net.ankio.auto.utils

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import net.ankio.auto.R
import net.ankio.auto.autoApp
import net.ankio.auto.http.api.BookNameAPI

fun Int.toThemeColor(): Int {
    return MaterialColors.getColor(
        ThemeUtils.themedCtx(autoApp),
        this,
        Color.WHITE,
    )
}

fun Int.toDrawable(): Drawable? {
    return autoApp.getDrawable(this)
}

suspend fun String.toBookCover(imageView: ImageView) {
    val book = BookNameAPI.getBook(this)
    val image = book.icon
    Glide.with(autoApp).load(image)
        .placeholder(R.drawable.default_book)   // 加载中
        .error(R.drawable.default_book)           // 加载失败
        .fallback(R.drawable.default_book)
        .into(imageView)
}