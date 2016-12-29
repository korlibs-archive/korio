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
}