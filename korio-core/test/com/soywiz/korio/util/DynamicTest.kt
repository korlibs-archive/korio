package com.soywiz.korio.util

import com.soywiz.korio.async.syncTest
import org.junit.Assert
import org.junit.Test

class DynamicTest {
	@Test
	fun eq() {
		Assert.assertEquals(true, Dynamic.binop(1, 1, "=="))
		Assert.assertEquals(true, Dynamic.binop(1.0, 1, "=="))
		Assert.assertEquals(false, Dynamic.binop(1.0, 1.1, "=="))
	}

	@Test
	fun op() {
		Assert.assertEquals(true, Dynamic.binop(1.0, 3, "<"))
		Assert.assertEquals(false, Dynamic.binop(1.0, 3, ">"))
		Assert.assertEquals(true, Dynamic.binop(1, 3.0, "<"))
		Assert.assertEquals(false, Dynamic.binop(1, 3.0, ">"))
		Assert.assertEquals(true, Dynamic.binop(1.0, 3.0, "<"))
		Assert.assertEquals(false, Dynamic.binop(1.0, 3.0, ">"))
		Assert.assertEquals(false, Dynamic.binop(6.0, 3.0, "<"))
		Assert.assertEquals(true, Dynamic.binop(6.0, 3.0, ">"))
	}

	@Test
	fun get() = syncTest {
		class DynamicObj(val obj: Any?) {
			suspend fun get(key: String) = DynamicObj(Dynamic.getAny(obj, key))

			fun <T> to(): T = obj as T
		}

		Assert.assertEquals(10, DynamicObj(mapOf("a" to 10)).get("a").to<Int>())
	}
}
