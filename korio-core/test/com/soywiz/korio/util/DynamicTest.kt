package com.soywiz.korio.util

import org.junit.Assert
import org.junit.Test

class DynamicTest {
	@Test
	fun eq() {
		Assert.assertEquals(true, Dynamic.binop(1, 1, "=="))
		Assert.assertEquals(true, Dynamic.binop(1.0, 1, "=="))
		Assert.assertEquals(false, Dynamic.binop(1.0, 1.1, "=="))
	}

	@Test
	fun name() {
		Assert.assertEquals(true, Dynamic.binop(1.0, 3, "<"))
		Assert.assertEquals(false, Dynamic.binop(1.0, 3, ">"))
		Assert.assertEquals(true, Dynamic.binop(1, 3.0, "<"))
		Assert.assertEquals(false, Dynamic.binop(1, 3.0, ">"))
		Assert.assertEquals(true, Dynamic.binop(1.0, 3.0, "<"))
		Assert.assertEquals(false, Dynamic.binop(1.0, 3.0, ">"))
		Assert.assertEquals(false, Dynamic.binop(6.0, 3.0, "<"))
		Assert.assertEquals(true, Dynamic.binop(6.0, 3.0, ">"))
	}
}