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

package net.ankio.demo

import net.ankio.auto.sdk.model.AssetModel
import net.ankio.auto.sdk.model.BillModel
import net.ankio.common.config.AccountingConfig
import net.ankio.common.model.BookModel
import net.ankio.common.model.CategoryModel
import kotlin.random.Random

object MockUtils {
    fun randomImage(): String {
        val images = arrayOf(
            "https://cdn.jsdelivr.net/gh/cellier/bank-icon-cn/png/72/交通银行%403x.png",
            "https://cdn.jsdelivr.net/gh/cellier/bank-icon-cn/png/72/兴业银行%403x.png",
            "https://cdn.jsdelivr.net/gh/cellier/bank-icon-cn/png/72/北京银行%403x.png",
            "data:image/png;base64,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",
        )
        return images.random()
    }

     fun generateRandomBooks(numBooks: Int = 5): List<BookModel> {
        return List(numBooks) {
            BookModel(
                name = List(10) { Random.nextInt('a'.code, 'z'.code).toChar() }.joinToString(""),
                icon = randomImage(),
                category = generateRandomCategories(Random.nextInt(30,60))
            )
        }
    }

    fun generateRandomCategories(numCategories: Int = 5): List<CategoryModel> {
        val categoryNames = arrayOf("食品", "交通", "娱乐", "住宿", "其他")
        val categoryTypes = arrayOf(0, 1)

        return List(numCategories) {
            val type = if (it % 2 == 0) categoryTypes.random() else 0
            CategoryModel(
                name = categoryNames.random(),
                icon = randomImage(),
                type = type,
                sort = it,
                id = it.toString(),
                parent = if (it % 2 == 0) "-1" else (it - 1).toString(), // 偶数ID的分类没有父分类，奇数ID的分类的父分类是前一个分类
            )
        }
    }

    fun generateRandomBills(numBills: Int = 5): List<BillModel> {
        return List(numBills) {
            BillModel(
                amount = Random.nextDouble(1.0, 100.0),
                time = System.currentTimeMillis(),
                remark = List(10) { Random.nextInt('a'.code, 'z'.code).toChar() }.joinToString(""),
                id = it,
                type = Random.nextInt(0, 5)
            )
        }
    }

    fun generateAssets(numAssets:Int = 5):List<AssetModel>{
        return List(numAssets){
            AssetModel(
                name = List(10) { Random.nextInt('a'.code, 'z'.code).toChar() }.joinToString(""),
                icon = randomImage(),
                sort = Random.nextInt(1, 100)
            )
        }
    }

    fun generateRandomConfig():AccountingConfig {
        return AccountingConfig().apply {
           assetManagement = true
            multiCurrency = true
            reimbursement = true
            multiBooks = true
            lending = true
            fee = true
        }
    }


}