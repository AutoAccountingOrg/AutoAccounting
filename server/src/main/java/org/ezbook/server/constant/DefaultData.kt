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
    // ======== AI 默认值 ========
    val AI_MODEL: String = "DeepSeek"

    val AI_BILL_RECOGNITION: Boolean = false
    val AI_CATEGORY_RECOGNITION: Boolean = false
    val AI_ASSET_MAPPING: Boolean = false
    val AI_MONTHLY_SUMMARY: Boolean = false
    val AI_SUMMARY_PROMPT: String = """
你是一个专业的财务分析师，擅长分析个人账单数据并提供有价值的财务建议。

任务要求：
1. 分析用户提供的账单数据，特别关注大额交易（≥100元）
2. 生成结构化的财务总结报告
3. 提供实用的理财建议和消费优化建议
4. 识别异常消费模式和潜在的节省机会
5. 重点分析大额支出的合理性和必要性

报告结构要求：
1. 📊 收支概览 - 总收入、总支出、结余情况、支出笔数
2. 💰 大额交易分析 - 重点关注100元以上的收支项目
3. 📈 消费分析 - 主要消费分类、消费趋势分析
4. 🏪 商户分析 - 主要消费商户、消费频次分析
5. 💡 理财建议 - 基于数据的个性化建议，特别针对大额支出优化
6. ⚠️ 风险提醒 - 异常消费或需要注意的地方

输出要求：
- 使用中文回复
- 数据准确，分析客观
- 建议实用可行
- 语言简洁易懂
- 适当使用emoji增强可读性
- 使用HTML而不是Markdown进行输出
- HTML建议使用卡片的形式展示，要丰富多彩，需要适配夜间模式
- 结构卡片可以使用水滴效果的卡片容器，其他容器效果自由发挥
- 不要标题，不要时间
- 最外层的容器不要背景、不要卡片

参考风格:
<div style="font-family: 'Segoe UI', system-ui, sans-serif;max-width: 800px;margin: 0 auto;color: var(--text);"><style>        :root{color-scheme:light dark;--text:#222;--border:rgba(0,0,0,.08);--neutral-05:rgba(0,0,0,.05);--progress-bg:rgba(0,0,0,.08);--blue:#3498db;--blue-rgb:52,152,219;--red:#e74c3c;--red-rgb:231,76,60;--amber:#f39c12;--amber-rgb:243,156,18;--gold:#f1c40f;--gold-rgb:241,196,15;--green:#2ecc71;--green-rgb:46,204,113;--emerald:#27ae60;--purple:#9b59b6;--purple-rgb:155,89,182}@media(prefers-color-scheme:dark){:root{--text:#e6e6e6;--border:rgba(255,255,255,.12);--neutral-05:rgba(255,255,255,.06);--progress-bg:rgba(255,255,255,.12)}}</style><div style="border-radius: 16px;padding: 20px;margin: 20px 0;backdrop-filter: blur(10px);border: 1px solid var(--border);overflow: hidden;"><div style="position: absolute; top: -50%; right: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(var(--blue-rgb), 0.1) 0%, transparent 70%); transform: rotate(-15deg); z-index: -1;"></div><h3 style="margin-top: 0; color: var(--blue);">📊 收支概览</h3><div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;"><div style="background: rgba(var(--blue-rgb), 0.2); padding: 15px; border-radius: 12px; text-align: center;"><div style="font-size: 24px; font-weight: bold; color: var(--blue);">¥0.00</div><div style="font-size: 14px; opacity: 0.8;">总收入</div></div><div style="background: rgba(var(--red-rgb), 0.2); padding: 15px; border-radius: 12px; text-align: center;"><div style="font-size: 24px; font-weight: bold; color: var(--red);">¥0.01</div><div style="font-size: 14px; opacity: 0.8;">总支出</div></div><div style="background: rgba(var(--amber-rgb), 0.2); padding: 15px; border-radius: 12px; text-align: center;"><div style="font-size: 24px; font-weight: bold; color: var(--amber);">¥-0.01</div><div style="font-size: 14px; opacity: 0.8;">净收入</div></div></div><div style="margin-top: 15px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px;"><div style="background: rgba(var(--green-rgb), 0.1); padding: 10px; border-radius: 8px; text-align: center;"><div style="font-size: 18px; font-weight: bold; color: var(--green);">0 笔</div><div style="font-size: 12px; opacity: 0.8;">收入笔数</div></div><div style="background: rgba(var(--red-rgb), 0.1); padding: 10px; border-radius: 8px; text-align: center;"><div style="font-size: 18px; font-weight: bold; color: var(--red);">1 笔</div><div style="font-size: 12px; opacity: 0.8;">支出笔数</div></div></div></div><div style="background: linear-gradient(135deg, rgba(var(--purple-rgb), 0.15), rgba(var(--purple-rgb), 0.15)); border-radius: 16px; padding: 20px; margin: 20px 0; backdrop-filter: blur(10px); border: 1px solid var(--border); position: relative; overflow: hidden;"><div style="position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(var(--purple-rgb), 0.1) 0%, transparent 70%); transform: rotate(15deg); z-index: -1;"></div><h3 style="margin-top: 0; color: var(--purple);">💰 大额交易分析</h3><div style="background: rgba(var(--purple-rgb), 0.1); padding: 15px; border-radius: 12px; margin: 10px 0;"><div style="display: flex; justify-content: space-between; align-items: center;"><div><div style="font-weight: bold; color: var(--purple);">公共课大满足套装</div><div style="font-size: 14px; opacity: 0.8;">收单机构财付通支付科技有限公司</div></div><div style="font-size: 18px; font-weight: bold; color: var(--red);">-¥0.01</div></div><div style="margin-top: 10px; padding: 10px; background: var(--neutral-05); border-radius: 8px;"><div style="font-size: 14px; color: var(--amber);">⚠️                    注意：虽然金额极小，但仍建议关注此类小额测试交易</div></div></div></div><div style="background: linear-gradient(135deg, rgba(var(--gold-rgb), 0.15), rgba(var(--amber-rgb), 0.15)); border-radius: 16px; padding: 20px; margin: 20px 0; backdrop-filter: blur(10px); border: 1px solid var(--border); position: relative; overflow: hidden;"><div style="position: absolute; top: -50%; right: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(var(--gold-rgb), 0.1) 0%, transparent 70%); transform: rotate(-10deg); z-index: -1;"></div><h3 style="margin-top: 0; color: var(--gold);">📈 消费分析</h3><div style="background: rgba(var(--gold-rgb), 0.1); padding: 15px; border-radius: 12px;"><div style="display: flex; justify-content: space-between; margin-bottom: 10px;"><span                    style="font-weight: bold;">其他类消费</span><span                    style="color: var(--red); font-weight: bold;">¥0.01 (100%)</span></div><div style="height: 20px; background: var(--progress-bg); border-radius: 10px; overflow: hidden;"><div style="height: 100%; width: 100%; background: linear-gradient(90deg, #f39c12, #e67e22); border-radius: 10px;"></div></div><div style="margin-top: 15px; color: var(--amber);">📊                消费趋势：仅有一笔极小金额消费，无法形成有效的消费趋势分析</div></div></div><div style="background: linear-gradient(135deg, rgba(var(--green-rgb), 0.15), rgba(var(--green-rgb), 0.15)); border-radius: 16px; padding: 20px; margin: 20px 0; backdrop-filter: blur(10px); border: 1px solid var(--border); position: relative; overflow: hidden;"><div style="position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(var(--green-rgb), 0.1) 0%, transparent 70%); transform: rotate(10deg); z-index: -1;"></div><h3 style="margin-top: 0; color: var(--green);">🏪 商户分析</h3><div style="background: rgba(var(--green-rgb), 0.1); padding: 15px; border-radius: 12px;"><div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;"><div><div style="font-weight: bold; color: var(--green);">                        收单机构财付通支付科技有限公司</div><div style="font-size: 14px; opacity: 0.8;">消费频次：1次</div></div><div style="font-size: 18px; font-weight: bold; color: var(--red);">¥0.01</div></div><div style="background: var(--neutral-05); padding: 10px; border-radius: 8px;"><div style="font-size: 14px; color: var(--emerald);">💡                    商户类型：第三方支付平台，通常用于在线消费或转账</div></div></div></div><div style="background: linear-gradient(135deg, rgba(var(--blue-rgb), 0.15), rgba(41, 128, 185, 0.15)); border-radius: 16px; padding: 20px; margin: 20px 0; backdrop-filter: blur(10px); border: 1px solid var(--border); position: relative; overflow: hidden;"><div style="position: absolute; top: -50%; right: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(var(--blue-rgb), 0.1) 0%, transparent 70%); transform: rotate(-5deg); z-index: -1;"></div><h3 style="margin-top: 0; color: var(--blue);">💡 理财建议</h3><div style="background: rgba(var(--blue-rgb), 0.1); padding: 15px; border-radius: 12px;"><div style="display: flex; align-items: start; margin-bottom: 15px;"><div style="font-size: 24px; margin-right: 10px;">💰</div><div><div style="font-weight: bold; color: var(--blue);">建立收入来源</div><div style="font-size: 14px; opacity: 0.9;">                        当前无收入记录，建议优先建立稳定的收入来源</div></div></div><div style="display: flex; align-items: start; margin-bottom: 15px;"><div style="font-size: 24px; margin-right: 10px;">📱</div><div><div style="font-weight: bold; color: var(--blue);">监控小额交易</div><div style="font-size: 14px; opacity: 0.9;">                        即使是0.01元的小额交易也应关注，防止成为频繁小额扣费的开始</div></div></div><div style="display: flex; align-items: start;"><div style="font-size: 24px; margin-right: 10px;">📊</div><div><div style="font-weight: bold; color: var(--blue);">完善财务记录</div><div style="font-size: 14px; opacity: 0.9;">                        建议开始系统记录所有收支，为后续财务分析打下基础</div></div></div></div></div><div style="background: linear-gradient(135deg, rgba(var(--red-rgb), 0.15), rgba(192, 57, 43, 0.15)); border-radius: 16px; padding: 20px; margin: 20px 0; backdrop-filter: blur(10px); border: 1px solid var(--border); position: relative; overflow: hidden;"><div style="position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(var(--red-rgb), 0.1) 0%, transparent 70%); transform: rotate(5deg); z-index: -1;"></div><h3 style="margin-top: 0; color: var(--red);">⚠️ 风险提醒</h3><div style="background: rgba(var(--red-rgb), 0.1); padding: 15px; border-radius: 12px;"><div style="display: flex; align-items: center; margin-bottom: 10px;"><div style="font-size: 20px; margin-right: 10px;">🔍</div><div style="color: var(--red); font-weight: bold;">零收入状态</div></div><div style="font-size: 14px; margin-bottom: 15px;">                当前账单显示无任何收入记录，这可能是数据不完整或确实无收入来源，需要重点关注</div><div style="display: flex; align-items: center; margin-bottom: 10px;"><div style="font-size: 20px; margin-right: 10px;">🔔</div><div style="color: var(--red); font-weight: bold;">测试交易关注</div></div><div style="font-size: 14px;">                0.01元的交易可能是测试交易或小额验证，建议确认是否为本人操作，防止账户安全问题</div></div></div></div>
        """.trimIndent()

    // ======== 自动记账 默认值 ========
    const val IGNORE_ASSET: Boolean = false
    const val PROACTIVELY_MODEL: Boolean = true
    const val BOOK_APP_ACTIVITY: String = "com.mutangtech.qianji.bill.auto.AddBillIntentAct"
    const val SHOW_AUTO_BILL_TIP: Boolean = true
    const val NOTE_FORMAT: String = "【商户名称】【商品名称】"
    const val WECHAT_PACKAGE: String = "com.tencent.mm"
    const val WECHAT_PACKAGE_ALIAS: String = "${WECHAT_PACKAGE}2"

    // 数据过滤关键字（逗号分隔存储）
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

    // 监听应用白名单（逗号分隔存储）
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

    // 默认账本应用包名
    val BOOK_APP = "com.mutangtech.qianji"

    // ======== 功能模块 默认值 ========
    val SETTING_ASSET_MANAGER = false
    val SETTING_FEE = false
    val SETTING_BOOK_MANAGER = false
    val SETTING_CURRENCY_MANAGER = false
    val SETTING_DEBT = false
    val SETTING_REIMBURSEMENT = false
    val SETTING_REMIND_BOOK = false


    // ======== 备份/同步/UI/系统 默认值 ========
    val LAST_BACKUP_TIME = 0L
    val AUTO_BACKUP = false
    val USE_WEBDAV = false
    val WEBDAV_HOST = "https://dav.jianguoyun.com/dav/"
    val WEBDAV_USER = ""
    val WEBDAV_PASSWORD = ""
    val WEBDAV_PATH = ""
    val DEBUG_MODE = false
    val FLOAT_TIMEOUT_OFF = 0
    val DEFAULT_BOOK_NAME = "默认账本"

    val EXPENSE_COLOR_RED = 0

    val CATEGORY_SHOW_PARENT = false

    val SEND_ERROR_REPORT = true

    val LOCAL_BACKUP_PATH = ""

    val SHOW_RULE_NAME = true

    // ======== 更新 默认值 ========
    val CHECK_RULE_UPDATE = true
    val CHECK_APP_UPDATE = true
    val RULE_VERSION: String = "none"
    val RULE_UPDATE_TIME: String = "none"
    val UPDATE_CHANNEL: String = "stable"

    // ======== UI 外观 默认值 ========
    val UI_PURE_BLACK: Boolean = false
    val UI_FOLLOW_SYSTEM_ACCENT: Boolean = true
    val UI_THEME_COLOR: String = "MATERIAL_DEFAULT"
    val USE_ROUND_STYLE = true

    val AUTO_GROUP = true
    val SHOW_SUCCESS_POPUP = true
    val AUTO_CREATE_CATEGORY = false

    // ======== 系统设置 默认值 ========
    val SYSTEM_LANGUAGE: String = "SYSTEM"
    val KEY_FRAMEWORK: String = "Xposed"  // 默认工作模式
    val HIDE_ICON: Boolean = false
    val INTRO_INDEX: Int = 0
    val LOCAL_ID: String = ""
    val TOKEN: String = ""
    val GITHUB_CONNECTIVITY: Boolean = true
    val LOAD_SUCCESS: Boolean = false
    val DONATE_TIME: String = ""

    // ======== AI 完整默认值 ========
    val API_KEY: String = ""
    val AI_ONE_API_URI: String = ""
    val AI_ONE_API_MODEL: String = ""
    val API_URI: String = ""
    val API_MODEL: String = ""

    // ======== 自动记账完整默认值 ========
    val AUTO_RECORD_BILL: Boolean = false        // 自动记录账单默认开启
    val AUTO_ASSET_MAPPING: Boolean = false     // 自动资产映射默认关闭
    val HOOK_AUTO_SERVER: Boolean = false
    val SETTING_TAG: Boolean = false

    // ======== 权限设置默认值 ========
    val SMS_FILTER: String = ""
    val LANDSCAPE_DND: Boolean = true

    // ======== 同步设置默认值 ========
    val SYNC_TYPE: String = "none"
    val LAST_SYNC_TIME: Long = 0L

    // ======== 同步哈希默认值 ========
    val HASH_ASSET: String = ""
    val HASH_BILL: String = ""
    val HASH_BOOK: String = ""
    val HASH_CATEGORY: String = ""
    val HASH_BAOXIAO_BILL: String = ""

    // ======== UI 完整默认值 ========
    val USE_SYSTEM_SKIN: Boolean = false
    val SHOW_DUPLICATED_POPUP: Boolean = true

    // ======== 悬浮窗默认值 ========
    val FLOAT_TIMEOUT_ACTION: String = "AUTO_ACCOUNT"
    val FLOAT_CLICK: String = "POP_EDIT_WINDOW"
    val FLOAT_LONG_CLICK: String = "NO_ACCOUNT"

    // ======== 更新完整默认值 ========
    val LAST_UPDATE_CHECK_TIME: Long = 0L
    val CHECK_UPDATE_TYPE: String = "auto"

    // ======== 脚本默认值 ========
    val JS_COMMON: String = ""
    val JS_CATEGORY: String = ""


}