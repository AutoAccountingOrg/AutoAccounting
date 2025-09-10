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

package org.ezbook.server.tools

import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel

/**
 * 分类处理工具
 *
 * 职责：
 * 1) 基于分类映射表对分类名进行字符串替换（长度降序，避免短词覆盖长词）
 * 2) 统一输出格式为："父类 - 子类"；若无子类则返回父类本身
 * 3) 从分类名中解析父/子分类；若只有子类，通过映射后的结果获取父类
 */
class CategoryProcessor {

    suspend fun setCategoryMap(billInfoModel: BillInfoModel) {
        // 1) 先做字符串映射替换（长度降序），不改变格式
        billInfoModel.cateName = mapCategory(billInfoModel.cateName)

        // 2) 若当前没有子类，尝试从分类表判断它其实是子类
        val (parent, child) = billInfoModel.categoryPair()
        if (child.isEmpty()) {
            val typeName = billInfoModel.type.name
            // 按名称+类型查询（book 传 null，避免因 remoteBookId 不可得而漏判）
            val model = runCatchingExceptCancel {
                val book =
                    org.ezbook.server.server.resolveBookByNameOrDefault(billInfoModel.bookName)
                Db.get().categoryDao().getByName(book.remoteId, typeName, parent)
            }.getOrNull()

            if (model != null && model.isChild()) {
                // 直接按 remoteId 查询父类，避免全表扫描
                val parentModel = runCatchingExceptCancel {
                    Db.get().categoryDao().getByRemoteId(model.remoteParentId)
                }.getOrNull()
                val parentName = parentModel?.name?.trim().orEmpty()
                val childName = model.name?.trim().orEmpty()
                if (parentName.isNotEmpty() && childName.isNotEmpty()) {
                    billInfoModel.cateName = "$parentName - $childName"
                }
            }
        }
    }

    /**
     * 应用分类映射。
     * 规则：仅当原始分类名与映射项的 `name` 完全相等时，才替换为 `mapName`；
     * 不进行子串替换，避免误伤（如 "外卖" 不应影响 "外卖红包"）。
     * @param original 原始分类名
     */
    private suspend fun mapCategory(original: String): String {
        if (original.isEmpty()) return original

        val mappings = runCatchingExceptCancel { Db.get().categoryMapDao().loadWithoutLimit() }
            .getOrNull()
            .orEmpty()

        if (mappings.isEmpty()) return original

        // 仅精确匹配；找到第一条匹配即返回映射结果，否则返回原始值
        val matched =
            mappings.firstOrNull { it.name.isNotEmpty() && it.mapName.isNotEmpty() && it.name == original }
        return matched?.mapName ?: original
    }

}


