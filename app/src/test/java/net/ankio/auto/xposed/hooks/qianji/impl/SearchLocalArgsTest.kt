package net.ankio.auto.xposed.hooks.qianji.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchLocalArgsTest {

    private fun build(count: Int) = SearchLocalArgs.build(
        paramCount = count,
        bookFilter = "book",
        dateFilter = "date",
        typesFilter = "types",
        sortFilter = "sort",
        billFlagFilter = "flag",
    )

    @Test
    fun `12 params stops at bill flag`() {
        val args = build(12)
        assertEquals(12, args.size)
        assertEquals("", args[0])
        assertEquals("book", args[1])
        assertEquals(false, args[7])
        assertEquals(false, args[8])
        assertEquals(true, args[9])
        assertEquals("sort", args[10])
        assertEquals("flag", args[11])
    }

    @Test
    fun `15 params adds three nullable filters`() {
        val args = build(15)
        assertEquals(15, args.size)
        assertNull(args[12])
        assertNull(args[13])
        assertNull(args[14])
    }

    @Test
    fun `17 params matches qianji 4_5_4 signature`() {
        val args = build(17)
        assertEquals(17, args.size)
        assertNull(args[12])
        assertNull(args[13])
        assertNull(args[14])
        assertEquals(true, args[15]) // includeNotCount
        assertEquals(true, args[16]) // includeNotBudget
    }

    @Test
    fun `future 18th param padded with true`() {
        val args = build(18)
        assertEquals(18, args.size)
        assertEquals(true, args[17])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects fewer than 12 params`() {
        build(11)
    }
}
