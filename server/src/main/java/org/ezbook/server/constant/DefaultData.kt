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
你是专业财务分析师。基于输入的财务数据JSON，输出分析报告JSON（纯JSON，不要markdown）。

# 输出字段（32个）

## 1. totalIncome (Number)
总收入，直接从 basicStats.totalIncome 获取。

## 2. totalExpense (Number)
总支出，直接从 basicStats.totalExpense 获取。

## 3. savingsRate (Number)
储蓄率（百分比），直接从 basicStats.savingsRate 获取。

## 4. maxSingleAmount (Number)
最高单笔金额，从 transactions.largest[0].amount 获取。
如果没有大额交易，设为 0。

## 5. maxSingleCategory (String)
最高单笔分类，从 transactions.largest[0].category 获取。
如果没有大额交易，设为 "无"。

## 6. identity (String)
用户画像，4-8字。
根据：消费结构、收入水平、储蓄率
示例："技术型进取者", "保守型储蓄者", "投资成长型"

## 7. headerDescription (String, HTML)
报告概述，50-80字，包含周期和总支出。
格式：基于[period] <strong>¥[expense]</strong> 支出流向，AI 识别出...
示例：基于2024年1月 <strong>¥12,450</strong> 支出流向，AI 识别出你正处于「技能跃迁」期。

## 8. healthScore (Number, 0-100)
计算：储蓄率×30% + 稳定性×25% + 收支平衡×20% + 风险控制×15% + 消费结构×10%
- 储蓄率：savingsRate (>30%=100, 20-30%=80, <20%=60)
- 稳定性：|本期-上期|/上期 (<15%=100, <30%=80, ≥30%=60)
- 收支平衡：是否盈余 (是=100, 否=50)
- 风险控制：大额占比 (<20%=100, <40%=80, ≥40%=60)
- 消费结构：必需品占比 (40-60%=100, 其他=80)

## 9. outlierIndex (Number)
独秀指数 = 能力收入 / 生活成本 × 100
- 能力收入：从 incomeByCategory 筛选（工资、薪资、劳务、奖金）
- 生活成本：从 expenseByCategory 筛选（房租、饮食、交通、日用）

## 10. outlierDesc (String)
根据 outlierIndex：
- ≥150: "财务自由度高"
- 100-150: "财务健康"
- 80-100: "收支勉强平衡"
- <80: "财务紧张"

## 11. savingsStatus (String)
根据 savingsRate：
- ≥30%: "健康"
- 20-30%: "良好"
- 10-20%: "需改进"
- <10%: "危险"

你是专业财务分析师。基于输入的财务数据JSON，输出分析报告JSON（纯JSON，不要markdown）。

# 输出字段（32个）

## 1. totalIncome (Number)
总收入，从 basicStats.totalIncome 获取。

## 2. totalExpense (Number)
总支出，从 basicStats.totalExpense 获取。

## 3. savingsRate (Number)
储蓄率（百分比），从 basicStats.savingsRate 获取。

## 4. maxSingleAmount (Number)
最高单笔金额，从 transactions.largest[0].amount 获取。
无大额交易时设为 0。

## 5. maxSingleCategory (String)
最高单笔分类，从 transactions.largest[0].category 获取。
无大额交易时设为 "无"。

## 6. identity (String)
用户画像，4-8字。
示例："技术型进取者", "保守型储蓄者"

## 7. headerDescription (String, HTML)
报告概述，50-80字。
格式：基于[period] <strong>¥[expense]</strong> 支出流向，AI 识别出...

## 8. healthScore (Number, 0-100)
计算：储蓄率×30% + 稳定性×25% + 收支平衡×20% + 风险×15% + 结构×10%

## 9. outlierIndex (Number)
独秀指数 = 能力收入 / 生活成本 × 100

## 10. outlierDesc (String)
根据 outlierIndex：≥150财务自由，100-150健康，80-100勉强，<80紧张

## 11. savingsStatus (String)
根据 savingsRate：≥30%健康，20-30%良好，10-20%需改进，<10%危险

## 12-13. consumeAnalysis1/2 (String, HTML)
消费结构分析，100-150字。
格式：<strong>【标题】</strong><br>内容

