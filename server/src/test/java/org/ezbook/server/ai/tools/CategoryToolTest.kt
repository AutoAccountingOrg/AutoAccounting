/*
 * Copyright (C) 2026 ankio
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

import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.CategoryModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryToolTest {

    @Test
    fun categoryType_mapsBillSubtypesToStoredCategoryTypes() {
        BillType.entries
            .filter { it.name.startsWith(BillType.Expend.name) }
            .forEach { assertEquals(BillType.Expend, CategoryTool.categoryType(it)) }
        BillType.entries
            .filter { it.name.startsWith(BillType.Income.name) }
            .forEach { assertEquals(BillType.Income, CategoryTool.categoryType(it)) }
        assertNull(CategoryTool.categoryType(BillType.Transfer))
    }

    @Test
    fun categoryNames_removesBlankAndDuplicateNames() {
        val categories = listOf(
            category(" 通勤 "),
            category(""),
            category(null),
            category("通勤"),
            category("其它")
        )

        assertEquals(listOf("通勤", "其它"), CategoryTool.categoryNames(categories))
    }

    @Test
    fun categoryPaths_preservesHierarchyAndDuplicateChildNames() {
        val commute = parent("通勤", "parent-commute")
        val travel = parent("旅行", "parent-travel")
        val commuteTaxi = child("打车", "child-commute-taxi", commute.remoteId)
        val travelTaxi = child("打车", "child-travel-taxi", travel.remoteId)

        assertEquals(
            listOf("通勤", "旅行", "通勤 - 打车", "旅行 - 打车"),
            CategoryTool.categoryPaths(listOf(commute, travel, commuteTaxi, travelTaxi))
        )
    }

    @Test
    fun selectCategory_acceptsOnlyExistingCategory() {
        val categories = listOf("通勤", "巴士", "地铁", "其它")

        assertEquals("地铁", CategoryTool.selectCategory(" 地铁\n", categories))
        assertNull(CategoryTool.selectCategory("公交地铁", categories))
        assertNull(CategoryTool.selectCategory("分类：地铁", categories))
    }

    @Test
    fun categoryExists_validatesSingleAndParentChildCategories() {
        val parent = category("通勤").apply {
            remoteId = "parent-commute"
            remoteParentId = "-1"
        }
        val child = category("地铁").apply {
            remoteId = "child-metro"
            remoteParentId = "parent-commute"
        }
        val categories = listOf(parent, child, category("其它"))

        assertEquals(true, CategoryTool.categoryExists("通勤", categories))
        assertEquals(true, CategoryTool.categoryExists("通勤 - 地铁", categories))
        assertEquals(false, CategoryTool.categoryExists("地铁", categories))
        assertEquals(false, CategoryTool.categoryExists("公交地铁", categories))
        assertEquals(false, CategoryTool.categoryExists("日常 - 地铁", categories))
    }

    @Test
    fun resolveCategoryPath_normalizesOnlyUnambiguousChildNames() {
        val commute = parent("通勤", "parent-commute")
        val travel = parent("旅行", "parent-travel")
        val metro = child("地铁", "child-metro", commute.remoteId)
        val commuteTaxi = child("打车", "child-commute-taxi", commute.remoteId)
        val travelTaxi = child("打车", "child-travel-taxi", travel.remoteId)
        val categories = listOf(commute, travel, metro, commuteTaxi, travelTaxi)

        assertEquals("通勤", CategoryTool.resolveCategoryPath("通勤", categories))
        assertEquals("通勤 - 地铁", CategoryTool.resolveCategoryPath("地铁", categories))
        assertEquals("旅行 - 打车", CategoryTool.resolveCategoryPath("旅行 - 打车", categories))
        assertNull(CategoryTool.resolveCategoryPath("打车", categories))
        assertNull(CategoryTool.resolveCategoryPath("公交", categories))
    }

    @Test
    fun fallbackCategory_usesExistingOtherCategory() {
        assertEquals("其它", CategoryTool.fallbackCategory(listOf("通勤", "其它")))
        assertEquals("其他", CategoryTool.fallbackCategory(listOf("通勤", "其他")))
        assertNull(CategoryTool.fallbackCategory(listOf("通勤", "地铁")))
        assertNull(CategoryTool.fallbackCategory(emptyList()))
    }

    private fun category(name: String?) = CategoryModel().apply {
        this.name = name
    }

    private fun parent(name: String, remoteId: String) = category(name).apply {
        this.remoteId = remoteId
        remoteParentId = "-1"
    }

    private fun child(name: String, remoteId: String, parentRemoteId: String) = category(name).apply {
        this.remoteId = remoteId
        remoteParentId = parentRemoteId
    }
}
