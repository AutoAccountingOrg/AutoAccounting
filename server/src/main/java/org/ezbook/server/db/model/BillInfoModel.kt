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

package org.ezbook.server.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.ezbook.server.constant.BillState
import org.ezbook.server.constant.BillType
import org.ezbook.server.tools.MD5HashTable

@Entity
data class BillInfoModel(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    /**
     * 账单类型
     */
    var type: BillType = BillType.Income,

    /**
     * 币种信息（JSON 格式的 CurrencyModel）
     *
     * 新格式：{"code":"USD","rate":7.19,"timestamp":1706745600000}
     * 兼容旧格式：纯币种代码字符串如 "CNY"
     *
     * @see CurrencyModel
     */
    var currency: String = "",

    /**
     * 金额 大于0
     */
    var money: Double = 0.00,

    /**
     * 手续费
     * 对于自动记账来讲小于0的是手续费，大于0的是优惠
     * 支出优惠，total = money + fee
     * 还款（转账）手续费  total = money - fee
     * 还款（转账）优惠  total = money + fee
     */
    var fee: Double = 0.00,

    /**
     * 记账时间
     * yyyy-MM-dd HH:mm:ss
     */
    var time: Long = 0,

    /**
     * 商户名称
     */
    var shopName: String = "",

    /**
     * 商品名称
     */
    var shopItem: String = "",

    /**
     * 分类名称
     */
    var cateName: String = "",

    /**
     * 拓展数据域，如果是报销或者销账，会对应账单ID
     */
    var extendData: String = "",

    /**
     * 账本名称
     */
    var bookName: String = "默认账本",

    /**
     * 账单所属资产名称（或者转出账户）
     */
    var accountNameFrom: String = "",

    /**
     * 转入账户
     */
    var accountNameTo: String = "",

    /**
     * 这笔账单的来源,例如是微信还是支付宝
     */
    var app: String = "",

    /**
     * 去重id，这个id是指将短时间内捕获到的同等金额进行合并的去重id
     */
    var groupId: Long = -1,

    /**
     * 数据来源渠道，这里指的是更具体的渠道，例如【建设银行】微信公众号，用户【xxxx】这种
     */
    var channel: String = "",

    /**
     * 订单状态
     */
    var state: BillState = BillState.Wait2Edit,

    /**
     * 备注信息
     */
    var remark: String = "",

    /**
     * 是否为自动记录的账单
     */
    var auto: Boolean = false,

    /**
     * 规则名称
     */
    var ruleName: String = "",

    /**
     * 标签名称列表，使用逗号分割的字符串存储多个标签名称
     * 例如: "餐饮,报销,出差" 表示该账单关联了这些标签
     */
    var tags: String = "",

    /**
     * 标志位集合：默认无，可通过位运算组合多个标志
     */
    var flag: Int = 0,

    /**
     * 映射前的原始来源账户名。
     * 由 BillService 在执行资产映射前设置，
     * 用于"记住资产映射"时以原始名查询映射表。
     */
    var rawAccountNameFrom: String = "",

    /**
     * 映射前的原始目标账户名。
     */
    var rawAccountNameTo: String = ""
) {
    companion object {
        /** 标志位：无 */
        const val FLAG_NONE = 0

        /** 标志位：不计入统计 */
        const val FLAG_NOT_COUNT = 1

        /** 标志位：不计入预算 */
        const val FLAG_NOT_BUDGET = 2
    }

    /**
     * 判断是否包含指定标志位
     * @param flag 目标标志位
     * @return 是否包含该标志位
     */
    fun hasFlag(flag: Int): Boolean {
        return (this.flag and flag) == flag
    }

    /**
     * 设置或清除指定标志位
     * @param flag 目标标志位
     * @param enabled 是否启用该标志位
     */
    fun setFlag(flag: Int, enabled: Boolean) {
        this.flag = if (enabled) this.flag or flag else this.flag and flag.inv()
    }

    fun needReCategory(): Boolean {
        // 分类内容为空 且 不是AI生成
        return (cateName.isEmpty() || cateName == "其他" || cateName == "其它") && !generateByAi()
    }


    fun hasValidCategory(): Boolean {
        return cateName.isNotEmpty() && cateName !== "其他" && cateName !== "其它"
    }

    fun generateByAi(): Boolean {
        return ruleName.contains("生成")
    }

    override fun toString(): String {
        return "BillInfoModel(id=$id, type=$type, currency='$currency', money=$money, fee=$fee, time=$time, shopName='$shopName', shopItem='$shopItem', cateName='$cateName', extendData='$extendData', bookName='$bookName', accountNameFrom='$accountNameFrom', accountNameTo='$accountNameTo', app='$app', groupId=$groupId, channel='$channel', state=$state, remark='$remark', auto=$auto, ruleName='$ruleName', tags='$tags', flag=$flag)"
    }

    fun hash(): String {
        // md5
        return MD5HashTable.md5(toString())
    }

    /**
     * 分类拆分：按 '-' 分割父/子分类。
     * - 正常："父-子" -> Pair(父, 子)
     * - 其它（无 '-'、在首位、在末位）：返回 Pair(原字符串, "")，默认放在前面
     */
    fun categoryPair(): Pair<String, String> {
        val name = cateName.trim()
        val i = name.indexOf('-')
        val parent = if (i <= 0) name else name.substring(0, i).trim()
        val child = if (i in 1 until name.lastIndex) name.substring(i + 1).trim() else ""
        return parent to child
    }

    /**
     * 获取标签列表
     * @return 标签名称列表
     */
    fun getTagList(): List<String> {
        if (tags.isEmpty()) return emptyList()
        return tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * 设置标签列表
     * @param tagList 标签名称列表
     */
    fun setTagList(tagList: List<String>) {
        tags = tagList.filter { it.isNotEmpty() }.joinToString(",")
    }

    /**
     * 添加标签
     * @param tag 标签名称
     */
    fun addTag(tag: String) {
        if (tag.isEmpty()) return
        val currentTags = getTagList().toMutableList()
        if (!currentTags.contains(tag)) {
            currentTags.add(tag)
            setTagList(currentTags)
        }
    }

    /**
     * 移除标签
     * @param tag 标签名称
     */
    fun removeTag(tag: String) {
        val currentTags = getTagList().toMutableList()
        currentTags.remove(tag)
        setTagList(currentTags)
    }

    /**
     * 检查是否包含指定标签
     * @param tag 标签名称
     * @return 是否包含该标签
     */
    fun hasTag(tag: String): Boolean {
        return getTagList().contains(tag)
    }

    fun isChild() = this.groupId > 0

    /**
     * 解析 currency 字段为 CurrencyModel
     * 兼容旧格式（纯币种代码字符串）和新格式（JSON）
     */
    fun currencyModel(): CurrencyModel = CurrencyModel.fromJson(currency)

    /**
     * 获取币种代码（如 "USD"、"CNY"）
     * 便捷方法，避免每次都解析完整 CurrencyModel
     */
    fun currencyCode(): String = currencyModel().code.ifEmpty { "CNY" }
}