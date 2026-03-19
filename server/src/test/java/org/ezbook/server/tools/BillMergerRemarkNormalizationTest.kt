/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
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

import org.junit.Assert.assertEquals
import org.junit.Test

class BillMergerRemarkNormalizationTest {

    @Test
    fun normalizeName_removesOnlyAdjacentDuplicateFragments() {
        assertEquals("京东自营旗舰店", BillMerger.normalizeName("京东自营京东自营旗舰店"))
        assertEquals("苹果旗舰店", BillMerger.normalizeName("苹果苹果旗舰店旗舰店"))
    }

    @Test
    fun normalizeName_keepsRepeatedTimestampPrefixesThatAreNotAdjacent() {
        val original = "地铁-固戍-2026-03-06 09:01:33-高新园-2026-03-06 09:34:31"

        assertEquals(original, BillMerger.normalizeName(original))
    }

    @Test
    fun deduplicateRemarkFields_removesSafeBoundaryOverlapOnly() {
        assertEquals(
            "京东自营" to "旗舰店",
            BillMerger.deduplicateRemarkFields("京东自营", "京东自营旗舰店")
        )
        assertEquals(
            "苹果旗舰店" to "",
            BillMerger.deduplicateRemarkFields("苹果旗舰店", "旗舰店")
        )
    }

    @Test
    fun deduplicateRemarkFields_keepsIndependentSegments() {
        val shopName = "地铁-固戍-2026-03-06 09:01:33"
        val shopItem = "高新园-2026-03-06 09:34:31"

        assertEquals(
            shopName to shopItem,
            BillMerger.deduplicateRemarkFields(shopName, shopItem)
        )
    }
}

