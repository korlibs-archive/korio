package com.soywiz.korio.async

import org.junit.Assert
import org.junit.Test

class SleepTest {
	@Test
	fun name() = sync {
		val start = System.currentTimeMillis()
		sleep(10)
		sleep(20)
		val end = System.currentTimeMillis()
		Assert.assertTrue((end - start) > 25L)
	}
}