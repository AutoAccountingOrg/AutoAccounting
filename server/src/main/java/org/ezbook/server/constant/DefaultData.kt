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

/**
 * 设置项默认值
 * 按照设置页面顺序组织，与 Setting.kt 一一对应，便于管理和维护
 */
object DefaultData {

    // ===================================================================
    // 记账设置 (settings_recording.xml)
    // ===================================================================

    // -------- 记账应用 --------
    val BOOK_APP = "com.mutangtech.qianji"                          // 默认账本应用包名
    val MANUAL_SYNC = false                                         // 手动同步模式默认关闭
    val DELAYED_SYNC_THRESHOLD: Int = 0                             // 延迟同步阈值默认0（实时同步）

    // -------- 记录方式 --------
    val AUTO_RECORD_BILL: Boolean = false                           // 自动记录账单默认关闭
    val LANDSCAPE_DND: Boolean = true                                // 横屏勿扰模式默认开启

    // -------- 账单识别 --------
    val AUTO_GROUP = false                                          // 自动去重默认关闭
    val AUTO_GROUP_TIME_THRESHOLD = 180                             // 自动去重时间阈值（秒），默认180秒
    val AUTO_TRANSFER_RECOGNITION = false                           // 自动识别转账账单默认关闭
    val AUTO_TRANSFER_TIME_THRESHOLD = 120                          // 转账账单合并时间阈值（秒），默认120秒
    val AI_BILL_RECOGNITION: Boolean = false                        // 使用AI识别账单默认关闭

    // -------- 账单管理 --------
    val SHOW_RULE_NAME = true                                       // 显示规则名称默认开启
    val SETTING_FEE = false                                         // 手续费默认关闭
    val SETTING_TAG: Boolean = false                                // 标签功能默认关闭
    val NOTE_FORMAT: String = "【商户名称】【商品名称】"              // 备注格式默认值

    // -------- 分类管理 --------
    val AUTO_CREATE_CATEGORY = false                                // 自动创建分类默认关闭
    val AI_CATEGORY_RECOGNITION: Boolean = false                    // 使用AI识别分类默认关闭

    // -------- 资产管理 --------
    val SETTING_ASSET_MANAGER = false                               // 资产管理默认关闭
    val SETTING_CURRENCY_MANAGER = false                            // 多币种默认关闭
    val SETTING_REIMBURSEMENT = false                               // 报销功能默认关闭
    val SETTING_DEBT = false                                        // 债务功能默认关闭
    val AUTO_ASSET_MAPPING: Boolean = false                         // 记住资产映射默认关闭
    val AI_ASSET_MAPPING: Boolean = false                            // 使用AI进行资产映射默认关闭

    // -------- 账本配置 --------
    val SETTING_BOOK_MANAGER = false                               // 多账本默认关闭
    val DEFAULT_BOOK_NAME = "默认账本"                               // 默认账本名称

    // ===================================================================
    // 交互设置 (settings_interaction.xml)
    // ===================================================================

    // -------- 提醒设置 --------
    val TOAST_POSITION: String = "bottom"                           // 提醒默认位置：底部
    val SHOW_SUCCESS_POPUP = true                                   // 成功提示弹窗默认开启
    val LOAD_SUCCESS: Boolean = false                               // 加载成功默认关闭
    val SHOW_DUPLICATED_POPUP: Boolean = true                       // 重复提示弹窗默认开启

    // -------- OCR识别 --------
    val OCR_FLIP_TRIGGER: Boolean = true                            // 翻转手机触发默认开启
    val OCR_SHOW_ANIMATION: Boolean = true                          // OCR识别时显示动画默认开启

    // -------- 弹窗风格 --------
    val USE_ROUND_STYLE = true                                      // 圆角风格默认开启
    val IS_EXPENSE_RED = false                                      // 支出是否显示为红色默认关闭
    val IS_INCOME_UP = true                                         // 收入是否显示向上箭头默认开启

    // -------- 记账小面板 --------
    val FLOAT_GRAVITY_POSITION: String = "right"                    // 记账小面板默认位置：右侧
    val FLOAT_TIMEOUT_OFF = 0                                       // 超时时间默认0（不超时）
    val FLOAT_TIMEOUT_ACTION: String = "POP_EDIT_WINDOW"           // 超时操作默认值
    val FLOAT_CLICK: String = "POP_EDIT_WINDOW"                    // 点击事件默认值
    val FLOAT_LONG_CLICK: String = "NO_ACCOUNT"                    // 长按事件默认值

    // -------- 记账面板 --------
    val CONFIRM_DELETE_BILL: Boolean = false                        // 删除账单前二次确认默认关闭

    // ===================================================================
    // AI助理 (settings_ai_assistant.xml)
    // ===================================================================

