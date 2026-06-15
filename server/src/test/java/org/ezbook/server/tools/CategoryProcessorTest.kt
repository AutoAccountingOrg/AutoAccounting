/*
 * Copyright (C) 2026 ankio
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ezbook.server.tools

import kotlinx.coroutines.runBlocking
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.db.model.CategoryMapModel
import org.ezbook.server.db.model.CategoryModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryProcessorTest {

    @Test
    fun resolveMappedCategory_keepsDuplicateChildNamesAmbiguous() = runBlocking {
        val bill = BillInfoModel().apply { cateName = "打车" }

        val resolved = resolveMappedCategory(bill, duplicateTaxiCategories(), processor())

        assertNull(resolved)
        assertEquals("打车", bill.cateName)
    }

    @Test
    fun resolveMappedCategory_expandsOnlyUniqueChildNames() = runBlocking {
        val commute = parent("通勤", "parent-commute")
        val metro = child("地铁", "child-metro", commute.remoteId)
        val bill = BillInfoModel().apply { cateName = "地铁" }

        val resolved = resolveMappedCategory(bill, listOf(commute, metro), processor())

        assertEquals("通勤 - 地铁", resolved)
        assertEquals("通勤 - 地铁", bill.cateName)
    }

    @Test
    fun resolveMappedCategory_appliesExplicitMappingBeforeUniqueResolution() = runBlocking {
        val commute = parent("通勤", "parent-commute")
        val travel = parent("旅行", "parent-travel")
        val commuteTaxi = child("打车", "child-commute-taxi", commute.remoteId)
        val travelTaxi = child("打车", "child-travel-taxi", travel.remoteId)
        val bill = BillInfoModel().apply { cateName = "出租车" }
        val mapping = CategoryMapModel().apply {
            name = "出租车"
            mapName = "旅行 - 打车"
        }

        val resolved = resolveMappedCategory(
            bill,
            listOf(commute, travel, commuteTaxi, travelTaxi),
            processor(listOf(mapping))
        )

        assertEquals("旅行 - 打车", resolved)
        assertEquals("旅行 - 打车", bill.cateName)
    }

    private fun processor(mappings: List<CategoryMapModel> = emptyList()) =
        CategoryProcessor { mappings }

    private fun duplicateTaxiCategories(): List<CategoryModel> {
        val commute = parent("通勤", "parent-commute")
        val travel = parent("旅行", "parent-travel")
        return listOf(
            commute,
            travel,
            child("打车", "child-commute-taxi", commute.remoteId),
            child("打车", "child-travel-taxi", travel.remoteId)
        )
    }

    private fun category(name: String) = CategoryModel().apply {
        this.name = name
    }

    private fun parent(name: String, remoteId: String) = category(name).apply {
        this.remoteId = remoteId
        remoteParentId = "-1"
    }

    private fun child(name: String, remoteId: String, parentRemoteId: String) =
        category(name).apply {
            this.remoteId = remoteId
            remoteParentId = parentRemoteId
        }
}
