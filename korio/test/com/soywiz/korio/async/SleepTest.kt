package com.soywiz.korio.async

import org.junit.Assert
import org.junit.Test

class SleepTest {
	@Test
	fun name() = syncTest {
		val start = time
		sleep(10)
		sleep(20)
		val end = time
		Assert.assertTrue((end - start) > 25L)
	}
}