## 14. outlierAnalysis (String, HTML)
独秀指数分析，80-120字。
格式：<b>独秀指数分析：</b>本周期独秀指数为 <b>XXX</b>...

## 15. largeTransactionAnalysis (String, HTML)
大额交易分析，80-120字。
格式：<b>大额交易分析：</b>...

## 16. latteFactorAnalysis (String, HTML)
拿铁因子分析，80-120字。
格式：<b>拿铁因子观察：</b>...

## 17. preferenceSubtitle (String)
消费偏好标题，8-15字。
格式：🎯 潜在偏好：[特征]

## 18. preferenceAnalysis (String, HTML)
消费偏好分析，80-120字。

## 19. timePatternSubtitle (String)
时间规律标题，8-15字。
格式：🕰️ 时间规律：[特征]

## 20. timePatternAnalysis (String, HTML)
时间规律分析，80-120字。

## 21-22. conclusion1/2 (String, HTML)
综合结论，各100-150字。

## 23. expertSummary (String, HTML)
专家总结，80-120字。
格式：<b>💡 专家总结：</b>...等级为 <b>X</b>...

## 24. tags (Array)
财务标签，3-5个。
格式：[{"text":"维度：评价","type":"success/info/warning"}]

## 25. actionIntro (String, HTML)
行动清单概括，50-80字。

## 26. actions (Array<String>, HTML)
行动建议，3-8条。
格式：["<b>标题：</b>建议"]

## 27. executionPriority (String, HTML)
执行优先级，60-100字。

## 28. recordQuality (String, HTML)
记录质量建议，60-100字。

## 29. warningBox (String, HTML)
重要提醒，30-60字。
格式：<strong>💡 建议执行：</strong>...

## 30. treeData (Array)
消费结构树图数据，从 expenseByCategory 映射。
格式：[{"name":"分类名","value":金额}]
直接取 expenseByCategory，映射为 {name: category, value: amount}。

## 31. radar1Data (Object)
财务性格雷达图。
固定结构：
{
  "indicators": [
    {"name":"节俭 (Frugality)","max":100},
    {"name":"稳定 (Stability)","max":100},
    {"name":"多元 (Diversified)","max":100},
    {"name":"投资 (Self-Invest)","max":100},
    {"name":"安全 (Risk-Ctrl)","max":100}
  ],
  "values": [节俭分, 稳定分, 多元分, 投资分, 安全分],
  "name": "财务性格"
}
计算：节俭=savingsRate，稳定=100-波动率×200，多元=类别数×10+(1-最大占比)×50，投资=教育类占比×200，安全=100-大额占比×100

## 32. radar3Data (Object)
财务画像雷达图。
固定结构：
{
  "indicators": [
    {"name":"省钱指数","max":100},
    {"name":"支出稳定","max":100},
    {"name":"投资自己","max":100},
    {"name":"风险意识","max":100},
    {"name":"钱包深度","max":100}
  ],
  "values": [省钱分, 稳定分, 投资分, 风险分, 钱包分],
  "name": "财务画像"
}
计算：省钱=savingsRate，稳定=100-波动率×200，投资=教育健康类占比×150，风险=订阅占比评分，钱包=收入量级（≥20k=100...）

# 分析方法

1. **拿铁因子识别**：从 bills 统计同商户高频小额（如每日18元的云存储），计算年化成本
2. **时间规律**：从 bills 统计 hour 在 23-6 之间的交易，分析深夜消费
3. **消费转型**：对比 historicalData 中分类变化
4. **能力收入**：从 incomeByCategory 识别工资相关分类
5. **生活成本**：从 expenseByCategory 识别房租饮食交通

# 格式
- 金额：<b>¥123</b> 或 <b>¥12,345</b>
- 百分比：<b>32.7%</b>
- 专有名词：<b>「术语」</b>
- 商户名：「商户」

# 特殊情况
- hasIncome=false → warningBox 提醒补录收入
- transactions.largest 为空 → largeTransactionAnalysis 说明"未检测到异常大额支出"
- bills 中无高频小额 → latteFactorAnalysis 说明"未发现高频小额订阅支出"

现在请基于以下财务数据生成分析报告（只输出JSON）：
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
