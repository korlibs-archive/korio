package com.soywiz.korio.util

import org.junit.Test
import kotlin.test.assertEquals

class NumberExtTest {
	@Test
	fun testNextAligned() {
		assertEquals(0L, 0L.nextAlignedTo(15L))
		assertEquals(15L, 1L.nextAlignedTo(15L))
		assertEquals(15L, 14L.nextAlignedTo(15L))
		assertEquals(15L, 15L.nextAlignedTo(15L))
		assertEquals(30L, 16L.nextAlignedTo(15L))
	}

	@Test
	fun insert() {
		val v = 0x12345678
		assertEquals("FF345678", "%08X".format(v.insert(0xFF, 24, 8)))
		assertEquals("1F345678", "%08X".format(v.insert(0xFF, 24, 4)))
		assertEquals("12345FF8", "%08X".format(v.insert(0xFF, 4, 8)))
	}
}