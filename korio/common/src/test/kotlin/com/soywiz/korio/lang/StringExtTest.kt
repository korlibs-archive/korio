package com.soywiz.korio.lang

import org.junit.Test
import kotlin.test.assertEquals

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
}