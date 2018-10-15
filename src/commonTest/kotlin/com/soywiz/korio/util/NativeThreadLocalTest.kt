package com.soywiz.korio.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeThreadLocalTest {
	@NativeThreadLocal
	companion object {
	    var demo = 10
	}

	@Test
	fun test() {
		demo = 5 // Would fail without @NativeThreadLocal on native
		assertEquals(5, demo)
	}
}