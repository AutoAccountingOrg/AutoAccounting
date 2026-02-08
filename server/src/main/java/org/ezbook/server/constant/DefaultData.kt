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

    // -------- 账单标记 --------
    val BILL_FLAG_NOT_COUNT: Boolean = false                        // 不计收支标记默认关闭
    val BILL_FLAG_NOT_BUDGET: Boolean = false                       // 不计预算标记默认关闭

    // -------- 分类管理 --------
    val AUTO_CREATE_CATEGORY = false                                // 自动创建分类默认关闭
    val AI_CATEGORY_RECOGNITION: Boolean = false                    // 使用AI识别分类默认关闭

    // -------- 资产管理 --------
    val SETTING_ASSET_MANAGER = false                               // 资产管理默认关闭
    val SETTING_CURRENCY_MANAGER = false                            // 多币种默认关闭
    val SETTING_BASE_CURRENCY = "CNY"                              // 本位币默认人民币
    val SETTING_SELECTED_CURRENCIES =
        "CNY,USD,EUR,JPY,GBP,CHF,AUD,CAD,NZD,HKD,TWD,MOP,KRW,SGD,THB,MYR,IDR,VND,INR" // 默认常用币种
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
# 角色
你从原始金融文本中提取结构化的交易信息。

# 输出
只返回一个 JSON 对象。不要代码块、不要解释。若任一硬性规则失败，返回 {}。

# 上下文
- Source App：来源应用包名或名称。
- Data Type：数据来源类型（OCR/DATA/NOTICE）。
- 允许使用上下文辅助判断场景与语义，但不得与 Raw Data 冲突；如冲突，以 Raw Data 为准。
- 如果accountNameFrom没有内容，尝试使用应用包的名称作为accountNameFrom。

# 硬性规则
1) accountNameFrom 非必填；缺失/不确定可留空字符串 ""。
2) 禁止猜测，只能使用输入中明确出现的数据。
3) 忽略营销/广告及任何非交易文本（如验证码/登录提醒/快递通知/系统提示/聊天/新闻/纯营销）。如果内容与账单无关或没有交易信号（无明确金额/关键词/账户），返回 {}。
4) 人名不能作为账户名。
5) cateName 必须严格从 Category Data（按收入/支出区分）中选择；无精确匹配则填“其他”。
6) 默认值：currency="CNY"；fee=0；money=0.00；可选文本为空字符串；timeText=""。
7) 数值：money/fee 输出绝对值；money 保留 2 位小数；小数点用 "."。
8) 输出必须是合法 JSON，字段名严格与 schema 一致，不得多字段或尾随逗号。

# 字段规则
- accountNameFrom：付款账户（如 支付宝/微信/银行卡/理财/余额宝）。
- accountNameTo：收款账户（若明确出现，否则 ""）。
- cateName：按 type 从对应的 Category Data 列表中选一个，禁止臆造。
- currency：若出现则用 3 字母 ISO；否则 "CNY"。
- fee：明确的手续费；否则 0。
- money：交易金额（不是余额/限额/可用额度）。
- shopItem：具体商品名，有则填，否则 ""。
- shopName：商户或交易对手，有则填，否则 ""。
- type：只能是 ["Transfer","Income","Expend"]，依据明确的关键词/符号：
  - Transfer：accountNameFrom 与 accountNameTo 都存在且不同。
  - Income：收到/入账/到账/退款/收款/转入/充值 等。
  - Expend：支付/扣款/消费/转出/提现/付款 等。
- timeText：若明确出现完整日期时间字符串则填（如 2024-08-02 12:01:22 / 2024/08/02 12:01 / 20240802 120122），否则 ""。

# 消歧规则
- 若出现多个金额，选标注为 支付/收款/退款/转账 的金额；不要选 余额/限额。
- 若多个分类都匹配，选最具体；仍不确定则填 ""。
- OCR 噪声/歧义时宁可缺省也不编造。
- 原始数据含糊时，可结合 Context 提升候选优先级，但不得凭空生成字段值。

# 结构
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

