package com.soywiz.korio.util

import org.junit.Assert
import org.junit.Test

class StringExtTest {
	@Test
	fun name() {
		Assert.assertEquals(listOf<String>(), "".splitInChunks(3))
		Assert.assertEquals(listOf("1"), "1".splitInChunks(3))
		Assert.assertEquals(listOf("12"), "12".splitInChunks(3))
		Assert.assertEquals(listOf("123"), "123".splitInChunks(3))
		Assert.assertEquals(listOf("123", "4"), "1234".splitInChunks(3))
		Assert.assertEquals(listOf("123", "45"), "12345".splitInChunks(3))
		Assert.assertEquals(listOf("123", "456"), "123456".splitInChunks(3))
		Assert.assertEquals(listOf("123", "456", "7"), "1234567".splitInChunks(3))
	}
}