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

import org.ezbook.server.ai.AiManager
import org.ezbook.server.db.Db
import org.ezbook.server.db.dao.CategoryDao
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.Category

class BillTool : BaseAiTool {

    private val prompt = """
# Task Description

You are an AI assistant specialized in structured information extraction from transaction-related raw data.

## Objective
Extract and output a structured JSON object containing **ONLY** the fields listed below.

---

## Absolute Rules

1. **"accountNameFrom" is MANDATORY**  
   - If missing or unextractable, **immediately return `{}`** without outputting anything else.

2. **No Guessing or Inferring**  
   - Only extract data **explicitly** appearing in the raw text.
   - **Do NOT** generate, assume, or deduce any missing or ambiguous information.

3. **Reject Promotional Content**  
   - Ignore advertisements, promotions, marketing texts — do not extract from them.

4. **People's Names are NOT valid account names**  
   - Do not treat human names as `accountNameFrom` or `accountNameTo`.

5. **Category Matching**  
   - `cateName` must be selected **strictly** from a given Category Data. No custom or improvised categories.

6. **Default Values**  
   - If `currency` is missing, set it to `"CNY"`.
   - If `fee` is missing, set it to `0`.
   - If `money` is missing, set it to `0.00`.
   - Empty string `""` for any non-present optional fields.

---

## Output Schema

```json
{
   "accountNameFrom": "",  // (Optional) Source account
   "accountNameTo": "",    // (Optional) Destination account
   "cateName": "",         // (REQUIRED) Category (must match Category Data)
   "currency": "",         // (Optional) Currency, default "CNY"
   "fee": 0,               // (Optional) Transaction fee, default 0
   "money": 0.00,          // (REQUIRED) Transaction amount, default 0.00
   "shopItem": "",         // (Optional) Specific item purchased
   "shopName": "",         // (Optional) Merchant name
   "type": "",             // (Optional) Must be one of ["Transfer", "Income", "Expend"]
   "time": 0               // (Optional) Unix timestamp in milliseconds
}

"""".trimIndent()

    override suspend fun execute(data: String): String? {
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

        return AiManager.getInstance().request(prompt, user)
    }
}