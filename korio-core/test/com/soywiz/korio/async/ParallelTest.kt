package com.soywiz.korio.async

import org.junit.Assert
import org.junit.Test

class ParallelTest {
	@Test
	fun empty() = syncTest {
		val out = ""
		parallel()
		Assert.assertEquals("", out)
	}

	@Test
	fun one() = syncTest {
		var out = ""
		parallel(
			{ sleep(100); out += "a" }
		)
		Assert.assertEquals("a", out)
	}

	@Test
	fun couple() = syncTest {
		var out = ""
		parallel(
			{ sleep(100); out += "a" },
			{ sleep(200); out += "b" }
		)
		Assert.assertEquals("ab", out)
	}
}