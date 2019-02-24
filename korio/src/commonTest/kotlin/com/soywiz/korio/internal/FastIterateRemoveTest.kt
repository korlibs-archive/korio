package com.soywiz.korio.internal

import kotlin.test.*

class FastIterateRemoveTest {
	@Test
	fun test() {
		assertEquals(listOf(1, 3, 5, 5, 3), arrayListOf(1, 2, 3, 4, 5, 5, 8, 8, 3).fastIterateRemove { it % 2 == 0 })
	}
}