package com.soywiz.korio.lang

import kotlin.test.*

class CharsetTest {
    @Test
    fun testSurrogatePairs() {
        val text = "{Test\uD83D\uDE00}"
        assertEquals(
            listOf(123, 84, 101, 115, 116, -16, -97, -104, -128, 125),
            text.toByteArray(UTF8).map { it.toInt() }
        )
    }

    @Test
    fun testSurrogatePairsTwo() {
        val text = "{Test\uD83D\uDE00}"
        assertEquals(
            text,
            text.toByteArray(UTF8).toString(UTF8).toByteArray(UTF8).toString(UTF8)
        )
    }
}