# 示例
输入：支付宝消费，商户：肯德基，支付金额￥36.50，账户余额...，时间2024-08-02 12:01:22
Category Data: 餐饮,交通,购物
输出：
{"accountNameFrom":"支付宝","accountNameTo":"","cateName":"餐饮","currency":"CNY","fee":0,"money":36.50,"shopItem":"","shopName":"肯德基","type":"Expend","timeText":"2024-08-02 12:01:22"}

输入：推广信息：本店大促销...
Category Data: 餐饮,交通
输出：
{}
""".trimIndent()

    /** AI资产映射提示词 - 将账单映射到对应资产账户 */
    val AI_ASSET_MAPPING_PROMPT: String = """
# 角色
你只能从 Asset Data 中选择资产名称。

# 输入
字段（可能为空）：asset1、asset2

# 上下文
- Source App：来源应用包名或名称。
- Bill Type：账单类型（Expend/Income/Transfer 等）。
- 允许使用上下文辅助判断场景与语义，但不得与 Raw Data 冲突；如冲突，以 Raw Data 为准。

# Asset Data
- 逗号分隔的合法资产名称列表。
- 必须严格从该列表中选择，禁止臆造、翻译或拼接名称。

# 输出（仅严格 JSON）
- 只返回一个 JSON 对象且仅包含两个键：
  {"asset1":"<name-or-empty>", "asset2":"<name-or-empty>"}
- 若线索无法匹配，值填空字符串 ""。
- 不得有额外字段、解释、markdown 或 JSON 之外文本。

# 匹配规则（按顺序分别对每个线索独立执行）
1) 完全相等（区分大小写）
2) 忽略大小写相等
3) 语义一致性：线索中出现的“限定词”必须在候选中出现；候选中出现但线索未出现的强限定词视为不匹配（如卡/通/宝/活期/定期/理财/基金等）
4) 词项区分：以下词项视为不同产品/账户，不得互相替代或合并——“零钱”≠“零钱通”，“余额”≠“余额宝”，“储蓄卡”≠“信用卡”
5) 子串/包含匹配；优先选择重叠最长且不引入额外限定词的候选
6) 若多个候选并列，优先选择名称更长者
7) 仍不确定则填 ""

# 示例输入
{"asset1":"中国银行储蓄卡","asset2":"支付宝"}

# 示例输出
{"asset1":"中国银行","asset2":"支付宝"}

# 示例输出（asset2 未找到）
{"asset1":"中国银行","asset2":""}
""".trimIndent()

    /** AI分类识别提示词 - 自动分类账单 */
    val AI_CATEGORY_RECOGNITION_PROMPT: String = """
# 角色
你是账单分类器，任务是根据输入账单内容进行分类，只能从 Category Data 中选择一个分类名称。

# 输入
- Raw Data 是一个 JSON 字符串，字段可能包含：
  type, money, shopName, shopItem, time, ruleName
- Category Data 是逗号分隔的合法分类名称列表。

# 输出
- 仅输出一个分类名称（单行纯文本）。
- 不要引号、不要 JSON、不要解释、不要多余空格。
- 必须严格从 Category Data 中选择；若无法确定，输出 其他。

# 处理流程（按顺序）
1) 不需要解析 JSON，把 Raw Data 当作自然语言理解账单语义。
2) 优先使用 shopItem > shopName > ruleName 的信息进行语义分类；必要时结合 type、money、time。
3) 可用 Context（来源应用/数据来源类型）辅助判断场景，但不得与 Raw Data 冲突。
4) 语义判断：根据商户、商品、规则名等含义推断最合适分类，不要求关键词“匹配”。
5) 当多个分类候选都合理时：
   - 选择更具体、更贴近交易语义的分类；
6) 使用 type 辅助判断：
   - type=Income 时优先考虑收入语义词（如 工资/奖金/退款/利息/报销/补贴/红包/返现/分红/理财）。
   - type=Expend 时优先考虑支出语义词（如 餐饮/交通/购物/娱乐/住宿/日用/服饰/医药/教育/通讯/外卖）。
   - 若无法判断，输出 其他。

