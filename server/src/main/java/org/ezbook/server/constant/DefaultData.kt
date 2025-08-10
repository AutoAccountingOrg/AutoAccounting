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

package org.ezbook.server.constant

object DefaultData {
    val AI_MODEL: String = "DeepSeek"
    val USE_AI_FOR_CATEGORIZATION: Boolean = false
    const val IGNORE_ASSET: Boolean = false
    const val PROACTIVELY_MODEL: Boolean = true
    const val BOOK_APP_ACTIVITY: String = "com.mutangtech.qianji.bill.auto.AddBillIntentAct"
    const val SHOW_DUPLICATED_POPUP: Boolean = true
    const val LANDSCAPE_DND: Boolean = true
    const val LOAD_SUCCESS: Boolean = false
    const val AI_AUXILIARY: Boolean = false
    const val SHOW_AUTO_BILL_TIP: Boolean = true
    const val NOTE_FORMAT: String = "【商户名称】【商品名称】"
    const val WECHAT_PACKAGE: String = "com.tencent.mm"
    const val WECHAT_PACKAGE_ALIAS: String = "${WECHAT_PACKAGE}2"
    val DATA_FILTER = listOf(
        "银行", "信用卡", "借记卡", "公积金",
        "元", "￥", "¥", "人民币",
        "消费", "支付", "支出", "转出", "取出", "取款",
        "收入", "转入", "存入", "存款", "退款",
        "还款", "贷款", "借款", "逾期",
        "转账",
        "账户", "余额",
        "交易", "动账", "账单",
    ).joinToString(",")

    val APP_FILTER = listOf(
        "cmb.pb", // 招商银行
        "cn.gov.pbc.dcep", // 数字人民币
        "com.sankuai.meituan", // 美团
        "com.unionpay", // 云闪付
        "com.tencent.mm", // 微信
        "com.eg.android.AlipayGphone", // 支付宝
        "com.jingdong.app.mall", // 京东
        "com.taobao.taobao", // 淘宝
        "com.xunmeng.pinduoduo", // 拼多多
        "com.sankuai.waimai", // 美团外卖
        "me.ele", // 饿了么
        "com.icbc", // 工商银行

        // 核心钱包/聚合支付
        "com.huawei.wallet", // 华为钱包
        "com.mipay.wallet", // 小米支付
        "com.oppo.wallet", // OPPO 钱包
        "com.coloros.wallet", // OPPO 钱包（ColorOS）
        "com.vivo.wallet", // vivo 钱包
        "com.google.android.apps.walletnfcrel", // Google Pay
        "com.paypal.android.p2pmobile", // PayPal

        // 出行/本地生活
        "com.sdu.didi.psnger", // 滴滴出行
        "com.wudaokou.hippo", // 盒马

        // 电商/内容平台
        "com.ss.android.ugc.aweme", // 抖音
        "com.smile.gifmaker", // 快手
        "com.achievo.vipshop", // 唯品会
        "com.suning.mobile.ebuy", // 苏宁易购
        "com.xiaomi.youpin", // 小米有品

        // 金融理财/支付工具
        "com.jd.jrapp", // 京东金融
        "com.baidu.wallet", // 度小满金融

        // 运营商缴费
        "com.greenpoint.android.mc10086", // 中国移动
        "com.sinovatech.unicom.ui", // 中国联通
        "com.ct.client", // 中国电信

        // 银行类
        "com.chinamworld.main", // 建设银行
        "com.android.bankabc", // 农业银行
        "com.chinamworld.bocmbci", // 中国银行
        "com.bankcomm.Bankcomm", // 交通银行
        "com.yitong.mbank.psbc", // 邮储银行
        "com.pingan.papd", // 平安银行
        "com.ecitic.bank.mobile", // 中信银行
        "cn.com.cmbc.newmbank", // 民生银行
        "com.cebbank.mobile.cemb", // 光大银行
        "com.cib.cibmb", // 兴业银行
        "cn.com.spdb.mobilebank.per", // 浦发银行（个人）
        "com.spdbccc.app", // 浦发信用卡
        "com.cgbchina.xpt", // 广发银行
        "com.hxb.mobile.client", // 华夏银行
        "com.bankofbeijing.mobilebanking", // 北京银行
        "cn.com.shbank.mper", // 上海银行
        "com.nbbank.mobilebank", // 宁波银行
        "com.webank.wemoney", // 微众银行
        "com.mybank.android.phone", // 网商银行

    ).joinToString(",")

    // 钱迹
    val BOOK_APP = "com.mutangtech.qianji"

    val SETTING_ASSET_MANAGER = false
    val SETTING_FEE = false
    val SETTING_BOOK_MANAGER = false
    val SETTING_CURRENCY_MANAGER = false
    val SETTING_DEBT = false
    val SETTING_REIMBURSEMENT = false
    val SETTING_REMIND_BOOK = false


    val LAST_BACKUP_TIME = 0L
    val AUTO_BACKUP = false
    val USE_WEBDAV = false
    val WEBDAV_HOST = ""
    val WEBDAV_USER = ""
    val WEBDAV_PASSWORD = ""
    val DEBUG_MODE = false
    val FLOAT_TIMEOUT_OFF = "10"
    val DEFAULT_BOOK_NAME = "默认账本"

    val EXPENSE_COLOR_RED = 0

    val CATEGORY_SHOW_PARENT = false

    val SEND_ERROR_REPORT = true

    val LOCAL_BACKUP_PATH = ""

    val SHOW_RULE_NAME = true

    val CHECK_RULE_UPDATE = true
    val CHECK_APP_UPDATE = true

    val USE_AI = false
    val AUTO_GROUP = true
    val USE_ROUND_STYLE = false
    val SHOW_SUCCESS_POPUP = true
    val AUTO_CREATE_CATEGORY = false


}