    // -------- AI配置 --------
    val FEATURE_AI_AVAILABLE: Boolean = false                       // AI功能可用性默认关闭（需要配置API后启用）
    val API_PROVIDER: String = "DeepSeek"                          // API提供商默认值
    val API_KEY: String = ""                                       // API密钥默认值
    val API_URI: String = ""                                       // API地址默认值
    val API_MODEL: String = ""                                     // API模型默认值

    // -------- 提示词管理 --------
    /** AI账单识别提示词 - 从原始数据中提取账单信息 */
    val AI_BILL_RECOGNITION_PROMPT: String = """
# Role
You extract structured transaction info from raw financial texts.

# Output
Return ONLY one JSON object. No code fences, no prose. If any hard rule fails, return {}.

# Hard Rules
1) accountNameFrom is MANDATORY. If missing/uncertain -> {}.
2) No guessing. Use data explicitly present in input.
3) Ignore promotions/ads and any non-transaction texts (e.g., 验证码/登录提醒/快递通知/系统提示/聊天/新闻/纯营销). If the content is unrelated to bills or contains no transaction signals (no explicit transaction amount/keyword, no account), return {}.
4) Human personal names are not valid account names.
5) cateName must be chosen strictly from Category Data (comma-separated). If no exact match, set "其他".
6) Defaults: currency="CNY"; fee=0; money=0.00; empty string for optional text; timeText="".
7) Numbers: output absolute value for money/fee; money with 2 decimals; dot as decimal point.
8) Output must be valid JSON with keys exactly as the schema; no extra keys or trailing commas.

# Field Rules
- accountNameFrom: source account (e.g., 支付宝/微信/银行卡/理财/余额宝)。
- accountNameTo: destination account if explicitly present; otherwise "".
- cateName: pick exactly one from Category Data; do not invent.
- currency: 3-letter ISO if present; else "CNY".
- fee: explicit transaction fee; else 0.
- money: transaction amount (not balance/limit/可用额度)。
- shopItem: concrete item name if present; else "".
- shopName: merchant or counterparty if present; else "".
- type: one of ["Transfer","Income","Expend"], based on explicit words/signs:
  - Transfer: both accountNameFrom and accountNameTo are present and different.
  - Income: 收到/入账/到账/退款/收款/转入/充值 等。
  - Expend: 支付/扣款/消费/转出/提现/付款 等。
- timeText: full date-time string if explicitly present (e.g., 2024-08-02 12:01:22 / 2024/08/02 12:01 / 20240802 120122). If absent -> "".

# Disambiguation
- If multiple amounts appear, choose the one labeled as 支付/收款/退款/转账 金额; never choose 余额/限额。
- If multiple categories fit, choose the most specific; if undecidable, set "".
- Prefer omission over fabrication when OCR noise/ambiguity exists.

# Schema
{
  "accountNameFrom": "",
  "accountNameTo": "",
  "cateName": "",
  "currency": "CNY",
  "fee": 0,
  "money": 0.00,
  "shopItem": "",
  "shopName": "",
  "type": "",
  "timeText": ""
}

# Examples
Input: 支付宝消费，商户：肯德基，支付金额￥36.50，账户余额...，时间2024-08-02 12:01:22
Category Data: 餐饮,交通,购物
Output:
{"accountNameFrom":"支付宝","accountNameTo":"","cateName":"餐饮","currency":"CNY","fee":0,"money":36.50,"shopItem":"","shopName":"肯德基","type":"Expend","timeText":"2024-08-02 12:01:22"}

Input: 推广信息：本店大促销...
Category Data: 餐饮,交通
Output:
{}
""".trimIndent()

    /** AI资产映射提示词 - 将账单映射到对应资产账户 */
    val AI_ASSET_MAPPING_PROMPT: String = """
# Role
You select asset names strictly from Asset Data.

# Inputs
Fields (may be empty): asset1, asset2

# Asset Data
- A comma-separated list of valid asset names.
- You MUST choose exactly from this list. Do not invent, translate, or combine names.

# Output (strict JSON only)
- Return ONLY a JSON object with exactly two keys:
  {"asset1":"<name-or-empty>", "asset2":"<name-or-empty>"}
- If a clue has no match, set its value to an empty string: "".
- No extra fields, no explanations, no markdown, no text outside JSON.

# Matching rules (apply in order, independently for each clue)
1) Exact equality (case-sensitive)
2) Case-insensitive equality
3) Substring/contains match; prefer the candidate with the longest overlap
4) If multiple candidates tie, prefer the longer candidate name
5) If still uncertain, use ""

# Example Input
{"asset1":"中国银行储蓄卡","asset2":"支付宝"}

# Example Output
{"asset1":"中国银行","asset2":"支付宝"}

# Example Output (asset2 not found)
{"asset1":"中国银行","asset2":""}
""".trimIndent()

