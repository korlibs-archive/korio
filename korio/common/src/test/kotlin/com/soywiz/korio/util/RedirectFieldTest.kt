package com.soywiz.korio.util

import kotlin.test.assertEquals

class RedirectFieldTest {
	class A {
		var z: Int = 10
	}

	class B(val a: A) {
		var z: Int by redirectField(a::z)
		val y: Int by redirectField(this::z)
	}

	@kotlin.test.Test
	fun redirect() {
		val b = B(A())
		assertEquals(b.z, 10)
		assertEquals(b.a.z, 10)
		assertEquals(b.y, 10)
		b.z = 20
		assertEquals(b.z, 20)
		assertEquals(b.a.z, 20)
		assertEquals(b.y, 20)
	}
}