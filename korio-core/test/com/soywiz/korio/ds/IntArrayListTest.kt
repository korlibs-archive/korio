package com.soywiz.korio.ds

import org.junit.Assert
import org.junit.Test

class IntArrayListTest {
	@Test
	fun name() {
		val values = IntArrayList(2)
		Assert.assertEquals(0, values.length)
		Assert.assertEquals(2, values.capacity)
		values.add(1)
		Assert.assertEquals(listOf(1), values.toList())
		Assert.assertEquals(1, values.length)
		Assert.assertEquals(2, values.capacity)
		values.add(2)
		Assert.assertEquals(listOf(1, 2), values.toList())
		Assert.assertEquals(2, values.length)
		Assert.assertEquals(2, values.capacity)
		values.add(3)
		Assert.assertEquals(listOf(1, 2, 3), values.toList())
		Assert.assertEquals(3, values.length)
		Assert.assertEquals(6, values.capacity)
	}
}