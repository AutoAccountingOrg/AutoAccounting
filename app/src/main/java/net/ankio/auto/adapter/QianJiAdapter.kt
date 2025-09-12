/*
 * Copyright (C) 2025 ankio
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

package net.ankio.auto.adapter

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.runBlocking
import net.ankio.auto.App
import net.ankio.auto.autoApp
import net.ankio.auto.constant.BookFeatures
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.http.api.AssetsAPI
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.SystemUtils
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.BillAction
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BillInfoModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import io.github.oshai.kotlinlogging.KotlinLogging

class QianJiAdapter : IAppAdapter {

    private val logger = KotlinLogging.logger(this::class.java.name)

    override val pkg: String
        get() = "com.mutangtech.qianji"
    override val link: String
        get() = "https://qianjiapp.com/"
    override val icon: String
        get() = "https://pp.myapp.com/ma_icon/0/icon_52573842_1744768940/256"
    override val desc: String
        get() = """
钱迹，一款简洁纯粹的记账 App，是一个 "无广告、无开屏、无理财" 的 "三无" 产品。
力求极简，专注个人记账，将每一笔收支都清晰记录，消费及资产随时了然于心。
        """.trimIndent()
    override val name: String
        get() = "钱迹"

    /**
     * 检查指定账户是否为信用卡类型
     * @param accountName 账户名称
     * @return 是否为信用卡账户
     */
    private fun isCreditAccount(accountName: String): Boolean = runBlocking {
        if (accountName.isEmpty()) return@runBlocking false

        // 首先通过名称进行简单判断
        val nameBasedCheck = accountName.contains("信用", ignoreCase = true) ||
                accountName.contains("credit", ignoreCase = true) ||
                accountName.contains("花呗", ignoreCase = true) ||
                accountName.contains("白条", ignoreCase = true)

        if (nameBasedCheck) return@runBlocking true

        // 如果启用了资产管理，则查询资产类型
        if (PrefManager.featureAssetManage) {
            try {
                val asset = AssetsAPI.getByName(accountName)
                return@runBlocking asset?.type == AssetsType.CREDIT
            } catch (e: Exception) {
                // 查询失败时回退到名称判断
                return@runBlocking false
            }
        }

        false
    }

    override fun features(): List<BookFeatures> {
        return if (AppAdapterManager.xposedMode()) {
            listOf(
                BookFeatures.MULTI_BOOK,
                BookFeatures.FEE,
                //  BookFeatures.TAG,
                BookFeatures.DEBT,
                BookFeatures.ASSET_MANAGE,
                BookFeatures.MULTI_CURRENCY,
                BookFeatures.REIMBURSEMENT
            )
        } else {
            listOf(
                BookFeatures.MULTI_BOOK,
                BookFeatures.FEE,
                //   BookFeatures.TAG,
                //   BookFeatures.LEADING,
                BookFeatures.ASSET_MANAGE,
                //  BookFeatures.MULTI_CURRENCY,
                BookFeatures.REIMBURSEMENT
            )
        }
    }

    override fun syncAssets() {
        if (AppAdapterManager.ocrMode()) {
            return
        }
        // Xposed模式下对该接口进行了Hook,支持数据同步功能
        val uriBuilder = StringBuilder("qianji://publicapi/addbill")
        uriBuilder.append("?action=").append(BillAction.SYNC_BOOK_CATEGORY_ASSET) //同步资产、账单等数据
        val intent = Intent(Intent.ACTION_VIEW, uriBuilder.toString().toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        SystemUtils.startActivityIfResolvable(intent, name)
    }

    override fun syncWaitBills(billAction: BillAction) {
        if (AppAdapterManager.ocrMode()) {
            return
        }
        // Xposed模式下对该接口进行了Hook,支持数据同步功能
        val uriBuilder = StringBuilder("qianji://publicapi/addbill")
        uriBuilder.append("?action=").append(billAction) // 同步需要报销和退款的账单
        val intent = Intent(Intent.ACTION_VIEW, uriBuilder.toString().toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        SystemUtils.startActivityIfResolvable(intent, name)
    }

    override fun syncBill(billInfoModel: BillInfoModel) {
        // 通过钱迹提供的 Uri-Scheme 完成自动化记账。
        // 文档：qianji://publicapi/addbill?type=0&money=26.5&time=2020-01-31 12:30:00&remark=备注&catename=分类&accountname=账户&accountname2=转入账户&bookname=账本&fee=手续费&showresult=0

        // 1) 映射账单类型到钱迹类型编码
        val qjType = toQianJiType(billInfoModel)

        // 2) 必填参数：type、money
        val uriBuilder = StringBuilder("qianji://publicapi/addbill")
            .append("?type=").append(qjType)

        // 3) 可选参数：时间（yyyy-MM-dd HH:mm:ss）
        if (billInfoModel.time > 0) {
            uriBuilder.append("&time=").append(formatTime(billInfoModel.time))
        }

        // 4) 备注
        if (billInfoModel.remark.isNotEmpty()) {
            uriBuilder.append("&remark=")
                .append(Uri.encode(billInfoModel.remark))
        }

        // 5) 分类：支持一二级分类，按 model 拆分后转换为 "/::/"
        val cate = buildQianJiCategory(billInfoModel)
        if (cate.isNotEmpty()) {
            uriBuilder.append("&catename=")
                .append(Uri.encode(cate))
        }

        // 6) 分类选择模式：0表示自动选择
        uriBuilder.append("&catechoose=0")

        // 7) 账本（可选）- 排除默认账本名称
        if (billInfoModel.bookName.isNotEmpty() &&
            billInfoModel.bookName != "默认账本" &&
            billInfoModel.bookName != "日常账本" &&
            PrefManager.featureMultiBook
        ) {
            uriBuilder.append("&bookname=")
                .append(Uri.encode(billInfoModel.bookName))
        }

        // 8) 账户信息处理（基于配置项）
        if (PrefManager.featureAssetManage) {
            if (billInfoModel.accountNameFrom.isNotEmpty()) {
                uriBuilder.append("&accountname=")
                    .append(Uri.encode(billInfoModel.accountNameFrom))
            }
            if (billInfoModel.accountNameTo.isNotEmpty()) {
                uriBuilder.append("&accountname2=")
                    .append(Uri.encode(billInfoModel.accountNameTo))
            }
        }

        // 转账或者信用卡还款的手续费，传入金额，必须>0，且 <money 。需要额外注意的是，如果想传入手续费，则 money 参数提供的金额，必须是包含 fee 参数的金额的，比如 money=2.0&fee=1.0 则代表 money 中，有 1.0 元的手续费。最终生成的账单金额为 2.0 元，且有 1.0 元的手续费，转入账户入账金额为 (money-fee)=1.0 元，转出账户扣除金额为 2.0 元

        // 9) 手续费（基于配置项）
        if (PrefManager.featureFee && billInfoModel.fee != 0.0) {
            if (billInfoModel.fee < 0) {
                uriBuilder.append("&fee=").append(-billInfoModel.fee)
                billInfoModel.money -= billInfoModel.fee
            } else {
                logger.warn { "钱迹接口不支持优惠记录" }
            }
        }

        uriBuilder.append("&money=").append(billInfoModel.money)

        // 10) 货币（基于配置项）
        if (PrefManager.featureMultiCurrency && billInfoModel.currency.isNotEmpty()) {
            uriBuilder.append("&currency=").append(billInfoModel.currency)
        } else {
            uriBuilder.append("&currency=CNY")
        }

        // 11) 扩展数据（自动记账添加的拓展字段）
        if (billInfoModel.extendData.isNotEmpty()) {
            uriBuilder.append("&extendData=")
                .append(Uri.encode(billInfoModel.extendData))
        }

        // 12) 账单ID（用于更新或关联）
        if (billInfoModel.id > 0) {
            uriBuilder.append("&id=").append(billInfoModel.id)
        }

        // TODO 标签

        if (!PrefManager.showSuccessPopup) {
            // 13) 统一不弹出成功提示
            uriBuilder.append("&showresult=0")
        }

        // 14) 发起隐式 Intent 调起钱迹
        val intent = Intent(Intent.ACTION_VIEW, uriBuilder.toString().toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        logger.info { "目标应用uri：${uriBuilder}" }
        SystemUtils.startActivityIfResolvable(intent, name) {
            AppAdapterManager.markSynced(billInfoModel)
        }
    }

    override fun supportSyncAssets(): Boolean {
        if (AppAdapterManager.xposedMode()) return true
        //TODO 初始化钱迹默认资产，只有没有账本和分类的时候才初始化
        return false
    }

    /**
     * 将内部账单类型映射到钱迹类型编码。
     * 钱迹支持的类型：
     * 0 支出、1 收入、2 转账、3 信用卡还款、5 报销
     * 15-20 为扩展类型（Xposed模式下支持）
     */
    private fun toQianJiType(billInfoModel: BillInfoModel): Int {
        return when (billInfoModel.type) {
            BillType.Expend -> 0
            BillType.Income -> 1
            BillType.Transfer -> {
                // 根据目标账户类型判断是否为信用卡还款
                if (isCreditAccount(billInfoModel.accountNameTo)) {
                    3 // 信用卡还款
                } else {
                    2 // 普通转账
                }
            }
            BillType.ExpendReimbursement -> 5
            BillType.IncomeReimbursement -> if (AppAdapterManager.xposedMode()) 19 else 1
            BillType.ExpendLending -> if (AppAdapterManager.xposedMode()) 15 else 0
            BillType.ExpendRepayment -> if (AppAdapterManager.xposedMode()) 16 else 0
            BillType.IncomeLending -> if (AppAdapterManager.xposedMode()) 17 else 1
            BillType.IncomeRepayment -> if (AppAdapterManager.xposedMode()) 18 else 1
            BillType.IncomeRefund -> if (AppAdapterManager.xposedMode()) 20 else 1
        }
    }

    /**
     * 构造钱迹可识别的分类字符串。
     * - 使用 BillInfoModel.categoryPair() 获取 (父, 子)
     * - 有子类时输出 "父/::/子"；否则仅输出父
     */
    private fun buildQianJiCategory(model: BillInfoModel): String {
        val (parent, child) = model.categoryPair()
        if (parent.isEmpty()) return ""
        return if (child.isNotEmpty()) "${parent}/::/${child}" else parent
    }

    /**
     * 将毫秒时间戳格式化为钱迹要求的时间格式：yyyy-MM-dd HH:mm:ss。
     */
    private fun formatTime(timeMillis: Long): String {
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }

    override fun sleep(): Long {
        return if (PrefManager.workMode == WorkMode.Xposed) 0L else 3000L
    }
}
