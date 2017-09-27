package com.soywiz.korio.util

import com.soywiz.korio.serialization.ObjectMapper
import org.junit.Test
import kotlin.test.assertEquals

class ClassFactoryTest {
	@Test
	fun name() {
		data class A(val a: Int, val b: String)
		val mapper = ObjectMapper()
		mapper.jvmFallback()
		assertEquals(
			mapOf("a" to 10, "b" to "test"),
			mapper.toUntyped(A(10, "test"))
		)
	}
}