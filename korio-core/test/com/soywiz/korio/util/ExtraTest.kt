package com.soywiz.korio.util

import org.junit.Assert
import org.junit.Test

class ExtraTest {
	class Demo : Extra by Extra.Mixin()

	var Demo.demo by extraProperty("demo", 0)

	@Test
	fun name() {
		val demo = Demo()
		Assert.assertEquals(0, demo.demo)
		demo.demo = 7
		Assert.assertEquals(7, demo.demo)
		Assert.assertEquals("{demo=7}", demo.extra.toString())
	}
}