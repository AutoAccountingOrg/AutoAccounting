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
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import net.ankio.auto.R
import net.ankio.auto.constant.BillType
import net.ankio.auto.constant.Currency
import net.ankio.auto.constant.DataType
import net.ankio.auto.database.Db
import net.ankio.auto.utils.ImageUtils

@Entity
class BillInfo {

    //账单列表
    @PrimaryKey(autoGenerate = true)
    var id = 0

    /**
     * 账单类型 只有三种
     */
    var type: BillType = BillType.Expend

    /**
     * 币种类型
     */
    var currency: Currency = Currency.CNY

    /**
     * 金额 大于0
     */
    var money: Float = 0.01F

    /**
     * 手续费
     */
    var fee: Float = 0.00F

    /**
     * 记账时间
     * yyyy-MM-dd HH:mm:ss
     */
    var timeStamp: Long = 0

    /**
     * 商户名称
     */
    var shopName: String = ""

    /**
     * 商品名称
     */
    var shopItem: String = ""

    /**
     * 分类名称
     */
    var cateName: String = "其他"

    /**
     * 该账单是否记为报销账单
     */
    var reimbursement: Boolean = false

    /**
     * 远程id，就是记账App中对应的账单ID
     */
    var remoteId: String = "-1"

    /**
     * 账本名称
     */
    var bookName: String = "默认账本"

    /**
     * 账单所属资产名称（或者转出账户）
     */
    var accountNameFrom: String = ""

    /**
     * 远程app中的资产id
     */
    var accountIdFrom: String = "-1"

    /**
     * 转入账户
     */
    var accountNameTo = ""

    /**
     * 远程app中的资产id
     */
    var accountIdTo: String = "-1"

    /**
     * 这笔账单的来源
     */
    var from = ""

    /**
     * 来源类型
     */
    var fromType: DataType = DataType.App

    /**
     * 分组id，这个id是指将短时间内捕获到的同等金额进行合并的分组id
     */
    var groupId: Int = 0

    /**
     * 数据渠道，这里指的是更具体的渠道，例如【建设银行】微信公众号，用户【xxxx】这种
     */
    var channel: String = ""

    /**
     * 是否已从App同步
     */
    var syncFromApp:Boolean = false
    fun toJSON(): String {
        return Gson().toJson(this)
    }



    companion object{
        fun fromJSON(json:String):BillInfo{
            return Gson().fromJson(json,BillInfo::class.java)
        }
        suspend fun getCategoryDrawable(cateName: String,context: Context): Drawable? {
            //TODO 根据分类名称获取对应的分类图标
            return AppCompatResources.getDrawable(context, R.drawable.default_cate)
        }
        suspend fun getBookDrawable(bookName: String,context: Context,imageView: ImageView) {

            ImageUtils.get(context, Db.get().BookNameDao().getByName(bookName)?.icon ?:"",{
                imageView.setImageDrawable(it)
            },{
                imageView.setImageDrawable(
                    ResourcesCompat.getDrawable(context.resources,R.drawable.default_book,context.theme)
                )
            })
        }
    }
}