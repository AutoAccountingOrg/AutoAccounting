/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package org.ezbook.server.ai.tools

import com.google.gson.Gson
import org.ezbook.server.Server
import org.ezbook.server.ai.AiManager
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.ServerLog
import org.ezbook.server.tools.runCatchingExceptCancel

class BillTool {

    private val prompt = """
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
6) Defaults: currency="CNY"; fee=0; money=0.00; empty string for optional text; time=0.
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
- time: Unix ms timestamp if full date/time is present; otherwise 0.

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
  "time": 0
}

# Examples
Input: 支付宝消费，商户：肯德基，支付金额￥36.50，账户余额...，时间2024-08-02 12:01:22
Category Data: 餐饮,交通,购物
Output:
{"accountNameFrom":"支付宝","accountNameTo":"","cateName":"餐饮","currency":"CNY","fee":0,"money":36.50,"shopItem":"","shopName":"肯德基","type":"Expend","time":0}

Input: 推广信息：本店大促销...
Category Data: 餐饮,交通
Output:
{}

""".trimIndent()

    suspend fun execute(data: String): BillInfoModel? {
        val categories = Db.get().categoryDao().all()
        val categoryNames = categories.joinToString(",") { it.name.toString() }
        val user = """
Input:
- Raw Data: 
  ```
  $data
  ```
- Category Data:
  ```
  $categoryNames
  ```      
        """.trimIndent()

        val data = AiManager.getInstance().request(prompt, user).getOrThrow()
        val bill = data.replace("```json", "").replace("```", "")
        ServerLog.d("AI分析结果: $bill")
        return runCatchingExceptCancel {
            val billInfoModel = Gson().fromJson(bill, BillInfoModel::class.java)
            if (billInfoModel.money < 0) billInfoModel.money = -billInfoModel.money
            if (billInfoModel.money == 0.0) return null
            billInfoModel
        }.onFailure {
            ServerLog.e("AI分析结果解析失败: $it", it)
        }.getOrNull()
    }
}