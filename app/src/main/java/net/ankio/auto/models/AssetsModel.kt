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
package net.ankio.auto.models

import android.content.Context
import android.graphics.drawable.Drawable
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.ImageUtils

class AssetsModel {
    // 账户列表
    var id = 0
    var name: String = "" // 账户名

    /**
     * 这里的图标是url链接或存储的base64图片
     */
    var icon: String = "" // 图标
    var sort = 0
    var type: Int = 0 // 账户类型
    var extras: String = "" // 额外信息，例如银行卡的卡号等

    companion object {

        suspend fun list(): List<AssetsModel> {
          /*  val data = runCatching {
                AppUtils.getService().sendMsg("assets/list", mapOf("page" to 0, "size" to 0)) as List<AssetsModel>
            }.getOrNull()?: emptyList()
            return data*/
            return emptyList()
        }

        suspend fun getByName(name: String): AssetsModel? {
         /*   val data = AppUtils.getService().sendMsg("assets/get", mapOf("name" to name))
            return runCatching { Gson().fromJson(data as JsonObject, AssetsModel::class.java) }.getOrNull()
        */
        return null}

        suspend fun getDrawable(
            account: String,
            context: Context,
        ): Drawable =
            withContext(Dispatchers.IO) {
                val accountInfo = getByName(account)
                ImageUtils.get(context, accountInfo?.icon ?: "", R.mipmap.ic_launcher_round)
            }
    }
}
