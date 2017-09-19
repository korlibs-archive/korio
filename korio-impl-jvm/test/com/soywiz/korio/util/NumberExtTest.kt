package com.soywiz.korio.util

import org.junit.Assert
import org.junit.Test

class NumberExtTest {
	@Test
	fun testNextAligned() {
		Assert.assertEquals(0L, 0L.nextAlignedTo(15L))
		Assert.assertEquals(15L, 1L.nextAlignedTo(15L))
		Assert.assertEquals(15L, 14L.nextAlignedTo(15L))
		Assert.assertEquals(15L, 15L.nextAlignedTo(15L))
		Assert.assertEquals(30L, 16L.nextAlignedTo(15L))
	}

	@Test
	fun insert() {
		val v = 0x12345678
		Assert.assertEquals("FF345678", "%08X".format(v.insert(0xFF, 24, 8)))
		Assert.assertEquals("1F345678", "%08X".format(v.insert(0xFF, 24, 4)))
		Assert.assertEquals("12345FF8", "%08X".format(v.insert(0xFF, 4, 8)))
	}
}