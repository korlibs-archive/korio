package com.soywiz.korio.serialization.json

import org.junit.Assert
import org.junit.Test

class JsonTest {
	@Test
	fun decode1() {
		Assert.assertEquals(mapOf("a" to 1), Json.decode("""{"a":1}"""))
		Assert.assertEquals(-1e7, Json.decode("""-1e7"""))
	}

	@Test
	fun decode2() {
		Assert.assertEquals(
			listOf("a", 1, -1, 0.125, 0, 11, true, false, null, listOf<Any?>(), mapOf<String, Any?>()),
			Json.decode("""["a", 1, -1, 0.125, 0, 11, true, false, null, [], {}]""")
		)
	}

	@Test
	fun decode3() {
		Assert.assertEquals("\"", Json.decode(""" "\"" """))
		Assert.assertEquals(listOf(1, 2), Json.decode(""" [ 1 , 2 ]"""))
	}

	@Test
	fun encode1() {
		Assert.assertEquals("1", Json.encode(1))
		Assert.assertEquals("null", Json.encode(null))
		Assert.assertEquals("true", Json.encode(true))
		Assert.assertEquals("false", Json.encode(false))
		Assert.assertEquals("{}", Json.encode(mapOf<String, Any?>()))
		Assert.assertEquals("[]", Json.encode(listOf<Any?>()))
		Assert.assertEquals("\"a\"", Json.encode("a"))
	}

	@Test
	fun encode2() {
		Assert.assertEquals("[1,2,3]", Json.encode(listOf(1, 2, 3)))
		Assert.assertEquals("""{"a":1,"b":2}""", Json.encode(hashMapOf("a" to 1, "b" to 2)))
	}
}
