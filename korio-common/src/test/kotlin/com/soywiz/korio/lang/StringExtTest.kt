package com.soywiz.korio.lang

import org.junit.Test
import kotlin.test.assertEquals

class StringExtTest {
	@Test
	fun name() {
		assertEquals(listOf("90"), "90".splitKeep(Regex("\\d+")))
		assertEquals(listOf("a", "90"), "a90".splitKeep(Regex("\\d+")))
		assertEquals(listOf("a", "90", "b"), "a90b".splitKeep(Regex("\\d+")))
		assertEquals(listOf("a", "90", "b", "3"), "a90b3".splitKeep(Regex("\\d+")))
		assertEquals(listOf("a", "90", "b", "3", "cc"), "a90b3cc".splitKeep(Regex("\\d+")))
	}

	@Test
	fun format() {
		assertEquals("hello 10", "%s %d".format("hello", 10))
		assertEquals("ff", "%02x".format(255))
		assertEquals("FF", "%02X".format(255))
	}
}