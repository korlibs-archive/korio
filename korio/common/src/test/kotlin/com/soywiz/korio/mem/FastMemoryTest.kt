package com.soywiz.korio.mem

import org.junit.Test
import kotlin.test.assertEquals

class FastMemoryTest {
	@Test
	fun name() {
		val mem1 = FastMemory.alloc(32)
		mem1.setArrayInt8(0, (0 until 32).map { it.toByte() }.toByteArray(), 0, 32)
		val mem2 = FastMemory.alloc(32)
		FastMemory.copy(mem1, 2, mem2, 5, 10)
		assertEquals(
			listOf(0, 0, 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
			mem2.getBytes(0, 32).map { it.toInt() }.toList()
		)
	}
}