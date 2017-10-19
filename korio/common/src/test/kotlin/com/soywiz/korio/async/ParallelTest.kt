package com.soywiz.korio.async

import kotlin.test.assertEquals

class ParallelTest {
	@kotlin.test.Test
	fun empty() = syncTest {
		val out = ""
		parallel()
		assertEquals("", out)
	}

	@kotlin.test.Test
	fun one() = syncTest {
		var out = ""
		parallel(
			{ sleep(100); out += "a" }
		)
		assertEquals("a", out)
	}

	@kotlin.test.Test
	fun couple() = syncTest {
		var out = ""
		parallel(
			{ sleep(100); out += "a" },
			{ sleep(200); out += "b" }
		)
		assertEquals("ab", out)
	}
}