    /** AI分类识别提示词 - 自动分类账单 */
    val AI_CATEGORY_RECOGNITION_PROMPT: String = """
# Role
You select exactly one category name from Category Data.

# Inputs
Fields: ruleName, shopName, shopItem

# Category Data
- A comma-separated list of valid category names.
- You MUST choose one exactly from this list. Do not invent, translate, or combine names.
- Exception: if uncertain after matching, output 其他.

# Output
- Raw text, single line: the chosen category name only.
- No quotes, no JSON, no explanations, no comments, no extra whitespace.
- If uncertain, output 其他.

# Matching rules (apply in order)
1) Exact equality (case-sensitive): compare against shopItem, then shopName, then ruleName.
2) Case-insensitive equality.
3) Substring/contains match. Prefer the candidate with the longest overlap.
4) If still uncertain, output 其他.

# Tie-breakers
- Prefer shopItem over shopName over ruleName.
- Prefer longer and more specific matches.
- Except the fallback 其他, never output a name that is not in Category Data.

# Example Input
{"shopName": "钱塘江超市", "shopItem": "上好佳薯片", "ruleName": "支付宝红包"}

# Example Output
购物
""".trimIndent()

    val AI_SUMMARY_PROMPT: String = """
你是专业的个人财务分析师。请基于“结构化数据（JSON）”与“账单样本”，生成可视化与洞察并重的分析报告。

核心要求（必须覆盖五大维度）：
一、行为规律维度（寻找“幕后真相”）
1) 消费归因分析：基于 summary 与 previousSummary，区分金额上涨来自“单价变化”还是“频次变化”。
2) 场景关联分析：基于 shops / samples / 标签文本 / 渠道信息，寻找行为链条；无证据时明确说明“样本不足”。
3) 时间节律分析：基于 weekdayStats / hourStats 与 samples，识别周期性或复购间隔；无法判断时说明原因。

二、支出结构维度（识别“必要”与“欲望”）
1) Need vs Want（生存/生活/欲望）占比：基于 categories / samples，做合理划分并说明口径。
2) “拿铁因子”汇总：基于 smallExpense（maxAmount 与 items），计算年度化总额并提示来源。
3) 支出健康度：评估刚性支出占比与财务灵活性，若缺数据需说明。

三、趋势与预测维度（提供“安全感”）
1) 余额水位预测：仅当有明确余额数据时推断，否则说明“资产余额缺失”。
2) 大额支出预警：基于 largeTransactions 与 samples，判断可能的周期性。
3) 财务压力测试：基于生存级支出估算可支撑天数（无法计算要说明）。

四、异常与诊断维度（扮演“审计员”）
1) 消费偏移诊断：基于 samples / largeTransactions，指出明显偏离的单笔支出。
2) 频率异常告警：基于 weekdayStats / hourStats，提示超出常态的频次。
3) 静态分类纠偏：基于 shops / categories 的历史匹配，提示可疑分类。

五、纯本地资产评估（隐私用户）
1) 资产构成分析：若 assets 仅有名称与类型，必须说明无法计算比例。
2) 负债侵蚀度：使用 debt 中“还款/借贷相关支出占比”作近似，并说明口径。

结构化数据字段说明（仅可引用这些字段）：
- summary / previousSummary / debt / categories / shops / smallExpense / weekdayStats / hourStats / dailyTrend / assets / largeTransactions / samples

图表要求（使用 ECharts）：
- 必须输出图表区，至少 4 张图：
  1) 分类支出占比（饼图，categories）
  2) 收支趋势（折线图，dailyTrend）
  3) 行为规律（柱状图，weekdayStats 或 hourStats）
  4) 拿铁因子（柱状图，smallExpense.items）
- 可额外添加商户排行图（shops）。
- ECharts 资源请使用官方库（可用 CDN），例如：
  https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js
- 图表数据必须严格来自结构化数据，禁止臆造。

输出要求：
- 使用中文回复
- 数据准确、分析客观、结论简洁
- HTML 输出（不要 Markdown）
- 卡片式布局，适配夜间模式
- 不要总标题，不要时间
- 最外层容器不要背景、不要卡片

格式建议：
1) 先给出“关键结论卡片”
2) 再给出“维度分析卡片”
3) 最后给出“图表卡片”

注意：
- 所有数字必须来自结构化数据或样本
- 对数据缺失要明确说明
- 不要夸张，不要空话
        """.trimIndent()

    // -------- AI功能 --------
    val AI_MONTHLY_SUMMARY: Boolean = false                         // 使用AI进行账单总结（月度）默认关闭

    // ===================================================================
    // 数据管理 (settings_data_management.xml)
    // ===================================================================

    // -------- 自动备份 --------
    val AUTO_BACKUP = false                                         // 自动备份默认关闭
    val BACKUP_KEEP_COUNT = 10                                      // 默认保留10个备份文件

