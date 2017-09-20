package com.soywiz.korio.async

import org.junit.Test
import kotlin.test.assertTrue

class SleepTest {
	@Test
	fun name() = syncTest {
		val start = time
		sleep(10)
		sleep(20)
		val end = time
		assertTrue((end - start) > 25L)
	}
}