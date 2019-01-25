package com.soywiz.korio.lang

import kotlin.test.*

class StringExtTest {
	@kotlin.test.Test
	fun name() {
		assertEquals(listOf("90"), "90".splitKeep(Regex("\\d+")))
		assertEquals(listOf("a", "90"), "a90".splitKeep(Regex("\\d+")))
		assertEquals(listOf("a", "90", "b"), "a90b".splitKeep(Regex("\\d+")))
		assertEquals(listOf("a", "90", "b", "3"), "a90b3".splitKeep(Regex("\\d+")))
		assertEquals(listOf("a", "90", "b", "3", "cc"), "a90b3cc".splitKeep(Regex("\\d+")))
	}

	@kotlin.test.Test
	fun format() {
		assertEquals("GMT+0200", "GMT%s%02d%02d".format("+", 2, 0))
	}

	//@kotlin.test.Test
	@Test
	fun formatHex() {
		assertEquals("FFFFFFFF", "%08X".format(0xFFFFFFFF.toInt()))
	}

	@Test
	fun replaceNonPrintableCharacters() {
		assertEquals("?hello??world", "\nhello\t\rworld".replaceNonPrintableCharacters(replacement = "?"))
	}

	@kotlin.test.Test
	fun name2() {
		assertEquals(listOf<String>(), "".splitInChunks(3))
		assertEquals(listOf("1"), "1".splitInChunks(3))
		assertEquals(listOf("12"), "12".splitInChunks(3))
		assertEquals(listOf("123"), "123".splitInChunks(3))
		assertEquals(listOf("123", "4"), "1234".splitInChunks(3))
		assertEquals(listOf("123", "45"), "12345".splitInChunks(3))
		assertEquals(listOf("123", "456"), "123456".splitInChunks(3))
		assertEquals(listOf("123", "456", "7"), "1234567".splitInChunks(3))
	}

}