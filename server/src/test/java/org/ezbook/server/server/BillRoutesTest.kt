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

package org.ezbook.server.server

import org.ezbook.server.constant.BillState
import org.junit.Assert.assertEquals
import org.junit.Test

class BillRoutesTest {

    private val defaultStates = listOf(
        BillState.Edited.name,
        BillState.Synced.name,
        BillState.Wait2Edit.name
    )

    @Test
    fun parseBillListStates_acceptsLegacyCommaSpaceFormat() {
        assertEquals(
            defaultStates,
            parseBillListStates("Edited, Synced, Wait2Edit", defaultStates)
        )
    }

    @Test
    fun parseBillListStates_ignoresEmptyTokens() {
        assertEquals(
            listOf(BillState.Edited.name, BillState.Wait2Edit.name),
            parseBillListStates(" Edited, , Wait2Edit, ", defaultStates)
        )
    }

    @Test
    fun parseBillListStates_fallsBackToDefaultStatesWhenEmpty() {
        assertEquals(defaultStates, parseBillListStates(null, defaultStates))
        assertEquals(defaultStates, parseBillListStates("", defaultStates))
        assertEquals(defaultStates, parseBillListStates(" , , ", defaultStates))
    }
}
