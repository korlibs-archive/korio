package com.soywiz.korio.serialization.json

import com.soywiz.korio.serialization.ObjectMapper
import org.junit.Test
import kotlin.test.assertEquals

class JsonPrettyTest {
	val mapper = ObjectMapper()

	@Test
	fun encode1() {
		assertEquals("1", Json.encodePretty(1, mapper))
		assertEquals("null", Json.encodePretty(null, mapper))
		assertEquals("true", Json.encodePretty(true, mapper))
		assertEquals("false", Json.encodePretty(false, mapper))
		assertEquals("{\n}", Json.encodePretty(mapOf<String, Any?>(), mapper))
		assertEquals("[\n]", Json.encodePretty(listOf<Any?>(), mapper))
		assertEquals("\"a\"", Json.encodePretty("a", mapper))
	}

	@Test
	fun encode2() {
		assertEquals("""
			|[
			|	1,
			|	2,
			|	3
			|]
		""".trimMargin(), Json.encodePretty(listOf(1, 2, 3), mapper))

		assertEquals("""
			|{
			|	"a": 1,
			|	"b": 2
			|}
		""".trimMargin(), Json.encodePretty(hashMapOf("a" to 1, "b" to 2), mapper))
	}

	data class Demo(val a: Int, val b: String)

	data class DemoList(val demos: ArrayList<Demo>)

	data class DemoSet(val demos: Set<Demo>)

	@Test
	fun encodeTyped() {
		assertEquals("""
			|{
			|	"a": 1,
			|	"b": "test"
			|}
			""".trimMargin(), Json.encodePretty(Demo(1, "test"), mapper)
		)
	}

	@Test
	fun encodeMix() {
		assertEquals("""
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
			""".trimMargin(),
			Json.encodePretty(mapOf(
				"a" to listOf(1, 2, 3, 4),
				"b" to listOf(5, 6),
				"c" to mapOf(
					"a" to true,
					"b" to null,
					"c" to "hello"
				)
			), mapper)
		)
	}
}