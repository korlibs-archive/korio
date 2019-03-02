package com.soywiz.korio.lang

import kotlin.test.*

class StringExtTest {
	@Test
	fun testParseInt() {
		assertEquals(16, "0x10".parseInt())
		assertEquals(16, "16".parseInt())
	}

	@Test
	fun testTransform() {
		assertEquals("hEEllo", "hello".transform { if (it == 'e') "EE" else "$it" })
	}

	@Test
	fun testEachBuilder() {
		assertEquals("hEEllo", "hello".eachBuilder { if (it == 'e') append("EE") else append(it) })
	}

	@Test
	fun testSplitInChunks() {
		assertEquals(listOf("ab", "cd"), "abcd".splitInChunks(2))
		assertEquals(listOf("ab", "cd", "e"), "abcde".splitInChunks(2))
	}

	@Test
	fun testSplitKeep() {
		assertEquals(listOf("a", "   ", "c"), "a   c".splitKeep(Regex("\\s+")))
	}

	@Test
	fun testFormat() {
		assertEquals("1 2", "%d %d".format(1, 2))
		assertEquals("01 2", "%02d %d".format(1, 2))
		assertEquals("f", "%x".format(15))
		assertEquals("0f", "%02x".format(15))
		assertEquals("0F", "%02X".format(15))
	}

}
