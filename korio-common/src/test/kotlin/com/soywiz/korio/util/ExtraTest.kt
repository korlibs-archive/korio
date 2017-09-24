package com.soywiz.korio.util

import org.junit.Test
import kotlin.test.assertEquals

class ExtraTest {
	class Demo : Extra by Extra.Mixin() {
		val default = 9
	}

	var Demo.demo by Extra.Property { 0 }
	var Demo.demo2 by Extra.PropertyThis<Demo, Int> { default }

	@Test
	fun name() {
		val demo = Demo()
		assertEquals(0, demo.demo)
		assertEquals(9, demo.demo2)
		demo.demo = 7
		assertEquals(7, demo.demo)
		assertEquals("{demo=7, demo2=9}", demo.extra.toString())
	}
}