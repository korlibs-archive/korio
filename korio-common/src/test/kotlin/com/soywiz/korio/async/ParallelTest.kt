package com.soywiz.korio.async

import org.junit.Test
import kotlin.test.assertEquals

class ParallelTest {
	@Test
	fun empty() = syncTest {
		val out = ""
		parallel()
		assertEquals("", out)
	}

	@Test
	fun one() = syncTest {
		var out = ""
		parallel(
			{ sleep(100); out += "a" }
		)
		assertEquals("a", out)
	}

	@Test
	fun couple() = syncTest {
		var out = ""
		parallel(
			{ sleep(100); out += "a" },
			{ sleep(200); out += "b" }
		)
		assertEquals("ab", out)
	}
}