package com.soywiz.korio.util

import org.junit.Assert.*
import org.junit.Test

class DynamicNodeTest {
	@Test
	fun name() {
		val info = DynamicNode(mapOf("hello" to "Carlos", "world" to 10, "list" to listOf(0, 10, 20, 30)))
		assertEquals("Carlos", info["hello"].asString())
		assertEquals(10, info["world"].toInt())
		assertEquals(0, info["list"][0].toInt())
		assertEquals(10, info["list"][1].toInt())
		assertEquals(listOf("hello", "world", "list"), info.getKeys())
		assertEquals(listOf("0", "1", "2", "3"), info["list"].getKeys())
	}
}