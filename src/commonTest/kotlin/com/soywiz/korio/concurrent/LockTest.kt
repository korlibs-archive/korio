package com.soywiz.korio.concurrent

import kotlin.test.*

class LockTest {
	@Test
	fun test() {
		val lock = Lock()
		var value = 0
		lock {
			value = 1
		}
		assertEquals(1, value)
	}
}