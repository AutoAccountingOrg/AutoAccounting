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

class CategoryTool {
    private val prompt = """
# Task Description

You are an AI assistant responsible for categorizing transactions based on provided transaction details.

## Objective
Given an input containing `ruleName`,`shopName` and `shopItem`, match them against a provided Category Data and **output ONLY the matching category name**.

---

## Instructions

1. Read the fields:
   - `shopName`
   - `shopItem`
   - `ruleName`

2. Match these details with the provided Category Data:
   - Use either `shopName` or `shopItem` or `ruleName` for matching.
   - Prefer exact or most appropriate match based on available information.

3. **Strict Output Rule**:
   - Output **ONLY** the matching category name.
   - **No explanations, no extra text, no JSON formatting, no comments.**
   - If no match can be confidently determined, output an empty string `""`.

4. **Do Not Guess**:
   - Do not create or invent new category names.
   - Only select from the given Category Data.

---

## Example Input

```json
{
    "shopName": "钱塘江超市",
    "shopItem": "上好佳薯片",
    "ruleName: "支付宝红包"
}

## Example Output

```
购物
```
## Important
Always match to the closest and most accurate category from the provided list.

If unsure or if no match is possible, return "" (an empty string) without explanation.
"""".trimIndent()

    suspend fun execute(data: String): String? {
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