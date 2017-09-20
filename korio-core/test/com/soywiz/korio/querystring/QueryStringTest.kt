package com.soywiz.korio.querystring

import com.soywiz.korio.serialization.querystring.QueryString
import org.junit.Test
import kotlin.test.assertEquals

class QueryStringTest {
	private fun assertIdem(str: String) {
		assertEquals(str, QueryString.encode(QueryString.decode(str)))
	}

	@Test
	fun name() {
		assertEquals(mapOf("a" to listOf("2"), "b" to listOf("3")), QueryString.decode("a=2&b=3"))
		assertIdem("a=1&b=2")
	}
}