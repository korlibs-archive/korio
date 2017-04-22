package com.soywiz.korio.util

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class RedirectFieldTest {
	class A {
		var z: Int = 10
	}

	class B(val a: A) {
		var z: Int by redirectField(a::z)
		val y: Int by redirectField(this::z)
	}

	@Test
	fun redirect() {
		val b = B(A())
		Assert.assertEquals(b.z, 10)
		Assert.assertEquals(b.a.z, 10)
		Assert.assertEquals(b.y, 10)
		b.z = 20
		Assert.assertEquals(b.z, 20)
		Assert.assertEquals(b.a.z, 20)
		Assert.assertEquals(b.y, 20)
	}
}