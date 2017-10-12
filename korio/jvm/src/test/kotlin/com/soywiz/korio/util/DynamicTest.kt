package com.soywiz.korio.util

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.ds.lmapOf
import org.junit.Test
import kotlin.test.assertEquals

class DynamicTest {
	@Test
	fun eq() {
		assertEquals(true, Dynamic.binop(1, 1, "=="))
		assertEquals(true, Dynamic.binop(1.0, 1, "=="))
		assertEquals(false, Dynamic.binop(1.0, 1.1, "=="))
	}

	@Test
	fun op() {
		assertEquals(true, Dynamic.binop(1.0, 3, "<"))
		assertEquals(false, Dynamic.binop(1.0, 3, ">"))
		assertEquals(true, Dynamic.binop(1, 3.0, "<"))
		assertEquals(false, Dynamic.binop(1, 3.0, ">"))
		assertEquals(true, Dynamic.binop(1.0, 3.0, "<"))
		assertEquals(false, Dynamic.binop(1.0, 3.0, ">"))
		assertEquals(false, Dynamic.binop(6.0, 3.0, "<"))
		assertEquals(true, Dynamic.binop(6.0, 3.0, ">"))
	}

	@Test
	fun get() = syncTest {
		class DynamicObj(val obj: Any?) {
			suspend fun get(key: String) = DynamicObj(Dynamic.getAny(obj, key))

			fun <T> to(): T = obj as T
		}

		assertEquals(10, DynamicObj(lmapOf("a" to 10)).get("a").to<Int>())
	}
}
