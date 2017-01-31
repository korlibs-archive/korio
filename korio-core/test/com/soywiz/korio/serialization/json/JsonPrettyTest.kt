package com.soywiz.korio.serialization.json

import org.junit.Assert
import org.junit.Test

class JsonPrettyTest {
	@Test
	fun encode1() {
		Assert.assertEquals("1", Json.encodePretty(1))
		Assert.assertEquals("null", Json.encodePretty(null))
		Assert.assertEquals("true", Json.encodePretty(true))
		Assert.assertEquals("false", Json.encodePretty(false))
		Assert.assertEquals("{\n}", Json.encodePretty(mapOf<String, Any?>()))
		Assert.assertEquals("[\n]", Json.encodePretty(listOf<Any?>()))
		Assert.assertEquals("\"a\"", Json.encodePretty("a"))
	}

	@Test
	fun encode2() {
		Assert.assertEquals("""
			|[
			|	1,
			|	2,
			|	3
			|]
		""".trimMargin(), Json.encodePretty(listOf(1, 2, 3)))

		Assert.assertEquals("""
			|{
			|	"a": 1,
			|	"b": 2
			|}
		""".trimMargin(), Json.encodePretty(hashMapOf("a" to 1, "b" to 2)))
	}

	data class Demo(val a: Int, val b: String)

	data class DemoList(val demos: ArrayList<Demo>)

	data class DemoSet(val demos: Set<Demo>)

	@Test
	fun encodeTyped() {
		Assert.assertEquals("""
			|{
			|	"a": 1,
			|	"b": "test"
			|}
			""".trimMargin(), Json.encodePretty(Demo(1, "test"))
		)
	}

	@Test
	fun encodeMix() {
		Assert.assertEquals("""
				|{
				|	"a": [
				|		1,
				|		2,
				|		3,
				|		4
				|	],
				|	"b": [
				|		5,
				|		6
				|	],
				|	"c": {
				|		"a": true,
				|		"b": null,
				|		"c": "hello"
				|	}
				|}
			""".trimMargin(), Json.encodePretty(mapOf(
				"a" to listOf(1, 2, 3, 4),
				"b" to listOf(5, 6),
				"c" to mapOf(
					"a" to true,
					"b" to null,
					"c" to "hello"
				)
			))
		)
	}
}