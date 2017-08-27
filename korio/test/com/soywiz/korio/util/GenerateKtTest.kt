package com.soywiz.korio.util

import org.junit.Assert
import org.junit.Test

class GenerateTest {
	fun evens() = generate<Int> {
		for (n in 0 until Int.MAX_VALUE step 2) yield(n)
	}

	@Test
	fun name() {
		Assert.assertEquals(listOf(0, 2, 4, 6), evens().take(4).toList())
	}
}