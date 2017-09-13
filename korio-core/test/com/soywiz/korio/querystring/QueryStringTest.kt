package com.soywiz.korio.querystring

import com.soywiz.korio.serialization.querystring.QueryString
import org.junit.Assert.*
import org.junit.Test

class QueryStringTest {
	@Test
	fun name() {
		assertEquals(mapOf("a" to listOf("2"), "b" to listOf("3")), QueryString.decode("a=2&b=3"))
	}
}