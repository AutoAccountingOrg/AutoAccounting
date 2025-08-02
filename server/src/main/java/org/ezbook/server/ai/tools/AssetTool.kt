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
import org.ezbook.server.ai.AiManager
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.db.Db

class AssetTool {
    private val prompt = """
# Task Description
You are an AI assistant responsible for locating assets from a provided Asset Data list.

## Objective
Given up to two input asset clues (`asset1`, `asset2`), match each against the Asset Data and **output ONLY the best-matching asset name(s)**.

---

## Instructions

1. Read the fields (they may be empty):
   - `asset1`
   - `asset2`

2. Match each clue independently against the Asset Data:
   - Use the most precise and appropriate matching logic (fuzzy matching allowed, but be confident).
   - Do not mix clues; each line corresponds to one input.

3. **Strict Output Rules**  
   - Line 1: result for `asset1`  
   - Line 2: result for `asset2`  
   - If a clue has no match, output an empty string `""` on that line.  
   - If both clues have no match, still output two lines of empty strings (second line immediately after the first, no extra text).

4. **Do NOT guess**  
   - Do not invent new asset names.  
   - Only choose from the provided Asset Data.

---

## Example Input

```json
{
  "asset1": "中国银行储蓄卡",
  "asset2": "支付宝"
}
## Example Output

中国银行
支付宝

If asset2 had no match, the correct output would be

中国银行
""

## Important
Always pick the closest and most accurate asset from the list.  
When unsure or if no match is possible, return `""` for that line—**no explanations, no extra text**.
"""".trimIndent()

    suspend fun execute(asset1: String, asset2: String): String? {
        val data = Gson().toJson(
            hashMapOf(
                "asset1" to asset1,
                "asset2" to asset2
            )
        )
        val assets = Db.get().assetsDao().load()
        val assetsNames = assets
            .filter { it.type != AssetsType.BORROWER && it.type != AssetsType.CREDITOR }
            .map { it.name.trim() }
            .distinct()                         // 去重且保持原顺序
            .joinToString(",")
        val user = """
Input:
- Raw Data: 
  ```
  $data
  ``` 
- Assets Data:
  ```
  $assetsNames
  ```     
        """.trimIndent()

        return AiManager.getInstance().request(prompt, user)
    }
}