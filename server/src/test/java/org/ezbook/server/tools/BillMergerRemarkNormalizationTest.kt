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

import org.ezbook.server.db.model.BillInfoModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    @Test
    fun normalizeName_handlesLargeRepeatedTextWithoutBlocking() {
        val input = "abc123".repeat(500)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val future = executor.submit<String> { BillMerger.normalizeName(input) }
            val result = future.get(2, TimeUnit.SECONDS)
            assertEquals(input, result)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun formatLocationAddressParts_dropsCountryAndProvinceAndKeepsCityDistrictDetail() {
        val format = BillMerger.formatLocationAddressParts(
            city = "中国浙江省嘉兴市南湖区xx路xx小区6号",
            district = "",
            detail = ""
        )

        assertEquals("嘉兴市南湖区xx路xx小区6号", format)
    }

    @Test
    fun formatLocationAddressParts_ignoresBlankSegments() {
        val format = BillMerger.formatLocationAddressParts(
            city = "",
            district = "南湖区",
            detail = ""
        )

        assertEquals("南湖区", format)
    }

    @Test
    fun formatLocationAddressParts_returnsFallbackWhenAddressMissing() {
        assertEquals(
            "未授权位置信息",
            BillMerger.formatLocationAddressParts(city = "", district = "", detail = "")
        )
    }

    @Test
    fun mergeChannelInfo_joinsDistinctSources() {
        val parent = BillInfoModel(channel = "微信[招商银行]")
        val child = BillInfoModel(channel = "建设银行动账")
        BillMerger.mergeChannelInfo(child, parent)
        assertEquals("微信[招商银行] 建设银行动账", parent.channel)
    }

    @Test
    fun mergeChannelInfo_keepsSingleSideWhenOtherEmpty() {
        val parent = BillInfoModel(channel = "支付宝")
        BillMerger.mergeChannelInfo(BillInfoModel(channel = ""), parent)
        assertEquals("支付宝", parent.channel)
    }
}

