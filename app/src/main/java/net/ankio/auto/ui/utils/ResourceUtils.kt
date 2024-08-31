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
import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.storage.ImageUtils
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryModel

object ResourceUtils {
    //   BookNameModel.getDrawable(billInfoModel.bookName, context, binding.bookImage)
    suspend fun getBookNameDrawable(name:String,context:Context,view:View) = withContext(Dispatchers.IO){
        val book = BookNameModel.getByName(name)
        val image = book.icon
        ImageUtils.get(context, image, R.drawable.default_book).let {
            withContext(Dispatchers.Main){
                view.background = it
            }
        }
    }

    suspend fun getCategoryDrawable(data: CategoryModel,image:ImageView) = withContext(Dispatchers.IO) {
        val icon = data.icon?:""
        ImageUtils.get(image.context, icon, R.drawable.default_cate).let {
            withContext(Dispatchers.Main){
                image.setImageDrawable(it)
            }
        }
    }
}