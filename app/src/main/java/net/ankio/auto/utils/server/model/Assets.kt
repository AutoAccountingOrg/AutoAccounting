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
package net.ankio.auto.utils.server.model

import android.content.Context
import android.graphics.drawable.Drawable
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.ImageUtils
import net.ankio.common.constant.AssetsType

class Assets {
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
        fun put(assets: Assets) {
            AppUtils.getScope().launch {
                AppUtils.getService().sendMsg("asset/put", assets)
            }
        }

        suspend fun get(limit: Int = 500,type: AssetsType): List<Assets> {
            val data = AppUtils.getService().sendMsg("asset/get", mapOf("limit" to limit , "type" to type.value))
            return Gson().fromJson(data as JsonArray, Array<Assets>::class.java).toList()
        }

        suspend fun getByName(name: String): Assets? {
            val data = AppUtils.getService().sendMsg("asset/get/name", mapOf("name" to name))
            return runCatching { Gson().fromJson(data as JsonObject, Assets::class.java) }.getOrNull()
        }

        suspend fun remove(name: String) {
            AppUtils.getService().sendMsg("asset/remove", mapOf("name" to name))
        }

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
