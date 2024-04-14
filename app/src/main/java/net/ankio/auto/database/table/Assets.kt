/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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
package net.ankio.auto.database.table

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.ankio.auto.R
import net.ankio.auto.database.Db
import net.ankio.auto.utils.ImageUtils
import net.ankio.common.constant.AssetType
import net.ankio.common.model.AssetsModel

@Entity
class Assets {
    // 账户列表
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var name: String = "" // 账户名

    /**
     * 这里的图标是url链接或存储的base64图片
     */
    var icon: String = "" // 图标
    var sort = 0
    var type: AssetType = AssetType.CASH // 账户类型
    var extras: String = "" // 额外信息，例如银行卡的卡号等

    companion object {
        fun fromModel(it: AssetsModel): Assets {
            val assets = Assets()
            assets.name = it.name
            assets.icon = it.icon
            assets.sort = it.sort
            assets.type = it.type
            assets.extras = it.extra
            return assets
        }

        suspend fun getDrawable(
            account: String,
            context: Context,
        ): Drawable {
            val accountInfo = Db.get().AssetsDao().get(account)
            return ImageUtils.get(context, accountInfo?.icon ?: "", R.mipmap.ic_launcher_round)
        }
    }
}
