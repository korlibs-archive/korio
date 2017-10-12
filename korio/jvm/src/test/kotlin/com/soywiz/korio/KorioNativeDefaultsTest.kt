package com.soywiz.korio

import org.junit.Test
import kotlin.test.assertEquals

class KorioNativeDefaultsTest {
	private fun testOverlapping(src: Int, dst: Int, count: Int, gen: () -> IntArray) {
		val jvm = gen()
		System.arraycopy(jvm, src, jvm, dst, count)
		val default = gen()
		KorioNativeDefaults.copyRangeTo(default, src, default, dst, count)
		assertEquals(jvm.toList(), default.toList(), "failed src=$src, dst=$dst")
	}

	@Test
	fun name() {
		for (x in 0..2) {
			for (y in 0..2) {
				testOverlapping(x, y, 3) { intArrayOf(1, 2, 3, 4, 5, 6) }
			}
		}
	}
}