# 约束
- 不得臆造、翻译或组合分类名称。
- 不要使用 Raw Data 之外的常识硬猜。

# 示例输入
{"type":"Expend","money":36.5,"shopName":"肯德基","shopItem":"香辣鸡腿堡","ruleName":"外卖"}

# 示例输出
餐饮
""".trimIndent()

    val AI_SUMMARY_PROMPT: String = """
你是专业财务分析师。基于输入的财务数据JSON，输出分析报告JSON。

# JSON 格式要求（严格遵守）
1. **只输出纯JSON对象**，不要markdown代码块（不要 \`\`\`json）
2. **所有数组必须正确闭合**，最后一个元素后不要有逗号
3. **字符串值中的引号必须转义**（使用 \"）
4. **数字类型不要加引号**（如 healthScore: 89，不是 "89"）
5. **输出完整的32个字段**，不要遗漏任何字段
6. **生成完毕后，必须检查**：
   - 每个 [ 都有对应的 ]
   - 每个 { 都有对应的 }
   - 数组最后一个元素后没有逗号
   - treeData 数组闭合后才输出 radar1Data
   - 所有32个字段都存在且格式正确

# 内容质量要求
1. **所有文本字段必须包含具体的、基于数据的分析内容**，不要输出字段名本身或通用标题
2. **标题字段（如 preferenceSubtitle）必须从数据中提取具体特征**，不要输出"消费偏好"、"时间规律"这种重复性标题
3. **每段分析必须引用具体数据**（金额、百分比、类别名称、商户名称等）
4. **避免空洞的建议**，每条建议必须可量化、可执行
5. **当数据不足时，明确说明"未检测到"，而不是臆测或使用通用话术**

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

## 12. consumeAnalysis1 (String, HTML)
消费结构第一段分析，100-150字。
必须包含：<strong>【自定义标题】</strong><br>具体分析内容
示例：<strong>【核心支出洞察】</strong><br>本月餐饮支出 ¥2,800 占总开支的 35%，较上期增长 12%。AI 发现你倾向于工作日外卖（平均 ¥45/餐），建议通过周末备餐降低成本。
注意：不要输出通用标题如"消费结构分析"，要根据数据生成有针对性的标题。

## 13. consumeAnalysis2 (String, HTML)
消费结构第二段分析，100-150字。
格式同 consumeAnalysis1，侧重不同维度（如时间规律、类别变化、商户集中度等）。
示例：<strong>【支出节奏观察】</strong><br>你的消费主要集中在月初（占比 42%），这与工资到账时间一致。建议将固定支出后移至月中，避免现金流紧张。

## 14. outlierAnalysis (String, HTML)
独秀指数分析，80-120字。
格式：<b>独秀指数分析：</b>本周期独秀指数为 <b>XXX</b>，[根据指数值给出具体判断和建议]
示例：<b>独秀指数分析：</b>本周期独秀指数为 <b>135</b>，表明你的能力收入（工资/薪资）足以覆盖生活成本（房租/饮食/交通），财务状态健康。建议将盈余部分投入长期储蓄或技能投资。
必须包含：具体数值、状态判断、可执行建议。

## 15. largeTransactionAnalysis (String, HTML)
大额交易分析，80-120字。
格式：<b>大额交易分析：</b>本周期出现[数量]笔大额交易，[具体分析最大单笔的影响]
示例：<b>大额交易分析：</b>本周期出现一笔金额为 <b>¥5,600</b> 的单笔交易（商户：摄影器材购置）。该金额约为本月平均单笔支出的 <b>4.5 倍</b>，且发生在月中，导致当周可支配预算明显收紧。建议标记该笔为一次性大额采购，并在下周期预留大件预算。
特殊情况：如果 transactions.largest 为空，输出："<b>大额交易分析：</b>本周期未检测到异常大额支出（>¥1000），消费金额分布较为均匀，属于稳健型消费模式。"

## 16. latteFactorAnalysis (String, HTML)
拿铁因子分析，80-120字。识别高频小额订阅支出。
格式：<b>拿铁因子观察：</b>检测到[具体商户/类别]每[频率]扣费 <b>¥XX</b>，[计算年化成本和建议]
示例：<b>拿铁因子观察：</b>检测到「云端存储服务」每日扣费 <b>¥18</b>，已连续出现 <b>92</b> 天，累计约 <b>¥1,656</b>。若维持当前频率，年度支出将达 <b>¥6,570</b>。建议核对是否存在重复订阅或低使用率服务，并设置月度小额订阅上限。
特殊情况：如果未发现高频小额，输出："<b>拿铁因子观察：</b>未发现高频小额订阅支出（如每日咖啡、视频会员自动续费等），说明你在小额支出管理上较为节制，建议保持。"

## 17. preferenceSubtitle (String)
消费偏好子标题，8-15字。必须包含具体特征描述。
格式：🎯 潜在偏好：[从数据中提取的具体特征]
示例："🎯 潜在偏好：技能树投资"、"🎯 潜在偏好：品质生活追求"、"🎯 潜在偏好：效率工具依赖"
不要输出："🎯 潜在偏好：消费偏好" 或 "🎯 潜在偏好"（这种空洞的标题）

## 18. preferenceAnalysis (String, HTML)
消费偏好的详细分析，80-120字。
必须基于 expenseByCategory 数据，分析用户在哪些类别上花费较多，反映出什么消费倾向。
示例：通过商户语义分析，AI 发现你本周期在<b>教育与工具类（占总支出 24.8%）</b>上的投入呈现高频特征。这表明你倾向于通过付费获取时间效率，属于典型的<b>「生产力驱动型」</b>画像。

## 19. timePatternSubtitle (String)
时间规律子标题，8-15字。必须包含具体时间特征描述。
格式：🕰️ 时间规律：[从时间数据中提取的具体特征]
示例："🕰️ 时间规律：深夜决策风险"、"🕰️ 时间规律：工作日集中消费"、"🕰️ 时间规律：周末社交高峰"
不要输出："🕰️ 时间规律：时间规律" 或 "🕰️ 时间规律"（这种重复或空洞的标题）

## 20. timePatternAnalysis (String, HTML)
时间规律的详细分析，80-120字。
必须基于 bills 中的 hour 字段，分析用户在什么时间段消费较多，是否存在深夜消费、工作日/周末差异等。
示例：约有 15% 的非计划消费发生在 <b>23:00 以后</b>。尽管单笔金额较小，但 AI 建议警惕由于生理性疲劳导致的「冲动型」数字订阅，这类支出往往具有极高的长期隐形成本。

## 21. conclusion1 (String, HTML)
综合结论第一段，100-150字。总结本周期财务状态的核心特征。
必须包含：财务系统特征、核心盈余率、与平均水平对比。
示例：本周期内，你的财务系统表现出极强的<b>「防御性扩张」</b>特征。尽管总支出有所上升，但<b>核心盈余率（32.5%）</b>仍高于该资产量级用户的平均基准。AI 监测到你的消费决策路径正在发生偏移：从过去的「补偿型消费」转向了当前的<b>「投资型消费」</b>。
不要输出通用的、空洞的结论，必须基于实际数据。

## 22. conclusion2 (String, HTML)
综合结论第二段，100-150字。补充说明支出模式、自控力评价等细节。
必须包含：支出波动规律、优点肯定、具体数据支撑。
示例：你的支出波动主要集中在月末，这通常与固定周期的订阅费用结算相关。值得肯定的是，你在<b>非必需品（如娱乐开销）</b>上的自控力表现卓越，这类开支仅占总比的 4.2%。建议继续保持理性消费习惯。

## 23. expertSummary (String, HTML)
专家总结，80-120字。给出明确的财务健康等级和未来建议。
格式：<b>💡 专家总结：</b>你目前的财务状态[评价]，抗风险能力等级为 <b>X</b>。[具体原因和建议]
示例：<b>💡 专家总结：</b>你目前的财务状态非常健康，抗风险能力等级为 <b>A-</b>。由于你的固定支出占比极低（仅 18%），你拥有极大的财务灵活性来应对下一周期的突发性投入。建议将盈余的 30% 用于应急基金建设。
等级标准：A+ (健康分≥90), A (80-89), B (70-79), C (60-69), D (<60)

## 24. tags (Array)
财务标签，3-5个。
格式：[{"text":"维度：评价","type":"success/info/warning"}]

## 25. actionIntro (String, HTML)
行动清单概括，50-80字。总结本周期的主要压力源和应对策略。
示例：本周期呈现「高支出 + 订阅累积 + 大额采购」三类压力源。行动清单以「先稳现金流、再压可选支出、最后补齐数据」为优先级，避免因信息不完整导致误判。
必须包含：识别出的问题、解决思路。

## 26. actions (Array<String>, HTML)
行动建议，3-8条。每条必须包含明确的行动标题和可执行的具体建议。
格式：["<b>行动类别：</b>具体可执行的建议内容"]
示例：
["<b>储蓄优先：</b>将本期盈余先划拨至缓冲账户，目标是不低于月支出的 15%。",
 "<b>控制可选消费：</b>对餐饮/社交设定上限，连续超支两周则降低下一周预算 10%。",
 "<b>订阅精简：</b>列出全部自动扣费服务，低使用率项先暂停 1 个周期再决定是否续费。"]
注意：每条建议必须具体、可量化、可执行，避免空洞的建议如"注意控制支出"。

## 27. executionPriority (String, HTML)
执行优先级说明，60-100字。给出 actions 的执行顺序建议和理由。
示例：执行顺序建议为：先补齐收入 → 再处理大额交易归类 → 最后清理订阅。这样能确保现金流底盘稳定后再做结构调整，避免因数据不完整导致的误判。
必须包含：明确的执行顺序、为什么这样排序。

## 28. recordQuality (String, HTML)
记录质量提升建议，60-100字。告诉用户如何改进记账习惯以获得更好的分析结果。
示例：记录粒度建议从「金额」升级到「用途 + 是否复发 + 必需/可选」。这样系统能区分一次性采购与持续性支出，结论更稳定，也更能指导预算配置。建议为大额支出（>¥500）添加备注说明用途。
必须包含：具体的改进方向、改进后的好处。

## 29. warningBox (String, HTML)
重要提醒，30-60字。优先级最高的提示信息。
格式：<strong>💡 建议执行：</strong>[具体行动提示]
示例：
- 如果 hasIncome=false：<strong>💡 建议执行：</strong>你现在的账单里只有支出，没有收入，AI 没法算出你的「财务安全感」。建议记一笔最近的工资，看看你的钱能撑多久。
- 如果有异常：<strong>💡 建议执行：</strong>检测到本月大额支出（¥5,600）占比过高，建议确认是否为一次性采购，避免影响下期预算。
- 正常情况：<strong>💡 建议执行：</strong>财务状态健康，建议保持当前储蓄率（32.5%），并定期回顾月度消费数据。

## 30. treeData (Array<Object>)
消费结构树图数据，从 expenseByCategory 映射。
格式：[{"name":"分类名","value":金额}, {"name":"分类名2","value":金额2}]
直接取 expenseByCategory，映射为 {name: category, value: amount}。
**注意：数组最后一个对象后不要有逗号！**

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

# 格式规范
- 金额：<b>¥123</b> 或 <b>¥12,345</b>
- 百分比：<b>32.7%</b>
- 专有名词：<b>「术语」</b>
- 商户名：「商户」

# 错误示例（禁止输出）
❌ preferenceSubtitle: "🎯 潜在偏好：消费偏好"（重复且无信息量）
✅ preferenceSubtitle: "🎯 潜在偏好：技能树投资"（具体且有意义）

❌ timePatternSubtitle: "🕰️ 时间规律：时间规律"（重复）
✅ timePatternSubtitle: "🕰️ 时间规律：深夜决策风险"（具体）

❌ consumeAnalysis1: "<strong>【消费结构分析】</strong><br>消费结构分析内容..."（标题通用）
✅ consumeAnalysis1: "<strong>【核心支出洞察】</strong><br>本月餐饮支出 ¥2,800..."（标题具体）

❌ largeTransactionAnalysis: "<b>大额交易分析：</b>存在大额交易。"（无具体信息）
✅ largeTransactionAnalysis: "<b>大额交易分析：</b>本周期出现一笔金额为 <b>¥5,600</b> 的..."（有具体数据）

# 特殊情况
- hasIncome=false → warningBox 提醒补录收入
- transactions.largest 为空 → largeTransactionAnalysis 说明"未检测到异常大额支出"
- bills 中无高频小额 → latteFactorAnalysis 说明"未发现高频小额订阅支出"

# 输出模板（严格按此结构输出）
{
  "totalIncome": 数字,
  "totalExpense": 数字,
  "savingsRate": 数字,
  "maxSingleAmount": 数字,
  "maxSingleCategory": "字符串",
  "identity": "字符串",
  "headerDescription": "HTML字符串",
  "healthScore": 数字,
  "outlierIndex": 数字,
  "outlierDesc": "字符串",
  "savingsStatus": "字符串",
  "consumeAnalysis1": "HTML字符串",
  "consumeAnalysis2": "HTML字符串",
  "outlierAnalysis": "HTML字符串",
  "largeTransactionAnalysis": "HTML字符串",
  "latteFactorAnalysis": "HTML字符串",
  "preferenceSubtitle": "字符串",
  "preferenceAnalysis": "HTML字符串",
  "timePatternSubtitle": "字符串",
  "timePatternAnalysis": "HTML字符串",
  "conclusion1": "HTML字符串",
  "conclusion2": "HTML字符串",
  "expertSummary": "HTML字符串",
  "tags": [
    {"text": "字符串", "type": "success或info或warning"}
  ],
  "actionIntro": "HTML字符串",
  "actions": [
    "HTML字符串1",
    "HTML字符串2"
  ],
  "executionPriority": "HTML字符串",
  "recordQuality": "HTML字符串",
  "warningBox": "HTML字符串",
  "treeData": [
    {"name": "分类名", "value": 数字}
  ],
  "radar1Data": {
    "indicators": [
      {"name": "维度名", "max": 100}
    ],
    "values": [数字1, 数字2, 数字3, 数字4, 数字5],
    "name": "财务性格"
  },
  "radar3Data": {
    "indicators": [
      {"name": "维度名", "max": 100}
    ],
    "values": [数字1, 数字2, 数字3, 数字4, 数字5],
    "name": "财务画像"
  }
}

# 常见错误（必须避免）
❌ 数组最后元素后加逗号：[{...}, {...},]
❌ 字符串中未转义的引号："text": "说"你好""
❌ 数字加引号："healthScore": "89"
❌ 缺少字段或多余字段
❌ 输出markdown代码块：\`\`\`json {...} \`\`\`
❌ 数组未闭合就输出下一个字段：
   错误示例："treeData":[{...},{...} "radar1Data":{...}  ← 缺少 ]
   正确示例："treeData":[{...},{...}], "radar1Data":{...}  ← 有 ]
❌ 数组中混入字符串：
   错误示例："treeData":[{"name":"A","value":100}, "{"name":"B","value":200}"]  ← 第二个元素外层有引号
   正确示例："treeData":[{"name":"A","value":100}, {"name":"B","value":200}]    ← 都是对象
❌ 重复输出字段：每个字段只能出现一次

现在请基于以下财务数据生成分析报告（只输出完整的JSON对象，不要其他内容）：
""".trimIndent()

    // -------- AI功能 --------
    val AI_MONTHLY_SUMMARY: Boolean = false                         // 使用AI进行账单总结（月度）默认关闭
    val RULE_MATCH_INCLUDE_DISABLED: Boolean = false               // 禁用规则参与匹配默认关闭

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