    // -------- 本地备份 --------
    val LOCAL_BACKUP_PATH = ""                                      // 本地备份路径默认值

    // -------- WebDAV备份 --------
    val USE_WEBDAV = false                                          // 启用WebDAV默认关闭
    val WEBDAV_URL = "https://dav.jianguoyun.com/dav/"              // WebDAV服务器URL默认值（示例：坚果云）
    val WEBDAV_USER = ""                                            // WebDAV用户名默认值
    val WEBDAV_PASSWORD = ""                                        // WebDAV密码默认值

    // ===================================================================
    // 系统设置 (settings_system.xml)
    // ===================================================================

    // -------- 外观设置 --------
    val SYSTEM_LANGUAGE: String = "SYSTEM"                          // 系统语言默认跟随系统
    val UI_FOLLOW_SYSTEM_ACCENT: Boolean = true                     // 跟随系统强调色默认开启
    val UI_THEME_COLOR: String = "MATERIAL_DEFAULT"                 // 主题色标识默认值
    val UI_PURE_BLACK: Boolean = false                              // 纯黑暗色默认关闭

    // -------- 更新设置 --------
    val CHECK_APP_UPDATE = true                                     // 应用更新默认开启
    val CHECK_RULE_UPDATE = true                                    // 规则更新默认开启
    val UPDATE_CHANNEL: String = "Stable"                          // 更新渠道默认稳定版

    // -------- 高级功能 --------
    val DEBUG_MODE = false                                          // 调试模式默认关闭
    val SEND_ERROR_REPORT = true                                    // 错误报告默认开启

    // ===================================================================
    // 其他设置（不在设置页面显示，但需要保留）
    // ===================================================================

    // -------- 自动记账相关（内部使用） --------
    const val IGNORE_ASSET: Boolean = false                         // 忽略资产默认关闭
    const val PROACTIVELY_MODEL: Boolean = true                     // 主动模式默认开启
    const val SHOW_AUTO_BILL_TIP: Boolean = true                   // 自动记账提示默认开启
    val SETTING_REMIND_BOOK: Boolean = false                        // 记账提醒默认关闭
    const val WECHAT_PACKAGE: String = "com.tencent.mm"            // 微信包名

    // 数据过滤关键字 - 白名单（逗号分隔存储）
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

    // 数据过滤关键字 - 黑名单（逗号分隔存储），匹配白名单后排除
    const val DATA_FILTER_BLACKLIST = ""

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

    // -------- 权限设置 --------
    val SMS_FILTER: String = ""                                     // 短信过滤默认值

    // -------- 同步设置 --------
    val SYNC_TYPE: String = "none"                                  // 同步类型默认值
    val LAST_SYNC_TIME: Long = 0L                                   // 最后同步时间默认值
    val LAST_BACKUP_TIME = 0L                                      // 最后备份时间默认值

    // -------- 同步哈希值 --------
    val HASH_ASSET: String = ""                                     // 资产哈希默认值
    val HASH_BILL: String = ""                                     // 账单哈希默认值
    val HASH_BOOK: String = ""                                     // 账本哈希默认值
    val HASH_CATEGORY: String = ""                                 // 分类哈希默认值
    val HASH_BAOXIAO_BILL: String = ""                             // 报销单哈希默认值

    // -------- UI设置（其他） --------
    val USE_SYSTEM_SKIN: Boolean = false                            // 系统皮肤默认关闭
    val CATEGORY_SHOW_PARENT = false                               // 显示父分类默认关闭

    // -------- 系统设置（其他） --------
    val KEY_FRAMEWORK: String = "Xposed"                           // 默认工作模式
    val HIDE_ICON: Boolean = false                                 // 是否隐藏启动图标默认关闭
    val INTRO_INDEX: Int = 0                                       // 引导页索引默认值
    val LOCAL_ID: String = ""                                      // 本地实例ID默认值
    val TOKEN: String = ""                                        // 访问令牌默认值
    val GITHUB_CONNECTIVITY: Boolean = true                        // GitHub连通性探测默认开启

    // -------- 更新设置（其他） --------
    val LAST_UPDATE_CHECK_TIME: Long = 0L                         // 检查更新时间默认值
    val CHECK_UPDATE_TYPE: String = "auto"                         // 更新类型默认值
    val RULE_VERSION: String = "none"                             // 规则版本默认值
    val RULE_UPDATE_TIME: String = "none"                         // 规则更新时间默认值

    // -------- 脚本设置 --------
    val JS_COMMON: String = ""                                     // 通用脚本默认值
    val JS_CATEGORY: String = ""                                   // 分类脚本默认值

    // -------- 其他 --------
    val DONATE_TIME: String = ""                                   // 捐赠时间默认值
}
