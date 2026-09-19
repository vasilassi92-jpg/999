package com.localstream.app

import com.localstream.app.server.RangeRequestParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RangeRequestParserTest {

    @Test
    fun testNullOrEmptyRange() {
        val result = RangeRequestParser.parseRange(null, 1000L)
        assertNull(result)

        val resultEmpty = RangeRequestParser.parseRange("", 1000L)
        assertNull(resultEmpty)
    }

    @Test
    fun testStandardByteRange() {
        val result = RangeRequestParser.parseRange("bytes=0-499", 1000L)
        assertNotNull(result)
        assertEquals(0L, result?.start)
        assertEquals(499L, result?.end)
        assertEquals(500L, result?.length)
        assertEquals("bytes 0-499/1000", result?.contentRangeHeader)
    }

    @Test
    fun testOpenEndedByteRange() {
        val result = RangeRequestParser.parseRange("bytes=500-", 1000L)
        assertNotNull(result)
        assertEquals(500L, result?.start)
        assertEquals(999L, result?.end)
        assertEquals(500L, result?.length)
        assertEquals("bytes 500-999/1000", result?.contentRangeHeader)
    }

    @Test
    fun testSuffixByteRange() {
        val result = RangeRequestParser.parseRange("bytes=-200", 1000L)
        assertNotNull(result)
        assertEquals(800L, result?.start)
        assertEquals(999L, result?.end)
        assertEquals(200L, result?.length)
    }

    @Test(expected = RangeRequestParser.InvalidRangeException::class)
    fun testInvalidOutOfBoundRange() {
        RangeRequestParser.parseRange("bytes=1500-2000", 1000L)
    }
}
